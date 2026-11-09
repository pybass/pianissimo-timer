# Saved data

The rules the format must keep are in [spec.md](spec.md), section 6. This file shows the shape: one example of the
whole file and the meaning of every field. The frozen JSON in `app/src/test/kotlin/.../data/` is the contract the
tests check; this example must stay equal to the frozen JSON of the current version.

## One file

Everything the app saves is one JSON document, written atomically by DataStore (a typed store with a JSON
serializer, not Preferences). The document carries its own `version`. On every launch the app reads the file and
runs the migration steps from the saved version up to the current one. Then it reads the two parts back: the library
(`selectedTimerId`, `timers`, `sounds`) and the `state`. A library that does not read gives way to the default one.
One that reads is repaired, so a bad value never costs more than itself: a value outside the range in its table
below gives way to the default of its field, a repeated id to a new one, an empty list to its default items. A state
that does not read, holds a value outside its range, or was saved by another version gives way to idle, and its bells
are cancelled. A file with no `version`, or one the app does not know, or one a migration step fails on, gives way
to the defaults as a whole.

Rules of the document:

- Items are referenced by `id`, never by name. An id never changes and is unique within its list: the default timer
  and sounds have fixed words (`timer`, `piano`), everything the user makes gets a random UUID. Names are for the
  user: they can be edited freely and must be unique within their list, ignoring case.
- Instants end in `At` and are milliseconds since the epoch. Durations carry their unit in the name.
- Enum values are lowercase words, not Kotlin constant names, so a rename in Kotlin does not change the file.
- Every field is always written, defaults included, so a changed default never changes saved data.
- Unknown fields are ignored on read, so a removed field does not break old files.

## Example

```json
{
  "version": 1,
  "selectedTimerId": "7a1c2e40-9d7b-4a3f-8e0c-2b6f1d9a5c11",
  "timers": [
    {
      "id": "7a1c2e40-9d7b-4a3f-8e0c-2b6f1d9a5c11",
      "name": "Pomodoro",
      "steps": [
        { "seconds": 1500, "soundId": "piano", "volumeDb": 0 },
        { "seconds": 300, "soundId": "bell", "volumeDb": -6 }
      ]
    },
    {
      "id": "timer",
      "name": "Timer",
      "steps": [
        { "seconds": 1500, "soundId": "piano", "volumeDb": 0 }
      ]
    }
  ],
  "sounds": [
    { "id": "piano", "name": "Piano", "type": "piano", "note": 60, "fadeInSeconds": 0.0, "lengthSeconds": 5.0 },
    { "id": "bell", "name": "Bell", "type": "bell", "note": 72, "fadeInSeconds": 0.0, "lengthSeconds": 8.0 },
    { "id": "flute", "name": "Flute", "type": "flute", "note": 64, "fadeInSeconds": 1.5, "lengthSeconds": 4.0 },
    { "id": "string", "name": "String", "type": "string", "note": 55, "fadeInSeconds": 0.0, "lengthSeconds": 6.0 }
  ],
  "state": {
    "type": "paused",
    "startedAt": 1762600000000,
    "pausedAt": 1762600600000,
    "bells": [
      {
        "dueAt": 1762601500000,
        "sound": { "id": "piano", "name": "Piano", "type": "piano", "note": 60, "fadeInSeconds": 0.0, "lengthSeconds": 5.0 },
        "volumeDb": 0
      },
      {
        "dueAt": 1762601800000,
        "sound": { "id": "bell", "name": "Bell", "type": "bell", "note": 72, "fadeInSeconds": 0.0, "lengthSeconds": 8.0 },
        "volumeDb": -6
      }
    ]
  }
}
```

The other two states:

```json
{ "type": "idle" }
```

```json
{ "type": "running", "startedAt": 1762600000000, "bells": [ ... ] }
```

## Fields

### Document

| Field | Meaning |
|---|---|
| `version` | Format version, an integer from 1. Bumped with every change of the shape, see "Changing the format". |
| `selectedTimerId` | The id of the chosen timer. Must be the id of a timer in `timers`; otherwise the first timer. |
| `timers` | The timer library. At least one timer; empty gives the default timer. |
| `sounds` | The sound library. At least one sound; empty gives the four default sounds. |
| `state` | The live timer: idle, running or paused. Dropped to idle on every version change. |

### Timer

| Field | Meaning |
|---|---|
| `id` | Never changes: `timer` for the default timer, a random UUID for the others. A repeated id is replaced on read. |
| `name` | Shown to the user. Unique among timers, ignoring case; the editor checks, the reader does not. |
| `steps` | At least one step; empty gives the step of a new timer. They ring one after the other without a gap. |

### Step

| Field | Meaning |
|---|---|
| `seconds` | Length of the step. 1 to 35999 (9:59:59); outside, 1500. |
| `soundId` | The id of a sound in `sounds`. Not a copy: an edit of the sound reaches every step. A missing id plays the first sound. |
| `volumeDb` | Loudness of the bell relative to the sound, in dB: the app can only make a sound quieter. -45 to 0; outside, 0. |

### Sound

| Field | Meaning |
|---|---|
| `id` | Never changes: the type's word for the four default sounds, a random UUID for the others. A repeated id is replaced on read. |
| `name` | Shown to the user. Unique among sounds, ignoring case; the editor checks, the reader does not. |
| `type` | The synthesizer recipe: `piano`, `bell`, `flute` or `string`. An unknown word reads as `piano`. |
| `note` | MIDI note number; 60 is C4, 262 Hz. 36 to 84 (C2 to C6); outside, 60. |
| `fadeInSeconds` | Seconds the loudness grows before the sound rings; 0 leaves only the soft start of the type. 0 to 10; outside, 0. |
| `lengthSeconds` | Seconds the sound rings, from full loudness to silence (60 dB down). It starts after the fade-in. 1 to 12; outside, 5. |

### State

| Field | Meaning |
|---|---|
| `type` | `idle`, `running` or `paused`. `idle` has no other fields. |
| `startedAt` | When the first step began. The progress of a step is measured from it or from the bell before. |
| `pausedAt` | Paused only. When Pause was pressed; the bells keep their times and the time left is measured from it. |
| `bells` | One per step, in order, as handed to Android at Start. At least one; a bell with a value outside its range drops the whole state to idle, as the bells already stand in Android. |

### Bell

| Field | Meaning |
|---|---|
| `dueAt` | When the bell rings. |
| `sound` | A copy of the step's sound as it was at Start, so a later edit or deletion in the library does not change the bell (spec 1.10). Same ranges as a sound. |
| `volumeDb` | The step's volume at Start. Same range as a step's. |

## Changing the format

Every change of the shape above takes four things in one commit: a renamed or removed field, a changed meaning or
range, and also a field added with a default, although old data would still read. The version then names the exact
shape, and no one has to judge whether old data still reads; the step for an added field writes it into every item,
so every field stays written.

1. Bump `version`.
2. Add the migration step from the version before to `MIGRATIONS` in `Migrations.kt`. A step works on the JSON tree
   only, never on the Kotlin classes: the classes describe the current version and will move on, the step must keep
   working. It does not set `version` and does not touch `state`.
3. Freeze the new JSON in the tests and keep the old one as the input of the step's test.
4. Update the example and the tables here.
