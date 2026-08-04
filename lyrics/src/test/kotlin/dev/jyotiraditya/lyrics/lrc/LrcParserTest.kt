package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.Voice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/lyrics/$name")) {
        "missing fixture $name"
    }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

class LrcLyricsParserTest {

    @Test
    fun `plain line-synced lrc has no word timing`() {
        val lyrics = LrcParser.parse(fixture("plain.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertTrue(lyrics.lines.isNotEmpty())
        assertTrue(lyrics.lines.all { it.words.isEmpty() })

        val first = lyrics.lines.first { !it.interlude }
        assertEquals(21_769L, first.startMs)
        assertEquals("I'm tired of being what you want me to be", first.text)
    }

    @Test
    fun `enhanced word-timed lrc extracts per-word timing`() {
        val lyrics = LrcParser.parse(fixture("enhanced.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val first = lyrics.lines.first { it.words.isNotEmpty() }
        assertTrue(first.words.isNotEmpty())
        for (word in first.words) {
            assertTrue(word.start in 0..first.text.length)
            assertTrue(word.end in word.start..first.text.length)
            assertTrue(word.endMs >= word.startMs)
        }
        // no leftover <mm:ss.xxx> word tags should remain in any displayed text
        assertTrue(lyrics.lines.none { it.text.contains("<") })
    }

    @Test
    fun `voice prefix and background lines are handled`() {
        val lyrics = LrcParser.parse(fixture("voice_bg.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // the "v1:" voice marker must never leak into displayed text
        assertTrue(lyrics.lines.none { it.text.startsWith("v1:") || it.text.contains("v1:") })

        // [bg: ... ] lines must survive as their own lines, fully word-timed and flagged background
        val bgLines = lyrics.lines.filter {
            it.words.isNotEmpty() &&
                    it.words.all { w -> w.background }
        }
        assertTrue(bgLines.isNotEmpty())
        val sampleBg = bgLines.first { it.text.startsWith("I'm") }
        assertEquals("I'm just tired of lookin' the other way", sampleBg.text)
        assertEquals(77_254L, sampleBg.startMs)
    }

    @Test
    fun `bg lines with a duplicated nested timestamp and voice prefix are cleaned`() {
        val lyrics = LrcParser.parse(fixture("voice_bg_nested.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // neither the outer nor the nested "[mm:ss.xxx]v1:" prefix should leak into any text
        assertTrue(lyrics.lines.none { it.text.contains("v1:") || it.text.contains("[") })

        // the bg line here is romaji of the same line (different script), so it's a
        // transliteration attached to the main line, not a separate background line
        val main = lyrics.lines.first { it.startMs == 373L }
        assertEquals("まる で 御伽 の 話", main.text)
        val transliteration = main.transliteration
        assertNotNull(transliteration)
        assertEquals("Maru de otogi no hanashi", transliteration!!.text)
        assertTrue(lyrics.lines.none { it.words.any { w -> w.background } })
    }

    @Test
    fun `non-nested bg adlib after a cjk line is not swallowed as its transliteration`() {
        val lyrics = LrcParser.parse(fixture("voice_bg_cjk_adlib.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val main = lyrics.lines.first { it.startMs == 11_891L }
        assertEquals("天才的なアイドル様", main.text)
        assertNotNull(main.transliteration)
        assertEquals("tensaiteki na aidoru-sama", main.transliteration!!.text)

        val adlib = lyrics.lines.first { it.words.isNotEmpty() && it.words.all { w -> w.background } }
        assertEquals("(You're my savior, you're my saving grace)", adlib.text)
    }

    @Test
    fun `duet lrc keeps voice sides, own line ends, and bg singer`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        // a line's end comes from its own final word stamp, not the start of the
        // next line that overlaps it
        val v3 = lines.first { it.text.startsWith("だから") }
        assertEquals(68_820L, v3.endMs)

        // v1 and v3 are pinned to the same side (singer index parity), v2 to the other
        val v1 = lines.first { it.text.startsWith("限り有る") }
        assertEquals(v3.voice, v1.voice)

        // a standalone bg line keeps background words and inherits the singer it backs
        val bg = lines.first { it.startMs == 194_156L }
        assertTrue(bg.words.isNotEmpty())
        assertTrue(bg.words.all { it.background })
        assertEquals(lines.first { it.startMs == 193_265L }.singer, bg.singer)
    }

    @Test
    fun `same-timestamp bilingual lines merge into one line with transliteration`() {
        val lyrics = LrcParser.parse(fixture("bilingual.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        // romaji + original script sharing a timestamp collapse into one line:
        // the original script stays as text, romaji attaches as transliteration
        val first = lines.first { it.startMs == 16_240L }
        assertEquals("単純なステージ", first.text)
        assertEquals("Tanjun na stage", first.transliteration?.text)

        // the merged line spans to the next line's start instead of the romaji
        // half getting a zero-length duration
        assertEquals(18_570L, first.endMs)

        // english-only lines have no partner and stay plain single lines
        val english = lines.first { it.text == "BLACK ROVER" }
        assertNull(english.transliteration)

        // every pair collapsed: no two remaining lines share a start time
        assertEquals(lines.size, lines.map { it.startMs }.distinct().size)
    }

    @Test
    fun `three same-timestamp lines merge romaji as transliteration and english as translation`() {
        val lyrics = LrcParser.parse(fixture("trilingual.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        val first = lines.first { it.startMs == 17_920L }
        assertEquals("レーダー　あたしの涙を探して", first.text)
        assertEquals("RĒDĀ atashi no namida o sagashite", first.transliteration?.text)
        assertEquals(listOf("Radar, search for my tears"), first.translation.map { it.text })

        // spans through the latest of the three original end times
        assertEquals(24_200L, first.endMs)

        // the translation kept its own word timing from the source file, not just plain text
        val translation = first.translation.single()
        assertTrue(translation.words.isNotEmpty())
        assertEquals(24_200L, translation.words.last().endMs)

        // every triplet collapsed: no two remaining lines share a start time
        assertEquals(lines.size, lines.map { it.startMs }.distinct().size)
    }

    @Test
    fun `matches detects bracket timestamps only`() {
        assertTrue(LrcParser.matches(fixture("plain.lrc")))
        assertTrue(LrcParser.matches(fixture("enhanced.lrc")))
        assertFalse(LrcParser.matches("just some plain unsynced text\nwith multiple lines"))
    }

    @Test
    fun `parse returns null when there is nothing synced`() {
        assertNull(LrcParser.parse("no timestamps here at all"))
    }

    @Test
    fun `id3-style header tags are ignored, not parsed as garbage lines`() {
        val raw = """
            [ti:Some Song]
            [ar:Some Artist]
            [al:Some Album]
            [length:03:41.51]
            [by:whoever ripped this]
            [00:14.53]The end is distant
        """.trimIndent()

        val lyrics = LrcParser.parse(raw)
        assertNotNull(lyrics)
        assertEquals(1, lyrics!!.lines.count { !it.interlude })
        assertEquals("The end is distant", lyrics.lines.first { !it.interlude }.text)
    }

    @Test
    fun `a bare timestamp with no text after it is dropped, not kept as an empty line`() {
        val raw = """
            [00:14.53]The end is distant
            [00:20.00]
            [00:25.94]Yet one grain of hope remains here
        """.trimIndent()

        val lyrics = LrcParser.parse(raw)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.lines.none { it.text.isBlank() })
    }

    @Test
    fun `three-digit millisecond fractions parse the same as two-digit centiseconds`() {
        val raw = """
            [00:01.234]millisecond precision
            [00:05.67]centisecond precision
        """.trimIndent()

        val lyrics = LrcParser.parse(raw)
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }
        assertEquals(1_234L, lines.first { it.text == "millisecond precision" }.startMs)
        assertEquals(5_670L, lines.first { it.text == "centisecond precision" }.startMs)
    }

    @Test
    fun `real trio track parses every line and resolves three distinct singers`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // the "♪♪♪" intro is now an interlude, so one less than 45
        val sung = lyrics.lines.filter { !it.interlude }
        assertEquals(44, sung.size)

        val singers = sung.map { it.singer }.filter { it >= 0 }.distinct()
        assertEquals(3, singers.size)
    }

    @Test
    fun `real trio track folds the intro note-glyph pickup into an interlude marker`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        val intro = lyrics!!.lines.first { it.startMs == 50L }
        assertEquals("* * *", intro.text)
        assertTrue(intro.interlude)
        assertEquals(-1, intro.singer)
    }

    @Test
    fun `a note-glyph line and a plain silence gap render the same marker`() {
        // glyph intro vs a real silence gap, same marker either way
        val glyphIntro = LrcParser.parse(fixture("duet.lrc"))!!.lines.first { it.interlude }
        val gapIntro = LrcParser.parse(fixture("plain.lrc"))!!.lines.first { it.interlude }

        assertEquals("* * *", glyphIntro.text)
        assertEquals("* * *", gapIntro.text)
    }

    @Test
    fun `real trio track keeps a mid-song overlapping bg line separate from the main lyric`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        // the main line here is a totally different sentence from the bg line
        // that starts under it, so this has to stay two lines, not get merged
        // as a transliteration pair the way the CJK/romaji bg cases do
        val main = lyrics!!.lines.first { it.startMs == 193_265L }
        assertEquals("有り余る愛の迷うまま", main.text)

        val bg = lyrics.lines.first { it.startMs == 194_156L }
        assertEquals("黒い感情も 君へ繋ぐPassion", bg.text)
        assertTrue(bg.words.isNotEmpty())
        assertTrue(bg.words.all { it.background })
        assertEquals(main.singer, bg.singer)
    }

    @Test
    fun `real trio track preserves a literal space inside a run of word-timed CJK`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        // there's a real space between "闇と" and "孤独" in the source, word
        // tags on both sides of it shouldn't make it disappear
        val line = lyrics!!.lines.first { it.startMs == 29_843L }
        assertEquals("永遠と見紛う闇と 孤独が疼く伽藍堂", line.text)
    }

    @Test
    fun `real trio track has valid word bounds and no leaked tags anywhere`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        for (line in lyrics!!.lines) {
            assertFalse(line.text.contains("<"))
            assertFalse(line.text.contains("v1:"))
            assertFalse(line.text.contains("v2:"))
            assertFalse(line.text.contains("v3:"))

            for (word in line.words) {
                assertTrue(word.start in 0..line.text.length)
                assertTrue(word.end in word.start..line.text.length)
                assertTrue(word.endMs >= word.startMs)
            }
        }
    }

    @Test
    fun `real trio track pins each singer to one side for the whole song`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        val sung = lyrics!!.lines.filter { !it.interlude && it.voice != Voice.GROUP }
        val sideBySinger = sung.associate { it.singer to it.voice }

        // same singer, same side, every time it shows up, it shouldn't drift
        // depending on who sang right before it
        for (line in sung) {
            assertEquals(sideBySinger.getValue(line.singer), line.voice)
        }

        // v1 and v3 share a side, v2 is on the other
        assertEquals(sideBySinger.getValue(0), sideBySinger.getValue(2))
        assertNotEquals(sideBySinger.getValue(0), sideBySinger.getValue(1))
    }
}
