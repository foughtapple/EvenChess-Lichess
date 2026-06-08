import { INITIAL_FEN } from 'chessops/fen';

import { type Prop, propWithEffect, toggle } from 'lib';
import { debounce } from 'lib/async';
import type { ColorChoice, ColorProp } from 'lib/setup/color';
import {
  allTimeModeKeys,
  timeControlFromStoredValues,
  timeModes,
  type TimeControl,
} from 'lib/setup/timeControl';
import { storedJsonProp } from 'lib/storage';
import { alert } from 'lib/view';
import * as xhr from 'lib/xhr';

import type LobbyController from './ctrl';
import {
  evenChessClockParamsForPoolId,
  evenChessComputerSetLevel,
  evenChessLevelValid,
  evenChessPendingPoolId,
  evenChessPreferredSetLevelParam,
  evenChessSearchStatusDebugEnabled,
  evenChessSetLevelForGameType,
  evenChessSubmitMode,
  evenChessTimeControlKeyForPoolId,
} from './evenchessSetup';
import type { EvenChessSearchStatus, ForceSetupOptions, GameMode, GameType, PoolMember, SetupStore } from './interfaces';
import { keyToId, variants } from './options';

const getPerf = (variant: VariantKey, tc: TimeControl): Perf =>
  variant !== 'standard' && variant !== 'fromPosition' ? variant : tc.speed();

export default class SetupController {
  root: LobbyController;
  store: Record<GameType, Prop<SetupStore>>;
  gameType: GameType | null = null;
  lastValidFen = '';
  fenError = false;
  friendUser = '';
  loading = false;
  color: ColorProp;
  forced?: ForceSetupOptions;

  // Store props
  variant: Prop<VariantKey>;
  fen: Prop<string>;
  gameMode: Prop<GameMode>;
  evenChessSetLevel: Prop<number>;
  evenChessTargetLevel: Prop<string>;
  evenChessApplyPreferences: Prop<boolean>;
  evenChessPlayerTargetLevel: Prop<string>;
  evenChessOpponentTargetLevel: Prop<string>;
  evenChessStrictSearch: Prop<boolean>;
  evenChessConfirmLevelContract: Prop<boolean>;
  evenChessFriendLevelMode: Prop<string>;
  evenChessFriendMyLevel: Prop<string>;
  evenChessFriendOpponentLevel: Prop<string>;
  ratingMin: Prop<number>;
  ratingMax: Prop<number>;
  aiLevel: Prop<number>;

  variantMenuOpen = toggle(false);

  timeControl: TimeControl;
  private evenChessSearchPollTimer?: number;
  private evenChessSearchPollToken = 0;

  constructor(ctrl: LobbyController) {
    this.root = ctrl;
    this.color = propWithEffect('random', this.onPropChange);
    // Initialize stores with default props as necessary
    this.store = {
      hook: this.makeSetupStore('hook'),
      friend: this.makeSetupStore('friend'),
      ai: this.makeSetupStore('ai'),
    };
  }

  // Namespace the store by username for user specific modal settings.
  private readonly storeKey = (gameType: GameType) =>
    `lobby.setup.${this.root.me?.username || 'anon'}.${gameType}`;

  makeSetupStore = (gameType: GameType) =>
    storedJsonProp<SetupStore>(this.storeKey(gameType), () => ({
      variant: 'standard',
      fen: '',
      timeMode: gameType === 'hook' ? 'realTime' : 'unlimited',
      time: 5,
      increment: 3,
      days: 2,
      gameMode: gameType === 'ai' || !this.root.me ? 'casual' : 'rated',
      evenChessSetLevel: evenChessSetLevelForGameType(gameType, undefined),
      evenChessTargetLevel: '',
      evenChessApplyPreferences: false,
      evenChessPlayerTargetLevel: '',
      evenChessOpponentTargetLevel: '',
      evenChessStrictSearch: false,
      evenChessConfirmLevelContract: false,
      evenChessFriendLevelMode: 'auto',
      evenChessFriendMyLevel: '',
      evenChessFriendOpponentLevel: '',
      ratingMin: -500,
      ratingMax: 500,
      aiLevel: 1,
    }));

