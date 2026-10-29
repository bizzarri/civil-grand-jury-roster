# Privacy and handoff

The Android app stores records in private app storage and has no network permission, accounts, analytics or cloud synchronization. The system document picker grants access to the individual files selected by the operator. Android automatic app backup is disabled.

Local records rely on Android's device storage protection and screen lock. There is no separate app login, independent user access control, or application-level encryption of the local JSON record. An unlocked device can expose the roster through the app. Local history is an operational log, not a tamper-proof audit system.

The full handoff format uses AES-256-GCM, a random 16-byte salt, random 12-byte IV, and PBKDF2-HMAC-SHA256 at 210,000 iterations. Incorrect passphrases and altered ciphertext fail authentication. Handoff passphrases protect exports, not an unlocked phone. The implementation has regression tests but has not received an independent security assessment.

CSV exports and the original court workbook are not protected by handoff encryption. Use destinations and retention practices appropriate for contact information. Export a handoff periodically and before uninstalling, losing access to, or replacing a device. The app cannot recover a forgotten handoff passphrase or an unbacked-up deleted installation.

Never commit operational data, court workbooks, handoffs, signing material or credentials to this repository. The ignore file is only a guardrail: review staged files before every commit. The example roster contains fictional people and `example.invalid` addresses.
