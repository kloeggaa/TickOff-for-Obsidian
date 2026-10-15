# Privacy Policy: TickOff for Obsidian

*Last updated: 2026-10-12*

> Unofficial app, not affiliated with or endorsed by Obsidian.

TickOff for Obsidian ("TickOff") is a free, open-source Android app made by an individual
developer. This page explains, in plain language, what the app does with your data.

## The short version

- No account, no ads, no analytics, no crash reporting, no tracking.
- The developer receives **no data** from the app. There is no TickOff server.
- Your tasks stay in your own vault on your phone.
- Only if you use **voice capture** is the transcribed text of your dictation sent to the AI
  provider you chose (OpenAI or Anthropic), using your own API key.

## What the app accesses

**Your task files.** You choose one folder (your vault) with Android's folder picker. TickOff
can then read and write files in that folder only. It reads the default task file there (by
default `TasksAI.md`), writes a check mark and a done date when you tick a task, and adds new
tasks you record. When you add a task list widget, you can instead pick a single file for it
with Android's file picker. TickOff then gets access to that one file only, and gives the
access back when you remove the last widget showing it. TickOff does not read any other notes.
You can revoke access at any time by uninstalling the app.

**Microphone (voice capture only).** When you open the recorder, the app asks for microphone
access. While you record, TickOff keeps the microphone open and passes the audio directly to
your phone's speech recognition service (for example Google's speech services or another engine
installed on your device), which turns it into text. TickOff asks that service to recognize
speech on the device where possible. The audio is held in memory only for that hand-over; TickOff
never stores it or sends it anywhere else.

**Settings.** Your vault folder, file name, AI provider, model name and API key are stored
in the app's private storage on your phone. They are excluded from Android backups and
device transfers. The API key is encrypted with a key from the Android Keystore that never
leaves your phone, and once saved it is not shown again in the app.

## What leaves your phone

Only when you record tasks:

- **What:** the text transcript of what you dictated, plus a fixed instruction and today's
  date, so the AI can turn it into task lines.
- **To whom:** OpenAI (api.openai.com) or Anthropic (api.anthropic.com), whichever you
  selected, authenticated with your own API key. The connection is encrypted (HTTPS).
- **Why:** to turn your dictation into tasks. Nothing else is sent, and the request goes
  directly from your phone to the provider.

When you tap **Test API key** or **Load models** in Settings, the app sends one request to the
provider you selected: your API key (and, for the test, the model name), to check the key or to
list the models it can use. No tasks or other text are sent, and no cost arises.

These requests are governed by the provider's terms and privacy policy for your API account:

- OpenAI: https://openai.com/policies/privacy-policy
- Anthropic: https://www.anthropic.com/legal/privacy

If you never use voice capture, the key test or the model list, the app makes no network connections at all.

## Opening Obsidian

If you turn it on for a widget, tapping a task opens the task file in the Obsidian app via
an `obsidian://` link. TickOff checks whether Obsidian is installed; it does not communicate
with Obsidian in any other way.

## Children

The app is not directed at children and collects no personal data from anyone.

## Changes

Changes to this policy are published in this file in the app's source repository, with a
new "Last updated" date.

## Contact

kloeggaa, abhaken.monthly406@passmail.com