  private readonly loadPropsFromStore = (forceOptions?: ForceSetupOptions) => {
    const storeProps = this.store[this.gameType!]();
    // Load props from the store, but override any store values with values found in forceOptions
    this.variant = propWithEffect(forceOptions?.variant || storeProps.variant, this.onDropdownChange);
    this.fen = this.propWithApply(forceOptions?.fen || storeProps.fen);
    const canChangeTimeMode = !!this.root.me || this.gameType !== 'hook';
    this.timeControl = timeControlFromStoredValues(
      propWithEffect(forceOptions?.timeMode || storeProps.timeMode, this.onDropdownChange),
      canChangeTimeMode ? allTimeModeKeys : ['realTime'],
      forceOptions?.time ?? storeProps.time,
      forceOptions?.increment ?? storeProps.increment,
      forceOptions?.days ?? storeProps.days,
      this.onPropChange,
      this.root.pools,
    );
    this.gameMode = this.propWithApply(forceOptions?.mode ?? storeProps.gameMode);
    this.evenChessSetLevel = this.propWithApply(
      evenChessSetLevelForGameType(this.gameType, storeProps.evenChessSetLevel),
    );
    const legacyTargetLevel = storeProps.evenChessTargetLevel ?? '';
    this.evenChessApplyPreferences = this.propWithApply(storeProps.evenChessApplyPreferences ?? false);
    this.evenChessPlayerTargetLevel = this.propWithApply(
      storeProps.evenChessPlayerTargetLevel ?? legacyTargetLevel,
    );
    this.evenChessTargetLevel = this.evenChessPlayerTargetLevel;
    this.evenChessOpponentTargetLevel = this.propWithApply(storeProps.evenChessOpponentTargetLevel ?? '');
    this.evenChessStrictSearch = this.propWithApply(storeProps.evenChessStrictSearch ?? false);
    this.evenChessConfirmLevelContract = this.propWithApply(
      storeProps.evenChessConfirmLevelContract ?? false,
    );
    this.evenChessFriendLevelMode = this.propWithApply(storeProps.evenChessFriendLevelMode ?? 'auto');
    this.evenChessFriendMyLevel = this.propWithApply(storeProps.evenChessFriendMyLevel ?? '');
    this.evenChessFriendOpponentLevel = this.propWithApply(storeProps.evenChessFriendOpponentLevel ?? '');
    this.ratingMin = this.propWithApply(storeProps.ratingMin);
    this.ratingMax = this.propWithApply(storeProps.ratingMax);
    this.aiLevel = this.propWithApply(storeProps.aiLevel);
    this.color(forceOptions?.color || 'random');

    this.enforcePropRules();
    // Upon loading the props from the store, overriding with forced options, and enforcing rules,
    // immediately save them to the store. This way, the user can know that whatever they saw last
    // in the modal will be there when they open it at a later time.
    this.savePropsToStore();
  };

  private readonly enforcePropRules = () => {
    // reassign with this.propWithApply in this function to avoid calling this.onPropChange

    // replace underscores with spaces in FEN
    if (this.variant() === 'fromPosition') this.fen = this.propWithApply(this.fen().replace(/_/g, ' '));

    if (this.gameMode() === 'rated' && this.ratedModeDisabled()) {
      this.gameMode = this.propWithApply('casual');
    }

    if (this.gameType === 'ai') this.evenChessSetLevel = this.propWithApply(evenChessComputerSetLevel);

    this.ratingMin = this.propWithApply(Math.min(0, this.ratingMin()));
    this.ratingMax = this.propWithApply(Math.max(0, this.ratingMax()));
    if (this.ratingMin() === 0 && this.ratingMax() === 0) {
      this.ratingMax = this.propWithApply(50);
    }
  };

