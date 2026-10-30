# Civil Grand Jury Roster

An offline Android app for maintaining a working civil grand-jury roster and transferring it to the next foreperson.

**Current source version: 1.7.** This is an early working application. It opens with an empty roster; no real juror information, court spreadsheets, account credentials or signing keys are included.

## What it does

- Tracks regular jurors, alternates, resignations, other departures and unfilled seats for one term.
- Keeps previous/personal email separate from county email, retains previous addresses, and records county-email test progress.
- Tracks onboarding and foreperson notes independently of court-source fields.
- Queues distribution-list changes for manual completion and records confirmed memberships.
- Imports XLSX and UTF-8 CSV/TSV, compares source rows with the working roster, and stages changes for per-member and per-field review.
- Preserves operational notes and checks when court data changes. Missing source rows never automatically remove or resign a member.
- Supports configurable app-header name, term years and exact start/end dates, seat counts, eight editable titles, and saved roster-status filters.
- Generates county-email suggestions using a configurable naming pattern. The Amador default is first initial + last name at `amadorcounty.gov`. Generated addresses still need county issuance and testing.
- Schedules configurable recurring meetings, individual dates and holiday exceptions, with attendance records preserved when dates are moved, cancelled or removed.
- Records in-person and online attendance from meeting-day roster checkboxes, with full attendance editing under Work.
- Completes oath and orientation onboarding in bulk through a preview.
- Exports a readable CSV and a passphrase-encrypted full handoff, including history and pending work.

Meeting scheduling and attendance are included. Quorum calculations remain deferred. The app does not send email, provision county accounts, modify external mailing lists, synchronize between devices, or calculate legal quorum requirements.

## For forepersons

This repository currently publishes **source code**. No prebuilt public APK release is included. Developers can build an installer using the instructions below.

1. Open **Config** to set the display name, term years, regular seats and alternate slots. Defaults are 19 regular seats and 20 alternate slots; adjust them for your organization.
2. Use **Import** to select a court workbook, confirm sheet roles/status and column mappings, and stage the differences.
3. Review each proposed change before accepting it. Use member forms for onboarding, email checks, operational notes and titles.
4. Use **Work** to track list changes. Make the actual list changes outside the app before marking tasks complete.
5. Create an encrypted handoff periodically and before replacing or uninstalling the app.

See [the user guide](docs/USER_GUIDE.md), [privacy and handoff notes](docs/PRIVACY.md), and the [fictional sample CSV](examples/roster.csv).

## Build on Windows

Requirements:

- PowerShell 7 or newer
- JDK 17 or newer, with `JAVA_HOME` set or passed explicitly
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0

The app uses Android platform APIs directly. There are no third-party runtime libraries and no Gradle setup is required. The current build script is Windows-only; Android Studio project integration and other host platforms are future improvements.

```powershell
git clone https://github.com/bizzarri/civil-grand-jury-roster.git
cd civil-grand-jury-roster

./build.ps1 `
  -BuildTools "$env:LOCALAPPDATA/Android/Sdk/build-tools/36.0.0" `
  -AndroidJar "$env:LOCALAPPDATA/Android/Sdk/platforms/android-36/android.jar" `
  -JavaHome $env:JAVA_HOME
```

Output: `dist/CivilGrandJuryRoster-1.7.apk`. Minimum supported Android version is 8.0 (API 26); target/compile SDK is 36.

The script uses AAPT2, javac, D8, zipalign and apksigner, then verifies signing and alignment. It generates a **local development signing key** under `.build/`; the key and built APK are ignored by Git. Keep your key to build future in-place updates. An APK signed with a different key cannot replace an existing installation. Export a handoff before changing installation/signing arrangements.

The development password `android` in the script is an intentional standard local-development value, not a service credential or production signing secret. Establish your own protected release-signing process before distributing maintained releases. No release-signing material is published here.

The Android package identifier remains `com.mauri.juryroster` for continuity with existing installations. Changing the configured display name does not change that identifier or the fixed Android launcher label.

## Run tests

```powershell
./test.ps1 `
  -AndroidJar "$env:LOCALAPPDATA/Android/Sdk/platforms/android-36/android.jar" `
  -JavaHome $env:JAVA_HOME
```

The first run downloads the pinned `org.json:json:20240303` artifact from Maven Central into an ignored test-dependency directory and checks its SHA-256. It is used only for JVM tests; Android supplies JSON at runtime. For an offline run, pass `-JsonJar` with a copy of that pinned artifact.

The 143 synthetic-data checks cover import review, preserving operations, resignations/vacancies, list changes, CSV quoting, encryption/password/tamper handling, save/reopen, old-format defaults, email generation, titles, exact term dates, configuration, roster filters, bulk onboarding, meeting schedules, attendance modes, historical snapshots and schedule exceptions. No test requires a real court workbook. Test coverage is described in [VALIDATION.md](VALIDATION.md).

## Contributing and reporting problems

Use [GitHub Issues](https://github.com/bizzarri/civil-grand-jury-roster/issues) for ordinary bugs and feature requests. **Do not upload real rosters, private handoffs, contact details, account credentials, or screenshots containing them.** Reproduce issues with fictional data instead.

See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md). The app is community-maintained and is not an official court or county product.

## License

[MIT](LICENSE). Copyright remains with the authors identified in the license. The Amador county-email default is a configurable public naming convention, not an affiliation or endorsement.
