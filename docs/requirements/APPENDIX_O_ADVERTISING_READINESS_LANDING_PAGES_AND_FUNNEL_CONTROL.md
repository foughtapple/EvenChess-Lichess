# Appendix O - Advertising Readiness, Landing Pages, and Funnel Control

**Suite:** EvenChess-Lichess Version 1  
**Status:** Live appendix  
**Generated:** 2026-05-28

## Purpose

Preserves Amendment 2 marketing/funnel requirements.


MKT-L1-001: Marketing config, variants, windows, subscriptions, ads, tokens, offers, prompts do not bypass fairness.  
MKT-L1-002: Advertising/funnel control cannot alter Set Level, Used Level, Assistance Load, Used Offset, ECR, matchmaking fairness, live strength, Target isolation, Stockfish exposure, AI exactness, or coaching permission.  
MKT-L1-003: Paid plans change access/quota/convenience/review frequency, not live rated strength.  
MKT-L1-004: Campaign variants change copy order/emphasis only.  
MKT-L1-005: Public copy must not imply cheating, hidden engine use, off-platform assistance, or stronger paid live help.

## O.3 Marketing config source

Backend-readable config is required. It supports hero headline/subheading, offer chip, trust strip, plan wording, pricing, FAQ, demo URL, launch region, landing variant, rewarded-ad wording, summary-token wording, kill-switch flag, and play windows. Config must have version/timestamp and safe fallback.

Safe default: "Chess that teaches you while you play. Opponent knows. Help is capped. Rating adjusts."

## O.4 Landing defaults

| Role | Default copy |
| --- | --- |
| Headline | Chess that teaches you while you play. |
| Subheading | EvenChess is a separate assisted chess mode where platform coaching is built into the rules. Your opponent sees it, help is capped, and ECR accounts for what you use. |
| CTA | Start with 10 free games. |
| Offer chip | 10 game tokens; 3 full match summaries; 1 performance summary after 10 completed games. |
| Trust strip | Opponent knows; Help is capped; Rating adjusts. |

Landing supports Hero, Trust, Difference, How it works, Proof, Pricing, FAQ, Final CTA. Variants: default, adult_improver, parent_learning, fair_rating, summary_loop, free_tokens. Forbidden phrases include cheat legally, secret engine, use Stockfish during games, beat stronger players with AI, best move shown live, Premium gives stronger help. Pricing includes fairness footnote.

## O.5 Offers, windows, attribution

Free offer states 10 game tokens, 3 match summaries, 1 performance summary after 10 games. Standard displays $10 AUD/4 weeks and $2.50/week. Premium displays $16 AUD/4 weeks and $4/week plus quotas. Play windows are backend-configurable and may show Play now/Next window but must not manipulate hidden queue/fairness.

Capture UTM source/medium/campaign/content/term, click IDs, variant, first/latest campaign, signup, first game, first subscription. Required events include landing_page_view, view_pricing, begin_signup, sign_up_complete, first_game_started/completed, games_completed_3, ten_games_completed, rewarded_ad_complete, summary views, checkout_start, purchases, renew/cancel.

Admin controls include marketing_site_enabled, active_landing_variant, offer version, play windows, rewarded_ads_enabled, plan flags, paid_acquisition_mode, campaign_pause_notice. Paid acquisition pauses if tracking/payments/queue/copy safety breaks.
