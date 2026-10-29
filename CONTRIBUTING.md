# Contributing

Bug reports, workflow suggestions and pull requests are welcome. Keep reports reproducible with fictional people and reserved example email domains. Do not include private contact information, court records, exported handoffs, signing keys or device logs containing personal details.

1. Fork the repository and create a focused branch.
2. Keep court-source changes separate from operational updates. Preserve stable member and title identifiers and backwards compatibility with saved/handoff data.
3. Add regression coverage for changes to import matching, reconciliation, serialization or encryption.
4. Run `test.ps1` and `build.ps1`; explain any checks you could not run.
5. Submit a pull request describing the user-visible behavior and validation.

Before committing, inspect the diff and tracked file list. Only fictional test fixtures belong in examples or tests. Do not add build outputs or local signing keys.

The initial build workflow is Windows/PowerShell-based. Improvements for standard Android tooling, accessibility and broader court spreadsheet layouts are welcome. Preserve the offline behavior unless a proposed design explicitly explains a change to it.
