# User guide

## Roster and configuration

The roster supports one grand-jury term. Current role is Juror or Alternate; service status is Active, Resigned, Excused or Completed. Departed members remain in the historical roster. Only active members count toward regular seats and alternate slots. Seat totals are operational counts, not a quorum ruling.

Config contains the app-header name, YYYY-YYYY term, capacities, eight juror-title names and roster filters. A member can hold multiple titles. Renaming a title slot updates its name without losing assignments; unassign a title before clearing its slot. Roster cards can be searched by name, email, position and assigned titles.

Roster-filter checkboxes can combine active regular jurors, active alternates, resigned members and other departures. Filters affect cards only, not counts or exports. If no boxes are selected, no cards are shown; no data is removed.

## Contacts, onboarding and county email

Previous/personal email and current county email are separate fields. Changes retain old addresses in email history. County-email test states are Not tested, Test sent, Confirmed and Failed. The app records the operator's test status; it does not send test mail. Changing a county address resets its confirmation. Save the new address before recording its test result.

Oath completion, orientation, checked contacts, county-account access, titles and operational notes are maintained manually. Court imports do not directly overwrite those operational fields.

A member form can generate a suggested county address. Handoff also provides a bulk preview for active members with blank county emails. Existing addresses and departed members are skipped in bulk generation. Duplicate suggestions require county confirmation; numeric suffixes are not guessed. Name accents and punctuation are normalized, so unusual names need review.

The domain and pattern can be changed in Handoff. Patterns use `{first_initial}`, `{first}` and `{last}` with optional dots, underscores or hyphens. Save the rule before generating. Suggestions do not establish that the county has created an account.

## Distribution-list work

From a saved member, queue Add, Update or Remove work for a named list. Make the real external list change, then mark the task complete in Work. Completed Add/Update work records membership; Remove work removes it. Pending work may be cancelled with history retained.

Changing a county email queues updates for recorded memberships. A departure cancels pending Add/Update work and queues removal from recorded memberships. Memberships not recorded in the app cannot be inferred.

## Court import

Use XLSX or UTF-8 CSV/TSV with separate First Name and Last Name columns. The parser looks for those headings within the first 40 rows. Confirm all sheet roles/status and mappings before staging.

The default mappings include Email Address → Previous/personal email and New/Current Email Address → County email. Confirm whether these meanings fit the actual court file. Spreadsheet Position is independent of current roster role: an old alternate number may remain even after promotion to a regular seat.

Each selected sheet has a default role and status, which can be changed before staging. Resigned-sheet defaults do not establish the member's historical role; correct it during review when needed.

Staging shows new members, changed fields, ambiguous identities and, optionally, active members not found in a complete source. Matches prefer court ID, then unique email or name. Review target identity and selected fields. No fuzzy merge is performed. Missing rows never automatically establish a departure.

Blank source cells preserve existing values unless explicit clears are enabled. Malformed email or date values are shown as warnings and omitted from proposed fields. Excel date serials are converted, including the 1904 date system. Formulas are not recalculated; where present, their stored values are used. Recalculate and save the workbook before importing.

Resolve pending proposals before staging another file. Reviews persist across app restarts. Data with unresolved source warnings may produce the same warnings again on reimport.

Limitations: legacy binary XLS and encrypted workbooks are unsupported; input limit is 8 MB, expanded XLSX limit 32 MB, and each sheet is limited to 10,000 rows and the first 128 columns. Columns beyond that range are not imported.

## Export and leadership transfer

CSV is a readable complete-roster snapshot, independent of display filters. It contains contact details and operational fields, but not the full history, task queue or import review. Formula-like cell contents are escaped. CSV is unencrypted.

An encrypted `.jury` handoff contains the full term, roster, address history, tests, onboarding, titles, memberships, tasks, import decisions and history. Choose a passphrase of at least 12 characters and transfer it separately from the file. It cannot be recovered if forgotten.

Restore decrypts and previews term and counts before replacing the working roster. It does not merge independent leaders' edits. A local copy from before the latest restore is kept for recovery. The app is single-device and has no automatic synchronization.
