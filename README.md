# dmt

dear music, thanks.

a tui-inspired local music player for android. music helped me through a lot,
this is the thank you note.

## what it does

- plays whatever audio is on your phone, nothing leaves the device
- cover art gets rendered as colored ascii, with a light sweep across it while playing
- library and albums with search, long-press anything to throw it in the queue
- queue and track info live in bottom sheets, tap the mini player for the full screen player
- format nerd info if you want it: codec, bitrate, sample rate, bit depth, file size
- notification and lockscreen controls, headset buttons, audio focus, pause on unplug
- accent picker (orange / moss / steel / mono) that also swaps the launcher icon
- everything lowercase, everything monospace

## building

open in android studio and hit run. minSdk 36.
release builds are minified and land around 4mb.

## stack

kotlin, compose, media3, datastore. single state + actions, no magic.

no ads, no analytics, no network permission. it just plays music.
