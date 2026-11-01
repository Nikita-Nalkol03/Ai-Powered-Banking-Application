# ArthVeda backend

## Start the AI assistant

The ArthVeda assistant uses Google Gemini from the backend. Set the key in the
same PowerShell window used to start Spring Boot. Restart the backend after
changing it. Do not put the key in the frontend or commit it to a file.

In PowerShell:

```powershell
$env:GEMINI_API_KEY = "your-Gemini-API-key"
.\mvnw.cmd spring-boot:run
```

The configured default model is `gemini-3.5-flash-lite`. To use a different
supported Gemini model, set `GEMINI_MODEL` in the same shell before starting
the backend.

The backend calls
`POST https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`
and authenticates with the `x-goog-api-key` header. The model is listed as
stable in [Google's model catalog](https://ai.google.dev/gemini-api/docs/models/gemini-3.5-flash-lite).
Requests contain `contents`, `systemInstruction`, and `generationConfig`; the
standalone connectivity check sends only a short `contents` prompt and a small
output limit.

### Optional real API connectivity test

The real Google API test is skipped unless `GEMINI_INTEGRATION_TEST=true` and
`GEMINI_API_KEY` are both set. It uses `GEMINI_MODEL` when set, otherwise the
configured default, makes a minimal real generation request, and reports only
the HTTP status/model or a sanitized provider error. Mocked tests do not need
credentials or external network access.

Run from Windows CMD with the key already set in that CMD window (do not put
the key in source control or print it):

```cmd
cd /d C:\Users\admin\Desktop\bank\Banking_Project-main
set "GEMINI_INTEGRATION_TEST=true"
mvnw.cmd -Dtest=GeminiApiIntegrationTest test
```

If needed, set `GEMINI_API_KEY` in that same CMD window before running Maven.
The real test does not retry invalid keys, invalid models/requests, or rate
limits. It makes at most three attempts with exponential backoff for transient
5xx and network failures.

The authenticated endpoint is `POST /api/ai/chat` with a JSON body:

```json
{
  "message": "How can I make a simple monthly budget?"
}
```

The backend logs a warning at startup if `GEMINI_API_KEY` is missing. The
customer dashboard shows the backend's actionable configuration or provider
error if an AI request fails. The backend sends only the submitted question and
its general-purpose assistant instructions to Gemini. It does not send account,
transaction, or balance data, and it does not forward earlier chat messages.
Avoid entering passwords, PINs, card details, one-time codes, or other sensitive
information. AI answers are general education, not personalized financial advice.

The customer dashboard also provides monthly spending analysis through the
authenticated `POST /api/ai/spending-insights` endpoint:

```json
{
  "accountNumber": "your-account-number"
}
```

The backend checks that the signed-in customer owns the account, then returns
the current month's debit/credit totals and transaction count alongside the
Gemini explanation. Gemini receives only those monthly aggregate values and
month labels. The account number is used only for the backend ownership check;
individual transactions and descriptions are not sent to Gemini.

If `GEMINI_API_KEY` is not configured, the endpoint returns a clear
service-unavailable response and the frontend shows that error. Invalid keys,
unavailable models and provider request limits are also reported with specific
next steps.

## Frontend API access

The Vite development server proxies `/api` requests to `http://localhost:8080`,
so no browser CORS configuration is needed when using that proxy. For a
separately hosted frontend, set `VITE_API_BASE_URL` to the backend origin in
the frontend environment and set `FRONTEND_ORIGIN` in the backend environment
to the frontend origin. The backend allows `http://localhost:5173` by default
and permits the authorization, JSON content-type, and idempotency headers used
by the application.

## Customer and admin dashboards

The customer transaction panel uses the existing ownership-checked account
history APIs. It supports server-side reference, transaction type/status, and
date-range filters, pagination, filtered credit/debit summaries, and CSV
statement downloads. CSV values are escaped to avoid spreadsheet formula
execution. Account numbers are masked in dashboard cards, with a user-triggered
copy action. Transfers and withdrawals require confirmation and are disabled
for non-active accounts.

The admin dashboard loads live counts and successful ledger volumes from
`GET /api/admin/dashboard`, which is restricted to `ADMIN`. Transfer volume is
reported separately and represents the credit-side transfer entries; none of
these ledger totals are presented as bank revenue. Recent transaction account
numbers are masked. Existing administrator account funding remains available.
There are no schema migrations or new database tables for these changes.

To start the backend from Windows CMD:

```cmd
cd /d C:\Users\admin\Desktop\bank\Banking_Project-main
mvnw.cmd spring-boot:run
```

In another CMD window, start the existing React app:

```cmd
cd /d C:\Users\admin\Desktop\bank\frontend
npm run dev
```

The project initializes its administrator from the existing
`smartbank.admin.*` Spring properties. Configure the admin email/password
through your local environment or untracked configuration before first startup;
do not use a demo password in a deployed environment.

Complaints, notifications, beneficiaries, account applications/closure
requests, account/customer administration, audit logs, rules-based suspicious
activity review, and downloadable admin reports are not present in the
existing schema or API and are not implemented by these dashboard changes.
