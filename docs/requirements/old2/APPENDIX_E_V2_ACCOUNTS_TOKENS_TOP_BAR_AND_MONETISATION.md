# Appendix E — Accounts, Tokens, Top Bar, and Monetisation

## E.1 Purpose

This appendix defines account/token/subscription requirements integrated into Lichess-style account and top-bar surfaces.

## E.2 Account model

REQ-E-V2-001: Use Lichess account foundations where possible.

REQ-E-V2-002: EvenChess account state must include ECR/MMR records, token balances, subscriptions, review rights, saved-game slots, custom analysis tokens, and settings.

REQ-E-V2-003: Same email/account anti-duplication and onboarding-token eligibility rules from prior amendments remain active unless superseded.

## E.3 Token balances

REQ-E-V2-010: Game-token balance must be visible in a top-bar/account-native location.

REQ-E-V2-011: Clicking game-token balance must take the player to the token/ad screen.

REQ-E-V2-012: Summary/review/custom-analysis tokens should be visible in account/subscription/review surfaces, not necessarily crowded into the top bar.

REQ-E-V2-013: Token displays must not imply tokens buy stronger live rated help.

## E.4 Game tokens

REQ-E-V2-020: A game token is consumed only when a valid game starts and passes the meaningful-play threshold.

REQ-E-V2-021: Failed queue/search must not consume a token.

REQ-E-V2-022: Opponent abort before meaningful play must not consume or must immediately refund the token.

REQ-E-V2-023: Standard/Premium access may bypass ad-supported game-token limits according to subscription rules, but must not affect rated fairness.

## E.5 Review and custom analysis tokens

REQ-E-V2-030: Custom ECE analysis at high levels, especially L10 for both sides, may require custom analysis tokens.

REQ-E-V2-031: Full-game ECE review may require a match-review or full-analysis token.

REQ-E-V2-032: Live ECE outputs produced during a game are recorded as part of the game history and do not require separate custom analysis tokens.

REQ-E-V2-033: Reanalysis at custom levels may consume tokens according to the Review appendix.

## E.6 Saved games and paid tier storage

REQ-E-V2-040: The system may retain a rolling set of recent games with ECE history.

REQ-E-V2-041: Paid tiers may allow users to mark games as saved so they persist beyond normal retention.

REQ-E-V2-042: A game saved while the account is paid should remain saved if the user later downgrades, but new saves may require an active eligible tier.

REQ-E-V2-043: Exact free/paid saved-game counts are a product decision and must be configurable.
