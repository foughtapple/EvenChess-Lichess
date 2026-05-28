# EvenChess-Lichess Patch Map

**Suite:** EvenChess-Lichess Version 1

## 1. Rule

Whenever Codex edits a file from upstream Lichess/lila, it must add/update a patch map entry before the phase is complete.

## 2. Risk levels

| Risk | Meaning |
| --- | --- |
| Low | Small hook/delegator/config with low conflict risk. |
| Medium | Moderate integration with game/UI/rating flow. |
| High | Core game/rating/matchmaking/UI file likely to conflict. |
| Unknown | Not enough inspection; resolve before release. |

## 3. Registry template

| Entry ID | File touched | Why core file was touched | Linked requirement | Merge risk | Tests | Can later isolate? | Notes/status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PM-0001 | Example: app/... | Example hook for EvenChess mode flag. | MODE-L1-001 | Unknown | Example test. | TBD | Replace when real work starts. |

## 4. Detail template

```markdown
### PM-YYYY-### - <short title>
- File touched:
- Upstream source area:
- Why core Lichess file was touched:
- Linked EvenChess requirement(s):
- Upstream merge risk:
- Tests added/updated:
- Normal chess regression performed:
- EvenChess regression performed:
- Can later be isolated:
- Isolation idea:
- Notes:
```

Before upstream sync, review all Medium/High/Unknown entries.
