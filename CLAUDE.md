# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Style Rules (apply to all output: code, comments, docs, commit messages)

- Be succinct. Say it once, say it short.
- No redundant comments. If the code is clear, don't comment it.
- No filler text, no restating the obvious, no "this function does X" before a function named X.
- When asked to "eliminate repetition" or "remove redundant comments", take it literally.
- No fluff, no fuzzy

## Project Overview

fsrs4s is a Scala 3 implementation of the FSRS (Free Spaced Repetition Scheduler) algorithm, used for spaced repetition learning systems like Anki.

## Build Commands

- **Compile**: `sbt compile`
- **Test**: `sbt test`
- **Single test**: `sbt "testOnly *SchedulerSuite"`
- **REPL**: `sbt console`

## Architecture

All code lives in `org.bargsten.fsrs`:

- `Scheduler` - Main scheduling logic implementing FSRS-6 algorithm with 21 weight parameters
- `Card` - Flashcard with state (Learning/Review/Relearning), stability, difficulty, due date
- `Parameters` - Algorithm config: weights, desired retention (default 0.9), learning/relearning steps
- `Rating` - User response: Again, Hard, Good, Easy
- `Review` - Captures rating + timestamp for a card review
- `CardId` - Opaque UUID wrapper
- `Days` - Opaque Int for day intervals (in `DateTimeUtil.scala`)

## FSRS Algorithm References

When modifying `Scheduler.scala`, verify against the reference implementation and algorithm docs:
- [FSRS Algorithm Wiki](https://github.com/open-spaced-repetition/fsrs4anki/wiki/The-Algorithm)
- [Technical explanation of FSRS](https://expertium.github.io/Algorithm.html)
- [Anki FSRS Deck Options](https://docs.ankiweb.net/deck-options.html#fsrs)

## Before Writing Code

- Check if a rough design or architecture decision is needed first. Ask if unclear.
- Design around data structures. Get the data model right before implementing logic around it.
- Develop the critical path first — the hard, fundamental part stripped to essentials.
- Don't introduce abstractions preemptively. Duplication is cheaper than the wrong abstraction. Let patterns emerge.
- Think about module and package structure before creating new packages.
- Don't create fine-grained packages with one class each ("categoritis"). Organise by feature, not by category.
- Don't introduce DTOs if not needed. E.g. if kafka models are generated from an avro spec, you can map directly to domain models without any DTO.

## Writing Code

- One level of abstraction per function. Don't mix high-level orchestration with low-level details.
- Functions should fit on a screen (~80–100 lines max).
- Group code by functional cohesion (things that contribute to the same task), not by class-per-responsibility.
- Keep dependencies minimal. Don't add libraries for trivial tasks.
- No tactical DDD patterns or hexagonal architecture unless explicitly requested.
- If you don't know a library, read its docs or source on GitHub. Don't guess the API.

## Testing

- Write integration and e2e tests early. They catch what AI misses — AI reasons locally, tests verify globally.
- For UI: write Selenium e2e tests first. Use them to verify and self-correct.
- One test per desired external behavior, plus one test per bug.
- Tests target the API of a cohesive unit — not individual classes or internal methods.
- Use tests to find edge cases.
- Don't write tests before the implementation exists (no TDD).

## APIs and Interfaces

- Treat APIs as permanent. Don't change signatures without explicit approval.
- Be strict in what you accept and what you return. Don't silently tolerate malformed input.
- Minimize observable behavior surface — anything observable will be depended on.

## Conventions and Consistency

- Follow existing patterns in the codebase. When in doubt, match what's already there.
- Global project structure matters. Local style within a function or module is flexible.
- If a convention exists (naming, structure, patterns), follow it. Don't introduce alternatives.

## AI Workflow

- Don't over-engineer prompts or plans. Work with what's given plainly.
- After producing code, expect it to be reviewed and challenged. ~50% of output will need major changes.
- Never commit secrets, credentials, or API keys.
- When fixing bugs: reproduce with a test first, then fix.
- If a task is ambiguous, ask one clarifying question rather than guessing.
