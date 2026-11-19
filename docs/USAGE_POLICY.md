# Usage limits

Routine Builder has no paid tier, purchases or subscriptions.

## Android and iOS

Standard native builds allow 10 cloud-enabled attempts per installation in a rolling 24-hour window. Each reservation expires after 24 hours, rather than at midnight. Confirmed on-device results refund the reservation. Failed or cancelled cloud attempts count because the service may already have received the request. Invalid input does not reserve an attempt.

Explicit local-only generation, manual editing, saving and exporting do not consume this allowance. Android blocks cloud attempts when its usage file is corrupt or unwritable. iOS stores its courtesy counter in app preferences.

The ledger persists across ordinary restarts and in-place updates. It is a local courtesy limit, not a tamper-proof identity system: reinstalling, clearing data, changing the client or advancing the clock can bypass it.

## Web

The web application has no per-browser generation counter. Cloud availability depends on the Firebase project's service quotas and configuration. Local generation requires a compatible browser and downloaded model; manual editing does not require either.

## Self-hosting

Configure server quotas, abuse protection and billing controls for your own Firebase project. Client limits and billing alerts are not hard spending caps. App Check should remain enforced. See [Firebase AI Logic quotas](https://firebase.google.com/docs/ai-logic/quotas).

## Evaluation builds

A dedicated evaluation build can omit the per-installation courtesy limit. Build Android with `-PjudgingAccess=true`; use the iOS `Evaluation` build configuration. Ordinary Debug/Release builds default to the standard allowance. This does not disable App Check, authorise cloud calls without consent, or remove Firebase service quotas and billing controls. Existing usage ledgers are preserved, so returning to a standard build does not reset the allowance. The web app already has no per-browser counter.

Never embed App Check debug tokens or credentials in source or distribution instructions. Production attestation must be tested on the actual distribution route. An evaluation build alone does not guarantee uninterrupted judging access.
