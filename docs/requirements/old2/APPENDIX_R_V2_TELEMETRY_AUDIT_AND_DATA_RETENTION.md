# Appendix R — Telemetry, Audit, and Data Retention

## R.1 Purpose

This appendix defines events, audit records, and ECE history retention.

## R.2 Live audit events

REQ-R-V2-001: Audit match contracts.

REQ-R-V2-002: Audit Set Level at game start.

REQ-R-V2-003: Audit every Used Level increase.

REQ-R-V2-004: Audit each ECE payload generated for live play.

REQ-R-V2-005: Audit each coaching render, hide, expand, suppress, and proposed-move check.

REQ-R-V2-006: Audit final Used Level, Assistance Load, Used Offset, and rating settlement.

## R.3 Display events

REQ-R-V2-010: Display Engine should emit events for shown/hidden/expanded cards and overlays where feasible.

REQ-R-V2-011: Proposed move checks should record arrow move, legality, level, and whether result was shown.

## R.4 Retention

REQ-R-V2-020: Store enough game/ECE history to support Live White, Live Black, Live Both review modes.

REQ-R-V2-021: Retention may be limited by rolling recent games and paid saved games.

REQ-R-V2-022: Privacy and storage controls must avoid keeping unnecessary raw AI/provider data.

REQ-R-V2-023: AI prompts/responses must be logged only according to safe diagnostic policy and must not contain secrets.
