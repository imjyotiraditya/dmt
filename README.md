# dmt

[![telegram](https://img.shields.io/badge/telegram-dmtpanda-26A5E4?style=flat-square&logo=telegram&logoColor=white)](https://t.me/dmtpanda)
[![release](https://img.shields.io/github/v/release/imjyotiraditya/dmt?style=flat-square&color=e07a2f)](https://github.com/imjyotiraditya/dmt/releases/latest)

dear music, thanks.

a tui-inspired music player for android. music helped me through a lot,
this is the thank you note.

## screenshots

<table>
  <tr>
    <td width="68%">
      <img src=".github/screenshots/library-landscape.png" alt="library, landscape" />
    </td>
    <td width="32%" rowspan="2">
      <img src=".github/screenshots/player-portrait.png" alt="player, portrait" />
    </td>
  </tr>
  <tr>
    <td>
      <img src=".github/screenshots/player-landscape.png" alt="player, landscape" />
    </td>
  </tr>
</table>

## what it does

- plays the music on your phone, and can stream from a self-hosted jellyfin
  server instead: sources get their own screen, tap to switch, log in once
  and it remembers the server and token (never the password). covers, lyrics
  and format info come from the server too
- home screen with shelves for your most played albums, tracks and artists,
  plus a "try something new" row of things you have never got round to playing
- bottom nav across home, library, search, sources and cfg
- library tabs for tracks, albums, artists, folders and playlists, each showing
  how many and how long; tap an album to play it, [↗] to open it, long-press
  for play/queue actions
- one search across tracks, albums and artists, so typing a guest artist finds
  the track they feature on
- a folder blocklist in cfg keeps voice notes and other junk out of the library
- cover art rendered as colored ascii with a light sweep while playing.
  tracks without art get a generated ascii pattern of their own.
  there is a raw artwork toggle if ascii is not your thing
- full screen player with a proper landscape layout, mini player everywhere else,
  swipe down to dismiss
- queue and track info live in bottom sheets, queue items can be removed one by one
- sleep timer (15/30/60), playback speed (0.75x to 2x), shuffle and repeat
- listening stats: time listened, play counts, most played with accent bars
- android auto: browse by tracks and albums (grid), voice search,
  shuffle and repeat buttons on the car screen
- picks up where you left off: last queue, track and position restore on launch,
  deleted files silently drop out
- format nerd info when you want it: codec, bitrate, sample rate, bit depth, size,
  down to spotting he-aac and vbr encodes
- bundled ffmpeg decoders, so formats the platform won't touch still play,
  dolby ac-4 included: mainline ffmpeg has no ac-4 decoder and android only
  ships one on licensed devices, so this uses paul b mahol's decoder from
  librempeg, built by
  [dmt-decoder-ffmpeg](https://github.com/imjyotiraditya/dmt-decoder-ffmpeg)
- if a track really can't be decoded it says so on the player instead of
  running silently to the end
- four accents (orange, moss, steel, mono) and the launcher icon follows
- hooks into the system equalizer, media notification with album art
- handles big libraries without falling over
- lyrics read straight from the file tags (mp3, flac, m4a), supporting synced
  apple ttml (line and word timing, background vocals, duet sides, multiple
  singers) and lrc, with a karaoke view in the player, plus translation,
  transliteration and a script toggle (original or romanized) where the
  file has them
- when a file has no lyrics, the lyr? key fetches them from lrclib on
  demand - never automatically
- shows up in external media widgets (kustom and the like) so they can read
  what's playing
- artists come from the album artist tag, so a feature doesn't split the
  artist into a new entry or turn the album into "various artists"

## building

open in android studio and hit run. minSdk 30.
release builds are minified and land around 12mb, most of it the
bundled ffmpeg decoders.
ci builds a signed release apk on every push.

## stack

kotlin, compose, media3, datastore, ibm plex mono. single state + actions,
no magic.

the ffmpeg decoder and media3 itself are built from
[dmt-ffmpeg](https://github.com/imjyotiraditya/dmt-ffmpeg) and
[dmt-media3](https://github.com/imjyotiraditya/dmt-media3).

no ads, no analytics. the network is only ever used to talk to your own
jellyfin server, and to lrclib when you ask for missing lyrics. it just
plays music.
