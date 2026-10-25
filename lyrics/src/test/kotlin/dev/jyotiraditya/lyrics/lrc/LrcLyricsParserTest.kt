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
        assertTrue(lyrics.lines.none { it.text.contains("<") })
    }

    @Test
    fun `voice prefix and background lines are handled`() {
        val lyrics = LrcParser.parse(fixture("voice_bg.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        assertTrue(lyrics.lines.none { it.text.startsWith("v1:") || it.text.contains("v1:") })

        val bgLines =
            lyrics.lines.filter {
                it.words.isNotEmpty() && it.words.all { w -> w.background }
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

        assertTrue(lyrics.lines.none { it.text.contains("v1:") || it.text.contains("[") })

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

        val adlib =
            lyrics.lines.first {
                it.words.isNotEmpty() && it.words.all { w -> w.background }
            }
        assertEquals("You're my savior, you're my saving grace", adlib.text)
        assertEquals(adlib.text.length, adlib.words.last().end)
    }

    @Test
    fun `duet lrc keeps voice sides, own line ends, and bg singer`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        val v3 = lines.first { it.text.startsWith("だから") }
        assertEquals(68_820L, v3.endMs)

        val v1 = lines.first { it.text.startsWith("限り有る") }
        assertEquals(v3.voice, v1.voice)

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

        val first = lines.first { it.startMs == 16_240L }
        assertEquals("単純なステージ", first.text)
        assertEquals("Tanjun na stage", first.transliteration?.text)

        assertEquals(18_570L, first.endMs)

        val english = lines.first { it.text == "BLACK ROVER" }
        assertNull(english.transliteration)

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

        assertEquals(24_200L, first.endMs)

        val translation = first.translation.single()
        assertTrue(translation.words.isNotEmpty())
        assertEquals(24_200L, translation.words.last().endMs)

        assertEquals(lines.size, lines.map { it.startMs }.distinct().size)
    }

    @Test
    fun `word times written in brackets are read as words, not as line times`() {
        val lyrics =
            LrcParser.parse(
                "[01:17.254][01:17.254]I'm [01:17.726]just [01:18.193]tired [01:18.710]of",
            )
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { !it.interlude }
        assertEquals(77_254L, line.startMs)
        assertEquals("I'm just tired of", line.text)
        assertEquals(
            listOf("I'm", "just", "tired", "of"),
            line.words.map { line.text.substring(it.start, it.end) },
        )
        assertEquals(listOf(77_254L, 77_726L, 78_193L, 78_710L), line.words.map { it.startMs })
    }

    @Test
    fun `the last word of a line is sung even when no time closes it`() {
        val lyrics = LrcParser.parse("[01:23.055]<01:23.055>Starting <01:24.039>to <01:24.474>fade")
        assertNotNull(lyrics)

        val last = lyrics!!.lines.first { !it.interlude }.words.last()
        assertEquals(24_474L + 60_000L, last.startMs)
        assertTrue(last.endMs > last.startMs)
    }

    @Test
    fun `word times written as offsets from the line are moved onto it`() {
        val lyrics =
            LrcParser.parse(
                "[01:15.433]<00:00.000>Wires <00:00.920>and <00:01.288>chains<00:07.419>",
            )
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { !it.interlude }
        assertEquals(listOf(75_433L, 76_353L, 76_721L), line.words.map { it.startMs })
        assertEquals(82_852L, line.words.last().endMs)
    }

    @Test
    fun `a line written once but sung twice keeps its word timing at both times`() {
        val lyrics =
            LrcParser.parse(
                "[01:17.254][02:57.887]<01:17.254>I'm <01:17.726>just <01:18.193>tired <01:18.710>",
            )
        assertNotNull(lyrics)

        val sung = lyrics!!.lines.filter { !it.interlude }
        assertEquals(listOf(77_254L, 177_887L), sung.map { it.startMs })
        assertEquals(listOf(177_887L, 178_359L, 178_826L), sung.last().words.map { it.startMs })
    }

    @Test
    fun `lines a hair apart still pair as one line and its reading`() {
        val lyrics =
            LrcParser.parse(
                """
                [00:16.24]単純なステージ
                [00:16.35]Tanjun na stage
                [00:18.57]渇くほど願い
                """
                    .trimIndent(),
            )
        assertNotNull(lyrics)

        val first = lyrics!!.lines.first { !it.interlude }
        assertEquals("単純なステージ", first.text)
        assertEquals("Tanjun na stage", first.transliteration?.text)
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
        val raw =
            """
            [ti:Some Song]
            [ar:Some Artist]
            [al:Some Album]
            [length:03:41.51]
            [by:whoever ripped this]
            [00:14.53]The end is distant
            """
                .trimIndent()

        val lyrics = LrcParser.parse(raw)
        assertNotNull(lyrics)
        assertEquals(1, lyrics!!.lines.count { !it.interlude })
        assertEquals("The end is distant", lyrics.lines.first { !it.interlude }.text)
    }

    @Test
    fun `a bare timestamp with no text after it is dropped, not kept as an empty line`() {
        val raw =
            """
            [00:14.53]The end is distant
            [00:20.00]
            [00:25.94]Yet one grain of hope remains here
            """
                .trimIndent()

        val lyrics = LrcParser.parse(raw)
        assertNotNull(lyrics)
        assertTrue(lyrics!!.lines.none { it.text.isBlank() })
    }

    @Test
    fun `three-digit millisecond fractions parse the same as two-digit centiseconds`() {
        val raw =
            """
            [00:01.234]millisecond precision
            [00:05.67]centisecond precision
            """
                .trimIndent()

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
        val glyphIntro = LrcParser.parse(fixture("duet.lrc"))!!.lines.first { it.interlude }
        val gapIntro = LrcParser.parse(fixture("plain.lrc"))!!.lines.first { it.interlude }

        assertEquals("* * *", glyphIntro.text)
        assertEquals("* * *", gapIntro.text)
    }

    @Test
    fun `real trio track keeps a mid-song overlapping bg line separate from the main lyric`() {
        val lyrics = LrcParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

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

        for (line in sung) {
            assertEquals(sideBySinger.getValue(line.singer), line.voice)
        }

        assertEquals(sideBySinger.getValue(0), sideBySinger.getValue(2))
        assertNotEquals(sideBySinger.getValue(0), sideBySinger.getValue(1))
    }
}
