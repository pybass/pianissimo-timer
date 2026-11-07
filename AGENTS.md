# AI Agent Start Guide

## Critical: Answers
Be brief. Use simple English: common words, short sentences.
Give the result, not the reasoning behind it. Say only what the user needs to act
or to avoid a mistake — no preamble, no recap, no filler. Go into detail only when asked.

All code, comments, commit messages, and files must ALWAYS be in English — no exceptions.

## Prototype mode
The project is a prototype. Do not write tests unless the user asks for them in the task.

## Android 16+ only
The app requires Android 16 (API 36) or newer, set by `minSdk` in `app/build.gradle.kts`. Use current platform
APIs directly. Never add version checks, fallbacks for older Android, or AndroidX `*Compat` wrappers that
exist only to support older versions. The one exception is the Live Update check in `BellNotification.kt`:
that API came in Android 16.1, and the app still runs on 16.0 without the chip.

## Editor: Zed with kotlin-lsp
The user edits in Zed with JetBrains kotlin-lsp, not Android Studio. The LSP flags Compose functions
(`SetupScreen`, `Keypad`) with the `FunctionName` inspection and ignores the ktlint key in `.editorconfig` that
allows them, so every file with Compose UI functions starts with `@file:Suppress("FunctionName")`. Keep it.

## Mandatory Rules (external)
Read these files fully and follow them.
- `~/.agents/rules/general.md`
- `~/.agents/rules/code.md`

## Kotlin: newest stack, no rules file yet
There is no `kotlin.md` rules file yet: the user is still learning Kotlin. Until it exists, `general.md` and
`code.md` are the only rules. The app supports only the newest Android, so always use the
newest stable Kotlin, Gradle, Android Gradle Plugin and libraries, and current Android practice. Never pick an older
tool, API or approach for compatibility.

## Critical: Teach, do not just code
The user is new to Kotlin and Android development and wants to understand every step.
- Before each step, say what it does and why it is needed. After it, say how to check that it worked.
- Explain every new tool, library, config key, Kotlin feature and Android concept the first time it appears,
  in plain words.
- Never add code or config the user has not seen explained. No "I also added..." without a reason the user can follow.
- Prefer the simplest thing that works over the proper thing. Name the trade-off when it matters.

## Project Reading (context)
- [docs/spec.md](docs/spec.md) — behavioral spec (normative): the non-obvious rules the app must keep.
  Read it before the first task. Keep it short; its header says what belongs there.
- [docs/rejected.md](docs/rejected.md) — ideas the user decided against, each with its reason. Do not propose one
  again unless its reason no longer holds.
