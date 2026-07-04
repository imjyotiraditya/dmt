# dmt

dear music, thanks.

a tui-inspired local music player for android. music helped me through a lot,
this is the thank you note.

## what it does

- plays the music on your phone, nothing leaves the device
- library, albums and folders tabs, all searchable, long-press anything to queue it
- cover art rendered as colored ascii with a light sweep while playing.
  tracks without art get a generated ascii pattern of their own.
  there is a raw artwork toggle if ascii is not your thing
- full screen player with a proper landscape layout, mini player everywhere else
- queue and track info live in bottom sheets, queue items can be removed one by one
- sleep timer (15/30/60), playback speed (0.75x to 2x), shuffle and repeat
- listening stats: time listened, play counts, most played with accent bars
- android auto: browse by tracks, albums (grid) and folders, voice search,
  shuffle and repeat buttons on the car screen
- picks up where you left off: last queue, track and position restore on launch,
  deleted files silently drop out
- format nerd info when you want it: codec, bitrate, sample rate, bit depth, size
- four accents (orange, moss, steel, mono) and the launcher icon follows
- hooks into the system equalizer, media notification with album art
- handles big libraries without falling over
- lyrics read straight from the file tags (mp3, flac, m4a), supporting synced
  apple ttml (line and word timing, background vocals, multiple singers) and
  lrc, with a karaoke view in the player

## building

open in android studio and hit run. minSdk 33.
release builds are minified and land around 4mb.
ci builds signed debug and release apks on every push.

## stack

kotlin, compose, media3, datastore. single state + actions, no magic.

no ads, no analytics, no network permission. it just plays music.