  private readonly savePropsToStore = (override: Partial<SetupStore> = {}) =>
    this.gameType &&
    this.store[this.gameType]({
      variant: this.variant(),
      fen: this.fen(),
      timeMode: this.timeControl.mode(),
      time: this.timeControl.time(),
      increment: this.timeControl.increment(),
      days: this.timeControl.days(),
      gameMode: this.gameMode(),
      evenChessSetLevel: evenChessSetLevelForGameType(this.gameType, this.evenChessSetLevel()),
      evenChessTargetLevel: this.evenChessPlayerTargetLevel(),
      evenChessApplyPreferences: this.evenChessApplyPreferences(),
      evenChessPlayerTargetLevel: this.evenChessPlayerTargetLevel(),
      evenChessOpponentTargetLevel: this.evenChessOpponentTargetLevel(),
      evenChessStrictSearch: this.evenChessStrictSearch(),
      evenChessConfirmLevelContract: this.evenChessConfirmLevelContract(),
      evenChessFriendLevelMode: this.evenChessFriendLevelMode(),
      evenChessFriendMyLevel: this.evenChessFriendMyLevel(),
      evenChessFriendOpponentLevel: this.evenChessFriendOpponentLevel(),
      ratingMin: this.ratingMin(),
      ratingMax: this.ratingMax(),
      aiLevel: this.aiLevel(),
      ...override,
    });

  private readonly savePropsToStoreExceptRating = () =>
    this.gameType &&
    this.savePropsToStore({
      ratingMin: this.store[this.gameType]().ratingMin,
      ratingMax: this.store[this.gameType]().ratingMax,
    });

  myRating = () => this.root.data.ratingMap && Math.abs(this.root.data.ratingMap[this.selectedPerf()]);
  isProvisional = () => (this.root.data.ratingMap ? this.root.data.ratingMap[this.selectedPerf()] < 0 : true);

  private readonly onPropChange = () => {
    if (this.isProvisional()) this.savePropsToStoreExceptRating();
    else this.savePropsToStore();
    this.root.redraw();
  };

  private readonly onDropdownChange = () => {
    // Handle rating update here
    this.enforcePropRules();
    if (this.isProvisional()) {
      this.ratingMin(-500);
      this.ratingMax(500);
      this.savePropsToStoreExceptRating();
    } else {
      if (this.gameType) {
        this.ratingMin(this.store[this.gameType]().ratingMin);
        this.ratingMax(this.store[this.gameType]().ratingMax);
      }
      this.savePropsToStore();
    }
    this.root.redraw();
  };

  private readonly propWithApply = <A>(value: A) => propWithEffect(value, this.onPropChange);

  openModal = (
    gameType: Exclude<GameType, 'local'>,
    forceOptions?: ForceSetupOptions,
    friendUser?: string,
  ) => {
    this.root.leavePool();
    this.gameType = gameType;
    this.loading = false;
    this.fenError = false;
    this.lastValidFen = '';
    this.friendUser = friendUser || '';
    this.variantMenuOpen(false);
    this.forced = forceOptions;
    this.loadPropsFromStore(forceOptions);
  };

  closeModal?: () => void; // managed by view/setup/modal.ts

  stopEvenChessSearchPolling = () => {
    this.evenChessSearchPollToken++;
    if (this.evenChessSearchPollTimer) {
      clearTimeout(this.evenChessSearchPollTimer);
      this.evenChessSearchPollTimer = undefined;
    }
  };

  private readonly openEvenChessSearchRedirect = (status: EvenChessSearchStatus) => {
    if (status.redirectUrl) {
      this.stopEvenChessSearchPolling();
      location.href = status.redirectUrl;
      return true;
    }
    return false;
  };

  private readonly startEvenChessSearchPolling = (status: EvenChessSearchStatus) => {
    this.stopEvenChessSearchPolling();
    if (!status.ok || !status.searchKey) {
      this.openEvenChessSearchRedirect(status);
      return;
    }
    if (this.openEvenChessSearchRedirect(status)) return;

    const token = this.evenChessSearchPollToken;
    const pollUrl = status.pollUrl || `/evenchess/play/search.json?searchKey=${encodeURIComponent(status.searchKey)}`;
    const poll = async () => {
      if (token !== this.evenChessSearchPollToken) return;
      try {
        const nextStatus: EvenChessSearchStatus = await xhr.json(pollUrl);
        if (token !== this.evenChessSearchPollToken) return;
        this.root.evenChessSearchStatus = nextStatus;
        this.root.redraw();
        if (this.openEvenChessSearchRedirect(nextStatus)) return;
        if (nextStatus.ok && nextStatus.searchKey) {
          this.evenChessSearchPollTimer = window.setTimeout(poll, 2000);
        }
      } catch (_) {
        if (evenChessSearchStatusDebugEnabled()) console.warn('EvenChess search poll failed; retrying.');
        if (token === this.evenChessSearchPollToken) this.evenChessSearchPollTimer = window.setTimeout(poll, 3000);
      }
    };

    void poll();
  };

