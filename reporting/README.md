# Private suggestion reports

The Android app provides an explicit confirmation before sending a selected suggestion and optional comment. Original prompts and other plan items are not included. iOS and web do not yet expose this action.

The HTTPS endpoint requires Firebase App Check. Firestore client rules deny all direct reads and writes. Only server-side project identities can access reports; never grant public database access. Requests are validated against an allowlist, bounded in size and capped at 100 new reports per UTC day. Random request IDs make retries idempotent. Errors are shown in the app; reports are not silently queued.

Reports contain name, description, comment, creation time and expiry time. The scheduled function deletes expired reports hourly, after 30 days. Daily aggregate counters expire after two days. Infrastructure may maintain separate technical request logs; application code does not log report text or tokens.

## Deployment

Use your own billing-enabled Firebase project, configure App Check for the Android app, create a Firestore Native database in `europe-west3`, and deploy with:

```
npm ci --prefix reporting
npm test --prefix reporting
firebase deploy --only firestore:rules,functions:reporting --project YOUR_PROJECT
firebase functions:artifacts:setpolicy --location europe-west3 --days 7 --project YOUR_PROJECT
```

The Android client derives its endpoint from its configured Firebase project ID. Do not point a fork at another developer's backend. Keep service credentials and App Check debug tokens out of source control. Enable budget alerts and review your service quotas. The function instance limit and application rate limit bound processing but are not hard spending caps.

Use the Firebase console's private Firestore view to review `suggestionReports`. Review reports to improve prompts, filtering and generation safeguards; do not copy reports into public issues or AI prompts. Restrict project IAM membership to authorised maintainers. No automated emails or public report dashboard are provided.

Validate rejected unauthenticated requests, successful App Check requests, exact stored fields, duplicate retries and scheduled deletion using synthetic data before distributing a build. Delete test reports and temporary debug registrations afterward. The Android instrumented `SuggestionReportTest` covers explicit confirmation, cancellation, submitted fields and retry feedback; it does not replace live endpoint validation.
