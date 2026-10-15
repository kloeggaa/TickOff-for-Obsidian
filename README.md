# TickOff for Obsidian

> Unofficial app, not affiliated with or endorsed by Obsidian.

TickOff is a small Android app with two home-screen widgets for a Markdown task file in your
Obsidian vault:

- **Task list widget.** Shows the open tasks (`- [ ] …`) of a Markdown file as a checklist.
  When you add the widget, you choose its file: the default task file, or any file you pick,
  so several widgets can show different lists (projects, shopping, …).
  Tapping the box writes `- [x]` and the done date (`✅ 2026-10-09`, the Obsidian Tasks
  convention) into the file. Tasks with a `📅 yyyy-mm-dd` due date are sorted as overdue 🔥,
  today 🌟, then upcoming. Tasks due beyond a per-widget window are hidden.
  Optionally, tapping a task opens the file in Obsidian. You choose how when you add the
  widget: Obsidian's built-in `obsidian://open` link, or the Advanced URI community plugin.
- **Voice capture widget.** A 1×1 mic button. Dictate your tasks; the phone's speech service
  turns them into text. That text is sent to OpenAI or Claude with **your own API key**,
  turned into task lines (relative dates such as "next Friday" are resolved), and added to
  the task file. Tasks are inserted after any ```` ```tasks ```` query blocks at the top of
  the file.

The app itself is small: settings (vault folder, task file, AI key), a live preview of the
task list, and an about/privacy page.

## How it accesses your vault

TickOff uses Android's Storage Access Framework only. You pick your vault's root folder once
(`ACTION_OPEN_DOCUMENT_TREE`) and TickOff keeps that grant. In Settings you choose the folder
inside the vault and the file name of the default task file (default `TasksAI.md`). A file you
pick for a single widget (`ACTION_OPEN_DOCUMENT`) gets its own grant, which is released when the
last widget using it is removed. TickOff requests no storage permission, and it uses no file
paths or "all files access".

## Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice capture, only while the recorder screen is open |
| `INTERNET` | Voice capture: sending the transcript to the AI provider you chose; the optional "Test API key" and "Load models" buttons in Settings |

The task list widget works fully offline and needs neither permission.

## Privacy

There are no accounts, analytics, ads or trackers. See [PRIVACY.md](PRIVACY.md).

## Building

Requirements: JDK 17 and the Android SDK (platform 36).

```sh
./gradlew :app:assembleFdroidDebug         # debug APK
./gradlew :app:testFdroidDebugUnitTest     # unit tests
./gradlew :app:lintFdroidDebug :app:lintPlayDebug
./gradlew :app:bundlePlayRelease           # release AAB for Google Play
./gradlew :app:assembleFdroidRelease       # release APK for sideloading (F-Droid variant)
```

There are two build variants with identical code: `fdroid` (shows a donation link on the About
screen) and `play` (no donation link, as Google Play's payments policy requires).

Release signing is read from a git-ignored `keystore.properties` in the project root (copy
`keystore.properties.example`). Without it, release builds are unsigned.

## Support

TickOff is free and has no ads. If it saves you time, you can support its development:
[buy me a coffee](https://buy.stripe.com/3cIaEY7zfgUGdW78ZHeUU01). Thank you!

## License

Copyright (C) 2026 kloeggaa

This program is free software: you can redistribute it and/or modify it under the terms of
the GNU General Public License as published by the Free Software Foundation, either version 3
of the License, or (at your option) any later version. See [LICENSE](LICENSE).

"Obsidian" is a trademark of its respective owner. This project is not affiliated with it.
