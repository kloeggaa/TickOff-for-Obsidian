# Google Play Data Safety: summary for TickOff for Obsidian

Answers for the Play Console "Data safety" form, matching what version 1.0.0 actually does.
See [PRIVACY.md](PRIVACY.md) for the user-facing policy.

## Overview

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes**: the voice-capture transcript leaves the device (see below). |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (HTTPS only). |
| Do you provide a way for users to request that their data is deleted? | **No**: the developer stores no user data. Data sent to OpenAI/Anthropic is held under the user's own API account with that provider. |
| Independent security review | No |

## Data types

| Data type (Play category) | Collected | Shared | Ephemeral | Required? | Purpose |
|---|---|---|---|---|---|
| App activity → **Other user-generated content** (text transcript of dictated tasks) | Yes | No ¹ | No ² | Optional (only when the user uses voice capture) | App functionality |

Not collected or shared: location, personal info (name, email, …), financial info, health,
messages, photos/videos, **audio** ³, files and docs ⁴, calendar, contacts, web browsing,
app info and performance (no crash logs or diagnostics), device or other IDs.

### Notes

1. **Not "shared":** the transcript goes to the AI provider the user picked and configured
   with their own API key, as a direct result of the user tapping "Stop and save". Play counts
   a transfer the user starts and reasonably expects as not "sharing".
2. **Not ephemeral:** the provider may keep API requests under its own retention policy, so
   we don't claim ephemeral processing.
3. **Audio:** the app does not record, store or transmit audio itself. Speech is recognized by
   the system's speech recognition service (requested on-device), which returns text to the
   app.
4. **Files:** the task files (the default one in the vault folder and any file picked for a
   widget) are read and written locally through the Storage Access Framework and never leave
   the device through the app.

## Other Play declarations

- **Ads:** No ads.
- **Target audience:** 18+ (or 13+). Not designed for children.
- **Permissions:** `RECORD_AUDIO` (voice capture), `INTERNET` (voice capture). No storage,
  location, contacts or "all files access" permissions.
- **Privacy policy URL:** the hosted PRIVACY.md, e.g.
  `https://github.com/kloeggaa/TickOff-for-Obsidian/blob/main/PRIVACY.md`