  private readonly showEvenChessPendingPool = (color: ColorChoice) => {
    const poolMember = this.hookToPoolMember(color);
    if (!poolMember) return false;
    this.root.leavePool();
    this.root.evenChessPoolMember = poolMember;
    this.root.setTab('pools');
    return true;
  };

  startEvenChessQuickPoolSearch = async (poolId: string) => {
    this.stopEvenChessSearchPolling();
    const storeProps = this.store.hook();
    const preferredSetLevel = evenChessPreferredSetLevelParam(
      storeProps.evenChessPlayerTargetLevel || storeProps.evenChessTargetLevel,
    );
    const params = new URLSearchParams({
      mode: evenChessSubmitMode('hook', storeProps.gameMode),
      timeControl: evenChessTimeControlKeyForPoolId(poolId),
      setLevel: evenChessSetLevelForGameType('hook', storeProps.evenChessSetLevel).toString(),
      outsideHelp: 'acknowledged',
    });
    const clockParams = evenChessClockParamsForPoolId(poolId);
    if (clockParams) {
      params.set('clockLimitSeconds', clockParams.clockLimitSeconds);
      params.set('clockIncrementSeconds', clockParams.clockIncrementSeconds);
    }
    if (preferredSetLevel) params.set('preferredSetLevel', preferredSetLevel);
    if (storeProps.evenChessConfirmLevelContract) params.set('confirmLevelContract', 'true');

    this.root.leavePool();
    this.root.evenChessPoolMember = { id: poolId };
    this.root.setTab('pools');
    this.root.redraw();

    try {
      const status: EvenChessSearchStatus = await xhr.json(`/evenchess/play/search.json?${params.toString()}`);
      this.root.evenChessSearchStatus = status;
      if (!status.ok) {
        this.root.evenChessPoolMember = undefined;
        this.root.redraw();
        await alert(status.error || 'EvenChess search could not start.');
        return;
      }
      this.root.redraw();
      if (!this.openEvenChessSearchRedirect(status)) this.startEvenChessSearchPolling(status);
    } catch (_) {
      this.root.evenChessPoolMember = undefined;
      this.root.redraw();
      await alert('Sorry, we encountered an error while starting your EvenChess search. Please try again.');
    }
  };

  toggleVariantMenu = () => {
    this.variantMenuOpen.toggle();
    this.root.redraw();
  };

  validateFen = debounce(() => {
    const fen = this.fen();
    if (!fen) return;
    xhr
      .text(
        xhr.url('/setup/validate-fen', {
          fen,
          strict: this.gameType === 'ai' ? 1 : undefined,
        }),
      )
      .then(
        () => {
          this.fenError = false;
          this.lastValidFen = fen;
          this.root.redraw();
        },
        () => {
          this.fenError = true;
          this.root.redraw();
        },
      );
  }, 300);

  ratedModeDisabled = () =>
    // anonymous games cannot be rated
    !this.root.me ||
    this.timeControl.mode() === 'unlimited' ||
    (this.variant() === 'fromPosition' && this.fen() !== INITIAL_FEN) ||
    // variants with very low time cannot be rated
    (this.variant() !== 'standard' && this.timeControl.notForRatedVariant());

  selectedPerf = (): Perf => getPerf(this.variant(), this.timeControl);

  ratingRange = (): string => {
    const rating = this.myRating();
    return rating ? `${Math.max(100, rating + this.ratingMin())}-${rating + this.ratingMax()}` : '';
  };

  hookToPoolMember = (color: ColorChoice): PoolMember | null => {
    const id = evenChessPendingPoolId({
      gameType: this.gameType,
      variant: this.variant(),
      gameMode: this.gameMode(),
      color,
      isRealTime: this.timeControl.isRealTime(),
      clock: this.timeControl.clockStr(),
      poolIds: this.root.pools.map(pool => pool.id),
    });
    return id
      ? {
          id,
          range: this.ratingRange(),
        }
      : null;
  };

