# Rejected

Ideas we decided against, each with its reason. Do not propose one again unless its reason no longer holds.

- **Restore the bells after a phone restart**, with a receiver for the boot that schedules them again. Rejected: a
  restart is almost always a deliberate act, or a system update the user agreed to, and a timer of some minutes is
  almost never running at that moment.
- **A digit keypad for the step time, filled from the right as in the Android Clock timer** (`2500` is 25:00).
  Rejected: the rule must be known, `25` gives 25 seconds; changing a time means retyping it; Samsung and iPhone
  users know wheels.
- **A nested `synth` object in a saved sound, one class per type**, so a type can have its own parameters.
  Rejected while every type has the same three settings: the object would only add depth, and a setting for one type
  would also mean a different editor per type. Moving the fields under `synth` later is one small migration step.
- **A copy of the saved file before a migration**, so a release could redo a step that lost data. Rejected: it adds
  a second copy of the data to keep in step, and a broken step is caught by the frozen JSON of the old version in the
  tests before it ships.
