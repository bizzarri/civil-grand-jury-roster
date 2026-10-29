# Validation

The public test suite uses synthetic records only:

| Suite | Checks |
| --- | ---: |
| Core domain/parser/encryption | 21 |
| Persistence and encrypted handoff | 5 |
| County email generation | 16 |
| Configuration and titles | 15 |
| Roster filters | 14 |
| **Total** | **71** |

Checks cover preserving operational notes and onboarding, review-before-apply, verification resets, email history, departures/vacancies, list work, ambiguous/stale import changes, CSV quoting/formula escaping, encryption tamper/password failures, save/reopen, configuration, old-format defaults, generated addresses, stable title assignments and display filters.

A synthetic CSV import fixture is provided in examples/roster.csv. XLSX import was exercised during development, but no private source workbook or aggregate private-roster assertions are included in this repository. Broader public XLSX fixtures and automated device UI coverage are future work.

The APK build compiles against API 36 with minimum API 26, then verifies v2/v3 signing and ZIP alignment. Earlier development versions were installed and visually checked on Android hardware. JVM tests and a successful APK build do not establish behavior on every phone or tablet, nor independently certify security.