  propsToFormData = (color: ColorChoice) =>
    xhr.form({
      variant: keyToId(this.variant(), variants).toString(),
      fen: this.variant() === 'fromPosition' ? this.fen() : undefined,
      timeMode: keyToId(this.timeControl.mode(), timeModes).toString(),
      time: this.timeControl.time().toString(),
      time_range: this.timeControl.timeV().toString(),
      increment: this.timeControl.increment().toString(),
      increment_range: this.timeControl.incrementV().toString(),
      days: this.timeControl.days().toString(),
      days_range: this.timeControl.daysV().toString(),
      mode: this.gameMode() === 'casual' ? '0' : '1',
      ratingRange: this.ratingRange(),
      ratingRange_range_min: this.ratingMin().toString(),
      ratingRange_range_max: this.ratingMax().toString(),
      level: this.aiLevel().toString(),
      evenChessFriendLevelMode: this.gameType === 'friend' ? this.evenChessFriendLevelMode() : undefined,
      evenChessFriendMyLevel: this.gameType === 'friend' ? this.evenChessFriendMyLevel() : undefined,
      evenChessFriendOpponentLevel:
        this.gameType === 'friend' ? this.evenChessFriendOpponentLevel() : undefined,
      color,
    });

  validFen = () => this.variant() !== 'fromPosition' || (!this.fenError && !!this.fen());

  valid = () =>
    this.validFen() &&
    this.timeControl.valid(this.minimumTimeIfReal()) &&
    this.validEvenChessSettings() &&
    this.validConstraints();

  validEvenChessSettings = () =>
    evenChessLevelValid(this.evenChessSetLevel()) &&
    evenChessLevelValid(this.evenChessPlayerTargetLevel()) &&
    this.validEvenChessFriendSettings();

  validEvenChessFriendSettings = () => {
    if (this.gameType !== 'friend') return true;
    const mode = this.evenChessFriendLevelMode();
    const myLevelValid = evenChessLevelValid(this.evenChessFriendMyLevel());
    const opponentLevelValid = evenChessLevelValid(this.evenChessFriendOpponentLevel());
    switch (mode) {
      case 'auto':
        return true;
      case 'my':
        return myLevelValid && this.evenChessFriendMyLevel() !== '';
      case 'opponent':
        return opponentLevelValid && this.evenChessFriendOpponentLevel() !== '';
      case 'both':
        return myLevelValid && opponentLevelValid && this.evenChessFriendMyLevel() !== '' && this.evenChessFriendOpponentLevel() !== '';
      default:
        return false;
    }
  };

  evenChessModeKey = () => {
    return evenChessSubmitMode(this.gameType, this.gameMode());
  };

  evenChessTimeControlKey = () => {
    if (this.timeControl.mode() === 'correspondence') return 'correspondence';
    if (this.timeControl.mode() === 'unlimited') return 'casual';
    const speed = this.timeControl.speed();
    if (speed === 'bullet') return 'bullet';
    if (speed === 'blitz') return 'blitz';
    if (speed === 'classical') return 'classical';
    return 'rapid';
  };

  private readonly invalid = <A>(forced: A | undefined, current: A) =>
    forced !== undefined && forced !== current;

  private readonly validConstraints = () => {
    if (this.forced) {
      if (this.invalid(this.forced.variant, this.variant())) return false;
      if (this.invalid(this.forced.mode, this.gameMode())) return false;
      if (this.invalid(this.forced.timeMode, this.timeControl.mode())) return false;
      if (this.invalid(this.forced.color, this.color())) return false;
      if (
        this.timeControl.mode() === 'correspondence' &&
        this.invalid(this.forced.days, this.timeControl.days())
      )
        return false;
      if (this.timeControl.mode() === 'realTime') {
        if (this.invalid(this.forced.time, this.timeControl.time())) return false;
        if (this.invalid(this.forced.increment, this.timeControl.increment())) return false;
      }
      if (this.invalid(this.forced.fen?.replace(/_/g, ' '), this.fen())) return false;
    }
    return true;
  };

  minimumTimeIfReal = () => (this.gameType === 'ai' && this.variant() === 'fromPosition' ? 1 : 0);

