# MissedCall Auto-Responder

**Project**: MissedCall Auto-Responder — Android app (Java) + PHP backend

> Professional, product-ready app that automatically detects missed calls and (after a configurable delay) sends a single automated message to the caller using the Fast2SMS API. Designed for business users who receive many calls and want to follow up automatically without spamming.

---

## Table of Contents

1. [Overview](#overview)
2. [Key Features](#key-features)
3. [User Stories & Use Cases](#user-stories--use-cases)
4. [Architecture & Components](#architecture--components)
5. [Technology Stack](#technology-stack)
6. [Android App Specifications](#android-app-specifications)
   - Permissions
   - App Components
   - Data model (local)
   - Deduplication logic
   - Background reliability
7. [PHP Backend Specifications](#php-backend-specifications)
   - API endpoints
   - Database schema
   - Security considerations
8. [Fast2SMS Integration (SMS) & WhatsApp Options](#fast2sms-integration-sms--whatsapp-options)
9. [Message Templates and Localization](#message-templates-and-localization)
10. [Privacy, Consent & Compliance](#privacy-consent--compliance)
11. [Testing Strategy](#testing-strategy)
12. [Deployment & CI/CD](#deployment--cicd)
13. [Monitoring & Analytics](#monitoring--analytics)
14. [Cost Estimate & Limits](#cost-estimate--limits)
15. [Roadmap & Milestones](#roadmap--milestones)
16. [Release Checklist](#release-checklist)
17. [Contribution Guidelines & Code Style](#contribution-guidelines--code-style)
18. [FAQ](#faq)
19. [Appendix: Example .env (server)](#appendix-example-env-server)
20. [CHANGELOG.md](#-changelogmd)
21. [LICENSE](#%EF%B8%8F-license)
22. [.gitignore](#-gitignore)
23. [Contribution Guide](#contribution-guide)
24. [License & Contact](#license--contact)
25. [Author](#author)

---

## Overview

This product automatically follows up with missed callers to reduce lost business opportunities. When the Android app detects a missed call it will wait **5 minutes** (configurable) and then send **one** message via Fast2SMS (or optionally via a WhatsApp Business/3rd-party provider). The app is designed to be reliable (foreground service + WorkManager), energy-efficient, and respectful of user privacy and regulations.

Goals:
- Immediate, non-intrusive follow-up for missed callers
- Exactly **one** message per missed call
- Configurable templates & delays
- Centralized logs via PHP backend for auditing and analytics
- Production-grade error handling and retries

---

## Key Features

- Missed-call detection (background)
- 5-minute delayed single-message sending
- Local deduplication: avoid multiple messages for the same missed call
- Optional server-side deduplication & logging
- Fast2SMS outbound SMS integration
- Optional WhatsApp integration (Twilio / Ultramsg / WATI) as an alternate channel
- Admin UI (web or mobile) for templates, logs, and status (future)
- Configurable business hours, do-not-disturb (DND) and opt-out handling

---

## User Stories & Use Cases

- **Business owner**: Won’t miss potential customers — they receive automated polite follow-ups.
- **Support team**: Get a log of missed callers and automated replies, reducing manual callbacks.
- **Customers**: Receive a courteous message asking if they need support.

Example user story:
> As a store owner, when I miss a customer call, I want an automatic message sent 5 minutes after the missed call so the customer feels acknowledged and I don’t lose business.

---

## Architecture & Components

High-level components:

1. **Android App (Java)**
   - BroadcastReceiver / PhoneStateListener
   - Foreground Service (phone-state monitor)
   - WorkManager (schedule delayed send after 5 minutes)
   - Local DB (SQLite) or Room for dedupe & audit
   - HTTP client for server communication (Retrofit/OkHttp)

2. **PHP Backend (optional but recommended)**
   - REST API (send request to Fast2SMS server-side)
   - MySQL database for logs & deduplication
   - Auth for admin endpoints (JWT or basic API key for mobile)

3. **Third-party services**
   - Fast2SMS for SMS delivery
   - Optional: Twilio/Ultramsg/WATI for WhatsApp messages

ASCII flow diagram:

```
Incoming Call --> PhoneStateListener --> Missed Call Detected --> Save to local DB
                                                      |
                                                      V
                                               Schedule WorkManager (5 min)
                                                      |
                                                      V
                                            Check dedupe + business hours
                                           /                           \
                                          V                             V
                               If not sent -> Send via PHP Backend -> Fast2SMS
                               If already sent -> skip
```

---

## Technology Stack

- **Android**: Java, Android Studio, WorkManager, Room (or SQLite), Retrofit/OkHttp
- **Backend**: PHP (7.4+ or 8.x), Composer, Lumen or Slim (optional microframework) or plain PHP + PDO
- **Database**: MySQL / MariaDB (backend), SQLite (local on device)
- **Deployment**: VPS / shared host supporting PHP & HTTPS (Certbot for SSL)

---

## Android App Specifications

### Required Permissions

For Android 13+ and Android 14, ask and explain at runtime.

- `READ_CALL_LOG` — to detect call details (note: Google Play may be restrictive)
- `READ_PHONE_STATE` — to read phone state
- `FOREGROUND_SERVICE` — to run service reliably in background
- `INTERNET` — to talk to backend & Fast2SMS
- `POST_NOTIFICATIONS` — to show notifications (Android 13+)

**Important:** Access to `READ_CALL_LOG` and call-log data is sensitive. If you plan to publish on Play Store, consider using `ROLE_CALL_SCREENING` / `ROLE_CALL_REDIRECTION` or using the `PhoneStateListener` and rely on call states. You must provide strong justification for call log permission in Play Console. Another safer approach is to detect missed calls via `TelephonyManager` broadcast and avoid persistent call-log reads.


### Main Components

1. **CallReceiver (BroadcastReceiver)**
   - Listens to telephony state changes.
   - Records incoming number & timestamp when ringing.
   - When state becomes `IDLE`, determine if call was missed (not answered and not an outgoing call).

2. **MissedCallService (Foreground Service)**
   - Keeps the critical monitoring alive when the app is backgrounded.
   - Shows a low-priority persistent notification.

3. **MessageScheduler (WorkManager)**
   - Schedules a one-off WorkRequest that executes after configured delay (5 minutes default).
   - Work checks dedupe & calls the backend or uses local HTTP client to hit Fast2SMS (only if you decide to send from device).

4. **Local DB (Room recommended)**
   - `missed_calls` table: stores `id, phone_number, call_time, scheduled_time, status, attempt_count, message_id, sent_at`.

5. **Deduplication**
   - Check if a message record for the same `phone_number` and `call_time` exists.
   - Use unique constraints or check `sent_at IS NULL` before sending.
   - Use a TTL (e.g., only consider calls within last X hours for dedupe) to avoid blocking legitimate separate calls.

6. **Network Layer**
   - Retrofit/OkHttp to call PHP backend endpoints `/api/send_sms` or `/api/log_missed_call`.

7. **Configuration & UI**
   - Settings screen: enable/disable auto reply, edit message template, set delay, DND times, business hours, opt-out keywords.


### Example Local DB Schema (Room / SQLite)

```sql
CREATE TABLE missed_calls (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  phone_number TEXT NOT NULL,
  call_time INTEGER NOT NULL,
  scheduled_time INTEGER,
  status TEXT DEFAULT 'PENDING', -- PENDING, SENT, SKIPPED, FAILED
  attempt_count INTEGER DEFAULT 0,
  message TEXT,
  message_provider_id TEXT, -- provider message id
  sent_at INTEGER
);

CREATE UNIQUE INDEX idx_missed_unique ON missed_calls(phone_number, call_time);
```


### Deduplication Strategy (prevent multiple messages)

- Use unique index (`phone_number`, `call_time`) so duplicate scheduling attempts are blocked.
- When scheduling a WorkManager job, save the missed_call row first; if insertion fails due to unique constraint, skip scheduling.
- Mark `status = SENT` after success. If Work fails, increment `attempt_count` and use exponential backoff.


### Background Reliability & Battery

- Use **Foreground Service** to monitor call events persistently.
- Use **WorkManager** (OneTimeWorkRequest with `setInitialDelay`) for delayed execution — it survives reboots (with `setRequiresDeviceIdle(false)` and `setBackoffCriteria`).
- Prompt users to whitelist the app from battery optimizations and explain why.

**Note:** Play Store policies and OEM restrictions vary — thorough testing required on Xiaomi/Realme/Oppo devices.

---

## PHP Backend Specifications

> Recommendation: Keep Fast2SMS API key on the server (never hardcode in the mobile app). The mobile app calls your PHP server which will validate and forward the SMS request to Fast2SMS. This prevents exposing your API key and allows central logging and rate-limiting.

### Endpoints (example)

- `POST /api/v1/missed_calls` — Log a missed call and request to send message (body: phone_number, call_time, device_id, template_id)
- `POST /api/v1/send_sms` — Internal endpoint to trigger SMS (only trusted requests, e.g., from server cron or the same server)
- `GET  /api/v1/logs` — (Admin) Retrieve logs (paginated)
- `POST /api/v1/register_device` — (Optional) Register device & issue device API token


### Example `missed_calls` table (MySQL)

```sql
CREATE TABLE missed_calls (
  id INT AUTO_INCREMENT PRIMARY KEY,
  device_id VARCHAR(128),
  phone_number VARCHAR(32) NOT NULL,
  call_time DATETIME NOT NULL,
  status ENUM('PENDING','SENT','FAILED','SKIPPED') DEFAULT 'PENDING',
  provider_msg_id VARCHAR(128),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  sent_at TIMESTAMP NULL
);
```


### Security & Auth

- Use HTTPS (Let's Encrypt).
- Authenticate mobile app using an API key or short-lived JWT token. Bind tokens to device IDs.
- Rate-limit requests per device and per phone_number.
- Store Fast2SMS API key in server environment variables (e.g., `.env`), not in code.


### PHP Example (Fast2SMS) — cURL PHP skeleton

```php
// Example: server-side Fast2SMS request (never from app)
$apiKey = getenv('FAST2SMS_API_KEY');
$numbers = '91' . $phone_number; // format expected by provider
$message = urlencode($message_text);
$postData = "sender_id=TXTIND&message={$message}&route=q&numbers={$numbers}";

$ch = curl_init('https://www.fast2sms.com/dev/bulkV2');
curl_setopt($ch, CURLOPT_POST, true);
curl_setopt($ch, CURLOPT_POSTFIELDS, $postData);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'authorization: ' . $apiKey,
    'Content-Type: application/x-www-form-urlencoded'
]);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
$result = curl_exec($ch);
$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

// Parse $result and update DB accordingly
```


### Retry, Idempotency & Logging

- Backend should be idempotent — use `device_id + call_time + phone_number` as dedupe key.
- Store provider response and message id for auditing.
- Implement retry logic with exponential backoff for transient errors.

---

## Fast2SMS Integration (SMS) & WhatsApp Options

### Fast2SMS (SMS)
- Preferred: send from server (PHP) to avoid exposing API key.
- Use provider's documented endpoints. Implement response parsing and log provider `message_id`.

cURL example shown above.

### WhatsApp (optional)
- Official WhatsApp Business Cloud API requires Facebook App & approval — heavier onboarding.
- Easier alternatives: Twilio API for WhatsApp, Ultramsg, WATI etc. They provide HTTP APIs to send templates or session messages.
- WhatsApp messaging has template approval and opt-in requirements; more complex than SMS.

Recommendation: Start with **Fast2SMS (SMS)** MVP, then add WhatsApp as an enterprise feature.

---

## Message Templates & Localization

- Allow templating variables: `{{business_name}}`, `{{agent_name}}`, `{{call_time}}`.
- Example default template:

> "Hello! We missed your call at {{call_time}}. We're sorry we couldn't pick up. Reply *CALLBACK* or visit {{business_link}} and we'll get back to you shortly."

- Offer multi-language support in the app and server.

---

## Privacy, Consent & Compliance

- Provide a clear privacy policy in the app.
- Allow caller to opt-out of automated messages (e.g., reply STOP) and honor it.
- Log opt-out requests and add phone numbers to a blocklist.
- If publishing to Play Store, include the required permission rationale and data handling documentation.

---

## Testing Strategy

1. **Unit Tests** (Android Java, local business logic)
2. **Integration Tests** (WorkManager flows, network calls mocked)
3. **End-to-End** (device-level: incoming call simulation, missed call detection, scheduled send)
4. **Backend Tests** (API endpoint, DB integrity, rate-limits)
5. **Manual handset testing** across vendors (Xiaomi, Samsung, OnePlus, Oppo) for background reliability

---

## Deployment & CI/CD

- Backend: GitHub Actions / GitLab CI to deploy to staging and production (upload to VPS via SSH or use Docker)
- Android: GitHub Actions to build signed APK / AAB. Automate Play Store uploads with `fastlane` (supply screenshots and privacy policy).

---

## Monitoring & Analytics

- Logging: use centralized logs (Papertrail/LogDNA) or self-hosted ELK.
- Alerts for delivery failures (e.g., >5% failures in last hour)
- Track KPIs: messages sent/day, msgs per device, success rate, opt-outs

---

## Cost Estimate & Limits

- Cost depends on Fast2SMS pricing. Example (placeholder): ₹0.08–₹0.50 per SMS depending on volume and route.
- Add monthly budget and throttling to avoid invoice surprises.

---

## Roadmap & Milestones

**MVP (v0.1)**
- Missed call detection on device
- 5-min delayed SMS via PHP backend (Fast2SMS)
- Local dedupe & logging to server
- Basic settings & message template

**v1.0**
- Admin web UI for logs & templates
- Battery-optimization guides & flows
- Multi-language templates
- Retry & better error handling

**v2.0**
- WhatsApp integration
- Team multi-device support
- Advanced scheduling & CRM integrations

---

## Release Checklist

- [ ] App permissions and privacy policy content ready
- [ ] Backend `.env` configured and secrets stored securely
- [ ] Test coverage for major flows
- [ ] CI pipeline configured for builds
- [ ] Play Store listing assets (screenshots, description)
- [ ] Beta testing with diverse devices (OEMs)

---

## Contribution Guidelines & Code Style

- Java: use consistent naming, Android Jetpack components where suitable. Use `Room` for DB.
- PHP: PSR-12 style, use prepared statements / PDO, avoid raw concatenated queries.
- Use Pull Requests with code review and a single-person merge rule.

---

## FAQ

**Q: Will the app spam callers if they call multiple times?**
A: No — dedup logic groups calls by timestamp and only sends one message per missed call. You can optionally configure a cooldown window (e.g., 2 hours) to avoid multiple messages to the same number in a short window.

**Q: Can the app send WhatsApp messages?**
A: Not natively. WhatsApp requires business API access (approval) or third-party provider. We recommend starting with SMS.

**Q: What about Play Store restrictions?**
A: Play Store restricts some sensitive permissions; you must provide justification and a privacy policy. Consider a lighter-permission approach: depend on `PhoneStateListener` and minimize `READ_CALL_LOG` usage.

---


## Appendix: Example `.env` (server)

```
APP_ENV=production
APP_DEBUG=false
DB_HOST=127.0.0.1
DB_NAME=missed_call
DB_USER=missed_user
DB_PASS=secret
FAST2SMS_API_KEY=YOUR_FAST2SMS_KEY_HERE
JWT_SECRET=some_long_random_secret
```


---

## 📜 CHANGELOG.md

### v0.1.0 — Initial Planning & Documentation (2025-10-05)
- Created initial `README.md` with full architecture and roadmap.
- Defined Android app and PHP backend specifications.
- Added permissions list and background handling strategy.
- Planned Fast2SMS integration (server-side).
- Added future roadmap for WhatsApp support.

### v0.2.0 — Upcoming
- Add Android skeleton project (Receiver, Service, WorkManager, Room DB).
- Add PHP backend skeleton (REST API, DB schema, Fast2SMS connector).
- Include configuration templates and initial deployment guide.

---


## ⚖️ LICENSE

MIT License

```
Copyright (c) 2025 Demoody Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 🧹 .gitignore

```
# Android Studio / IntelliJ
.idea/
.gradle/
/local.properties
.DS_Store
/build/
/captures/
*.iml

# APK / Build Outputs
*.apk
*.aab
app/release/

# Logs
*.log

# Node / PHP Vendor
node_modules/
vendor/
composer.lock
package-lock.json

# Environment files
.env
.env.*

# Backup & temp
*.bak
*.tmp
*.swp
*.swo

# OS generated
Thumbs.db
.DS_Store
```

---

# Contribution Guide
- Fork repository
- Create branch: `feature/xxx` or `bugfix/yyy`
- Follow code style (Java: Android style guide)
- Create PR with description and linked issue
- CI must pass before merge

---

# License & Contact
- MIT License — you may use and modify the code for your organization. Include attribution if you redistribute.
- For commercial / closed-source product consider proprietary license.

**Contact**: Project owner / maintainer - wasim@demoody.com

---
## Author
**Develope By** - [Sk Wasim Akram](https://github.com/skwasimakram13)

- 👨‍💻 All of my projects are available at [https://skwasimakram.com](https://skwasimakram.com)

- 📝 I regularly write articles on [https://blog.skwasimakram.com](https://blog.skwasimakram.com)

- 📫 How to reach me **hello@skwasimakram.com**

- 🧑‍💻 Google Developer Profile [https://g.dev/skwasimakram](https://g.dev/skwasimakram)

- 📲 LinkedIn [https://www.linkedin.com/in/sk-wasim-akram](https://www.linkedin.com/in/sk-wasim-akram)

---

💡 *Built with ❤️ and creativity by Wassu.*
