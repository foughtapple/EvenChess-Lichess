import Lpv from '@lichess-org/pgn-viewer';
import type { Opts } from '@lichess-org/pgn-viewer/interfaces';

import { requestIdleCallbackSafe } from 'lib';
import { installEvenChessUniversalOverlay } from 'lib/evenchessUniversalOverlay';
import { initMiniBoards } from 'lib/view';

import { renderHistoryChart } from './chart';
import { renderEvenChessOpeningAi } from './evenchessOpeningAi';
import type { OpeningPage } from './interfaces';
import panels from './panels';
import { init as searchEngine } from './search';
import renderPlaceholderWiki from './wiki';

export function initModule(data?: OpeningPage): void {
  data ? page(data) : searchEngine();
}

function page(data: OpeningPage) {
  $('.opening__intro .lpv').each(function (this: HTMLElement, index: number) {
    const viewer = Lpv(this, {
      pgn: this.dataset['pgn']!,
      initialPly: 'last',
      showMoves: 'bottom',
      showClocks: false,
      showPlayers: false,
      chessground: cgConfig,
      menu: {
        getPgn: {
          enabled: true,
          fileName: (this.dataset['title'] || this.dataset['pgn'] || 'opening').replace(' ', '_') + '.pgn',
        },
      },
    });
    installOpeningViewerOverlay(this, viewer, `opening-intro-${index}`);
  });
  initMiniBoards();
  highlightNextPieces();
  renderEvenChessOpeningAi(data);
  panels($('.opening__panels'), id => {
    if (id === 'opening-panel-games') loadExampleGames();
  });
  searchEngine();
  requestIdleCallbackSafe(() => {
    renderHistoryChart(data);
    renderPlaceholderWiki(data);
  });
}

const cgConfig: Opts['chessground'] = {
  coordinates: false,
};

const loadExampleGames = () =>
  $('.opening__games .lpv--todo')
    .removeClass('lpv--todo')
    .each(function (this: HTMLElement, index: number) {
      const viewer = Lpv(this, {
        pgn: this.dataset['pgn']!,
        initialPly: parseInt(this.dataset['ply'] || '99'),
        showMoves: 'bottom',
        showClocks: false,
        showPlayers: true,
        chessground: cgConfig,
        menu: {
          getPgn: {
            enabled: true,
            fileName: (this.dataset['title'] || 'game').replace(' ', '_') + '.pgn',
          },
        },
      });
      installOpeningViewerOverlay(this, viewer, `opening-game-${index}`);
    });

const installOpeningViewerOverlay = (element: HTMLElement, viewer: ReturnType<typeof Lpv>, gameId: string) => {
  installEvenChessUniversalOverlay({
    surface: 'opening',
    getFen: () => viewer.curData().fen,
    getPly: () => (viewer.curData() as { ply?: number }).ply ?? 0,
    getGameId: () => gameId,
    getSide: () => viewer.orientation(),
    getOrientation: () => viewer.orientation(),
    getBoardElement: () => element.querySelector<HTMLElement>('cg-board')?.parentElement,
  });
};

const highlightNextPieces = () => {
  $('.opening__next cg-board').each(function (this: HTMLElement) {
    Array.from($(this).find('.last-move'))
      .map(el => el!.style.transform)
      .forEach(transform => {
        $(this).find(`piece[style="transform: ${transform};"]`).addClass('highlight');
      });
  });
};
