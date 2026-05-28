# EvenChess-Lichess Local Stage 1 Environment

## Current local setup

- Docker Desktop is installed and working.
- WSL Ubuntu is installed and working.
- lila-docker advanced setup has run from ~/dev/lila-docker.
- Local Lichess site is intended to run at http://localhost:8080/.
- Editable Lila source is at ~/dev/lila-docker/repos/lila.

## Purpose

Stage 1 proves local Lichess can run and be safely modified before EvenChess product code is implemented.

## One-command startup target

Future helper scripts should provide:

- scripts/evenchess-local-start.sh
- scripts/evenchess-local-status.sh
- scripts/evenchess-local-stop.sh

The startup helper should start lila-docker, print service status, print URLs, and remind the user of seeded test accounts.

## Seeded accounts

The local database seed includes test users. Password used during setup: password.

Use seeded users for local account/game testing.

## Rule

This document describes the local environment only. It is separate from the final EvenChess-Lichess Version 1 requirements suite.
