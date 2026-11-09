# Pianissimo Timer — behavioral spec

**Normative, but not final.** Each item is one checkable rule about what the app does. Code that differs is a bug.
An item may be wrong or out of date. When an item blocks a task or a better design,
say so and propose a new wording. Change an item only after the user agrees. An item tagged **(planned)** is
agreed but not built yet.

**Keep it short.** This is not a full description of the app. An item belongs here only when the rule is not
obvious from using the app or reading the code, and a later change could break it without anyone noticing.
Do not add items for completeness, and do not restate the README or the code. When in doubt, leave it out.

## Terms

The code, the docs and the chat use these words with exactly these meanings. Do not add a term for a part of one
of them: a "tone" or a "ring" is not a thing of its own.

- **Timer**: a named list of steps, kept in a library like sounds. The chosen timer is idle, running or paused.
  When it runs, its steps follow each other without a gap: the bell of a step is the start of the next.
- **Step**: one length of time with its own sound and volume. The bell rings at its end.
- **Bell**: the ring at the end of a step. Android starts it at the set time, even when the app is not running.
- **Sound**: what the bell plays. A name and the settings of the synthesizer. The user keeps a library of sounds;
  the bell carries its own copy, so a later edit of the library does not change it.

## 1. The bell

1.1 The bell rings at the set time with the screen off, the app closed or killed by the system, or Do Not
Disturb on. — why: the app hands the bell to Android at Start; it does not wait for the moment itself.

1.2 The bell plays on the alarm volume, not the notification volume.

1.3 Only Stop and Pause cancel a bell. Closing the app does not: Home, a swipe from recent apps, or a kill by the
system.

1.4 After Stop or Pause, no bell is left scheduled, not even one started earlier and forgotten.

1.5 A phone restart, Settings → Force stop and uninstalling the app cancel the bell (Android does this), and the app
never brings it back: after any of them it opens idle. — why: a restart is a deliberate act or an agreed update, and
a timer of some minutes is almost never running then.

1.6 The app never changes a system volume, the alarm volume included. It sets the bell's loudness only with its
own volume, which can only make the sound quieter. — why: changing the alarm volume would spoil the user's
wake-up alarms.

1.7 Listen plays the bell exactly as it will ring: the same sound, the alarm stream and the app's volume. — why: the
user picks the volume by ear; a preview on another stream, like media, would mislead.

1.8 Pause removes every bell from Android and keeps the time left of the current step; Resume schedules the bells of
that step and the ones after it again, the first for now plus that time. A paused bell cannot ring, even if the app
is killed. Pause does not silence a bell that is already ringing.

1.9 A paused bell survives the app being killed: opening the app shows it paused with the same time left.
— why: its notification outlives the app and must not point to a lost pause.

1.10 The bell plays its step's sound as it was at Start, even after the app is killed; Pause and Resume keep it.
Editing or deleting that sound later does not change the bell. — why: the bell rings without the app, so it carries
its own copy.

## 2. Visibility

2.1 While a bell is scheduled, the user always sees it: the fermata icon with a chip of the time left to the next bell
in the status bar, and a notification with that time in the shade, with the buttons of the screen. While a step rings
and the next one waits, both notifications show: the ringing one is the sound's own and goes away with it. The
notification shows on the lock screen too. — why: a bell nobody could see rang at night.

2.2 The notification comes back after the user swipes it away.

2.3 Opening the app while a bell is scheduled shows its countdown.

2.4 Nothing of a bell stays after it has played: the fermata icon shows only while a bell waits, is paused or sounds.

2.5 The status bar icon differs for each state: waiting, paused and ringing. — why: the chip shows a time both
when waiting and when paused, and a ringing bell has no chip, so the icon tells the state.

2.6 On Samsung One UI the chip and the Now bar on the lock screen show only thanks to an undocumented extra on the
waiting and paused notifications. The Now bar shows no time left, and One UI labels the notification "Gemini
Intelligence" in the shade and in the open Now bar card. Checked on One UI 9.0; an update may break it.

## 3. Setting the time

3.1 The time is set on three wheels, hours (0–9), minutes and seconds (0–59), so a step is at most 9:59:59.

## 4. Sounds

4.1 There is always at least one sound. No sound is built in: any can be renamed, edited or deleted.

4.2 Sound names are unique, ignoring case.

## 5. Timers

5.1 There is always at least one timer, a timer always has at least one step, and a step always has a length greater
than zero. Timer names are unique, ignoring case. — why: a 0:00 step would ring together with the one before it.

5.2 A step names its sound; it does not keep a copy. A step whose sound was deleted shows and plays the first sound
in the library. — why: an edit of a sound in the library must reach every timer that uses it.

5.3 Start hands Android one bell per step, all at once; the ring of a step schedules nothing. — why: a chain built
at each ring breaks when one link fails.

## 6. Saved data

6.1 An update never loses the timers and sounds the user made: data saved by any earlier version is brought to the
current format at the first launch, step by step through every version between. The builds up to 0.2.0 saved into
other files; that data is not read.

6.2 Saved data the app cannot use never crashes it, and a bad value never costs more than itself: a value outside its
range gives way to the default of its field, an unreadable library to the default one, an unusable state to idle with
its bells cancelled. — why: a crash on reading would repeat on every launch; a lost timer is the user's work.

6.3 An update resets a running or paused timer to idle whenever the saved format changes. — why: the bells carry
copies of the sounds in the old form; a timer of some minutes is almost never running during an update.
