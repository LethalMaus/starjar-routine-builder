# Usage limits

Routine Builder has no paid tier, purchases or subscriptions.

## Android

Android allows 10 cloud-enabled attempts per installation in a rolling 24-hour window. Each reservation expires after 24 hours, rather than at midnight. Confirmed on-device results refund the reservation. Failed or cancelled cloud attempts count because the service may already have received the request. Invalid input does not reserve an attempt.

Explicit local-only generation, manual editing, saving and exporting do not consume this allowance. Corrupt or unwritable usage storage blocks cloud attempts but does not block those features.

The ledger persists across ordinary restarts and in-place updates. It is a local courtesy limit, not a tamper-proof identity system: reinstalling, clearing data, changing the client or advancing the clock can bypass it.

## Web

The web application has no per-browser generation counter. Cloud availability depends on the Firebase project's service quotas and configuration. Local generation requires a compatible browser and downloaded model; manual editing does not require either.

## Self-hosting

Configure server quotas, abuse protection and billing controls for your own Firebase project. Client limits and billing alerts are not hard spending caps. App Check should remain enforced. See [Firebase AI Logic quotas](https://firebase.google.com/docs/ai-logic/quotas).
