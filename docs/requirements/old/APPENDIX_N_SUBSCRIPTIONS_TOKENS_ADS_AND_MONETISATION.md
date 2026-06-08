# Appendix N - Subscriptions, Tokens, Ads, and Monetisation

**Suite:** EvenChess-Lichess Version 1
**Status:** Live appendix
**Generated:** 2026-05-28

## Purpose

Preserves Amendment 1 monetisation/token requirements.


Monetisation changes access, quotas, and convenience only. It must not change rated fairness.

| Plan/state | Requirement |
| --- | --- |
| New account onboarding | 10 game tokens, 3 full-quality match summaries, 1 performance-summary token unlocked after 10 completed games. |
| Free ad-supported | May watch rewarded ads for game tokens; earned ad game-token cap = 3. |
| Standard | $2.50 AUD/week, billed every 4 weeks at $10 AUD; ad-free game access subject to reasonable-use/abuse controls. |
| Premium | $4 AUD/week, billed every 4 weeks at $16 AUD; Standard plus 10 match summaries/day and 1 performance summary/day. |

## N.3 Account rules

TOKEN-L1-001: Accounts are email/password based unless equivalent secure path satisfies requirements.
TOKEN-L1-002: Users choose username linked to email/account.
TOKEN-L1-003: Same email cannot create another active account.
TOKEN-L1-004: Reused closed-account email receives no onboarding tokens.
TOKEN-L1-005: Multiple accounts may exist but each has separate ECR, tokens, subscriptions, quotas, leaderboard eligibility.
TOKEN-L1-006: Tokens/quotas are non-transferable.
TOKEN-L1-007: Leaderboard eligibility requires enough games/rating certainty.

## N.4 Rewarded ads and token consumption

Rewarded ad completion grants 1 game token; free accounts hold maximum 3 earned ad tokens; full bank blocks another token ad. Tokens are consumed only when a valid game starts and reaches meaningful play. Failed queue does not consume. Opponent abort before meaningful play does not consume or is refunded. Token holder abort after accepting/meaningful play may consume or trigger cooldown. Platform outage aborts do not consume.

## N.6 Fairness boundary

SUB-L1-001: Subscription, ads, tokens, quotas do not bypass fairness.
SUB-L1-002: Payment must not change Set Level, Used Level, Assistance Load, Used Offset, ECR, matchmaking fairness, Stockfish profile, AI exactness, Target isolation, or live coaching strength.
SUB-L1-003: Access/quota can change who starts games or gets summaries; rated fairness remains unchanged once game starts.
SUB-L1-004: Premium is never stronger live help.

MVP excludes phone verification, device/session risk scoring, same-IP creation limits, high-risk cluster token delays unless later approved. Tests cover email/token eligibility, caps, consumption/refund, and fairness non-effect.