  private readonly submitNativeSetup = async (color: ColorChoice) => {
    this.stopEvenChessSearchPolling();
    const poolMember = this.hookToPoolMember(color);
    if (poolMember) {
      this.root.enterPool(poolMember);
      this.closeModal?.();
      return;
    }

    if (this.gameType === 'hook') this.root.setTab(this.timeControl.isRealTime() ? 'real_time' : 'seeks');
    this.loading = true;
    this.root.redraw();

    let urlPath = `/setup/${this.gameType}`;
    if (this.gameType === 'hook') urlPath += `/${site.sri}`;
    const urlParams = {
      user: this.friendUser || undefined,
      evenChessFriendLevelMode:
        this.gameType === 'friend' ? this.evenChessFriendLevelMode() : undefined,
      evenChessFriendMyLevel:
        this.gameType === 'friend' ? this.evenChessFriendMyLevel() : undefined,
      evenChessFriendOpponentLevel:
        this.gameType === 'friend' ? this.evenChessFriendOpponentLevel() : undefined,
    };
    let response;
    try {
      response = await xhr.textRaw(xhr.url(urlPath, urlParams), {
        method: 'post',
        body: this.propsToFormData(color),
      });
    } catch (_) {
      this.loading = false;
      this.root.redraw();
      await alert('Sorry, we encountered an error while creating your game. Please try again.');
      return;
    }

    const { ok, redirected, url } = response;

    if (!ok) {
      const errs: Record<string, string> = await response.json();
      await alert(
        errs
          ? Object.keys(errs)
              .map(k => `${k}: ${errs[k]}`)
              .join('\n')
          : 'Invalid setup',
      );
      if (response.status === 403) {
        // 403 FORBIDDEN closes this modal because challenges to the recipient
        // will not be accepted. see friend() in controllers/Setup.scala.
        this.closeModal?.();
      }
    } else if (redirected) {
      location.href = url;
    } else {
      this.loading = false;
      this.closeModal?.();
    }
  };

  submit = async () => {
    const color = this.color();
    this.stopEvenChessSearchPolling();
    if (this.gameType === 'ai') {
      this.evenChessSetLevel(evenChessComputerSetLevel);
      this.savePropsToStore({ evenChessSetLevel: evenChessComputerSetLevel });
      await this.submitNativeSetup(color);
      return;
    }
    if (this.gameType === 'friend') {
      await this.submitNativeSetup(color);
      return;
    }

    const preferredSetLevel = evenChessPreferredSetLevelParam(this.evenChessPlayerTargetLevel());
    const params = new URLSearchParams({
      mode: this.evenChessModeKey(),
      timeControl: this.evenChessTimeControlKey(),
      setLevel: this.evenChessSetLevel().toString(),
      outsideHelp: 'acknowledged',
    });
    if (this.timeControl.mode() === 'realTime') {
      params.set('clockLimitSeconds', Math.round(this.timeControl.initialSeconds()).toString());
      params.set('clockIncrementSeconds', Math.round(this.timeControl.increment()).toString());
    }
    if (preferredSetLevel) params.set('preferredSetLevel', preferredSetLevel);
    if (this.evenChessConfirmLevelContract()) params.set('confirmLevelContract', 'true');
    const showsPendingPool = this.showEvenChessPendingPool(color);
    this.loading = true;
    this.root.redraw();

    try {
      const status: EvenChessSearchStatus = await xhr.json(`/evenchess/play/search.json?${params.toString()}`);
      this.root.evenChessSearchStatus = status;
      this.loading = false;
      if (!status.ok) {
        if (showsPendingPool) this.root.evenChessPoolMember = undefined;
        this.root.redraw();
        await alert(status.error || 'EvenChess search could not start.');
        return;
      }
      this.closeModal?.();
      if (!showsPendingPool) this.root.setTab(this.timeControl.isRealTime() ? 'real_time' : 'seeks');
      this.root.redraw();
      if (!this.openEvenChessSearchRedirect(status)) this.startEvenChessSearchPolling(status);
    } catch (_) {
      this.loading = false;
      if (showsPendingPool) this.root.evenChessPoolMember = undefined;
      this.root.redraw();
      await alert('Sorry, we encountered an error while starting your EvenChess search. Please try again.');
    }
  };
}
