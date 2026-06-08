# Appendix T — Operations, Deployment, and Incident Response

## T.1 Purpose

This appendix defines operational controls for EvenChess V2.

## T.2 Feature flags

REQ-T-V2-001: Major EvenChess systems must be feature-flagged: MMR Engine, ECE live calls, Display Engine overlays, AI summaries, proposed-move mode, full-game mode, token gates, and ads.

REQ-T-V2-002: Feature flags must not silently change rated fairness without audit/versioning.

## T.3 Health checks

REQ-T-V2-010: Monitor ECE latency, Stockfish latency, AI latency/cost/fallback, queue times, token flow, custom analysis consumption, and rating settlement health.

REQ-T-V2-011: Monitor overlay stale-payload errors and Display Engine render failures.

## T.4 Incident handling

REQ-T-V2-020: Operators must be able to pause ECE live help, AI summaries, proposed-move analysis, custom review, ads, tokens, and paid promotions.

REQ-T-V2-021: Asymmetric assistance outages may require no-rate/annul/review flows.

REQ-T-V2-022: Campaigns must pause if copy implies cheating, hidden engine use, or pay-to-win help.

## T.5 Deployment

REQ-T-V2-030: Deployment must preserve local Lichess/lila-docker dev flow.

REQ-T-V2-031: Each phase must include rollback notes and tests.
