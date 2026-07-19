package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.core.common.TuiPanel
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.LyricLine
import dev.jyotiraditya.dmt.domain.model.LyricWord
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Transliteration
import dev.jyotiraditya.dmt.domain.model.Voice
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import kotlin.math.ceil

private enum class LineState { ACTIVE, PASSED, UPCOMING }

@Composable
fun LyricsPanel(
    lyrics: Lyrics,
    trackId: String?,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    romanized: Boolean,
    contentAspect: Float,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val position = smoothPositionMs(positionMs, isPlaying)
    val listState = rememberLazyListState()
    val scrollTarget = remember(position, lyrics) {
        if (!lyrics.synced) {
            -1
        } else {
            val active = lyrics.lines.indexOfFirst { position in it.startMs until it.endMs }
            if (active >= 0) active else lyrics.lines.indexOfLast { it.endMs in 0..position }
        }
    }

    LaunchedEffect(trackId) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(scrollTarget) {
        if (lyrics.synced && scrollTarget >= 0) {
            listState.animateScrollToItem((scrollTarget - 2).coerceAtLeast(0))
        }
    }

    TuiPanel(modifier = modifier) {
        Box(
            modifier = Modifier
                .aspectRatio(contentAspect),
        ) {
            LazyColumn(state = listState) {
                itemsIndexed(lyrics.lines) { _, line ->
                    LyricLineRows(
                        line = line,
                        romanized = romanized,
                        state = lineState(line, position, lyrics.synced),
                        positionMs = position,
                        seekable = lyrics.synced && durationMs > 0 && line.startMs >= 0,
                        onClick = {
                            onSeekFraction(
                                (line.startMs.toFloat() / durationMs).coerceIn(0f, 1f),
                            )
                        },
                    )
                }
            }
        }
    }
}

private fun lineState(
    line: LyricLine,
    positionMs: Long,
    synced: Boolean,
): LineState =
    when {
        !synced || line.startMs < 0 -> LineState.UPCOMING
        positionMs in line.startMs until line.endMs -> LineState.ACTIVE
        line.endMs <= positionMs -> LineState.PASSED
        else -> LineState.UPCOMING
    }

private data class LyricRun(
    val background: Boolean,
    val text: String,
    val words: List<LyricWord>,
)

private fun buildRuns(line: LyricLine): List<LyricRun> {
    if (line.words.isEmpty()) {
        return listOf(
            LyricRun(
                background = false,
                text = line.text,
                words = emptyList(),
            ),
        )
    }

    val groups = mutableListOf<MutableList<LyricWord>>()
    line.words.sortedBy { it.start }.forEach { word ->
        val current = groups.lastOrNull()
        if (current != null && current.last().background == word.background) {
            current += word
        } else {
            groups += mutableListOf(word)
        }
    }

    val runs = mutableListOf<LyricRun>()
    var boundary = 0
    groups.forEachIndexed { index, group ->
        val runStart = boundary
        val runEnd = if (index == groups.lastIndex) {
            line.text.length
        } else {
            groups[index + 1].first().start
        }
        val raw = line.text.substring(runStart, runEnd)
        val leading = raw.takeWhile(Char::isWhitespace).length
        val trimmed = raw.trim()
        val shift = runStart + leading
        val words = group.map { word ->
            word.copy(
                start = (word.start - shift).coerceIn(0, trimmed.length),
                end = (word.end - shift).coerceIn(0, trimmed.length),
            )
        }
        runs += LyricRun(
            background = group.first().background,
            text = trimmed,
            words = words,
        )
        boundary = runEnd
    }
    return runs
}

private fun isArabicScript(text: String): Boolean =
    text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC }

private fun TextUnit.scaledBy(factor: Float): TextUnit = (value * factor).sp

private fun singerColorFor(line: LyricLine): Color =
    when {
        line.interlude -> singerPalette.first()
        line.singer < 0 -> GroupVoice
        else -> singerPalette[line.singer % singerPalette.size]
    }

@Composable
private fun LyricLineRows(
    line: LyricLine,
    romanized: Boolean,
    state: LineState,
    positionMs: Long,
    seekable: Boolean,
    onClick: () -> Unit,
) {
    val translit = line.transliteration
    val shown = if (romanized && translit != null) {
        line.copy(
            text = translit.text,
            words = translit.words,
            transliteration = Transliteration(
                text = line.text,
                words = line.words,
            ),
        )
    } else {
        line
    }

    val singerColor = singerColorFor(shown)
    val hasSinger = !shown.interlude && shown.singer >= 0
    val align = when (shown.voice) {
        Voice.SECONDARY -> TextAlign.End
        Voice.GROUP -> TextAlign.Center
        else -> TextAlign.Start
    }

    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (seekable) it.tuiClickable(onClick) else it }

    if (shown.interlude) {
        InterludeRow(
            line = shown,
            state = state,
            positionMs = positionMs,
            modifier = rowModifier.padding(
                top = if (shown.sectionStart) 18.dp else 6.dp,
                bottom = 6.dp,
            ),
        )
        return
    }

    val runs = remember(shown) { buildRuns(shown) }
    Column(
        modifier = rowModifier
            .padding(top = if (shown.sectionStart) 18.dp else 6.dp, bottom = 6.dp),
    ) {
        runs.forEach { run ->
            LyricRunText(
                run = run,
                state = state,
                positionMs = positionMs,
                singerColor = singerColor,
                hasSinger = hasSinger,
                align = align,
            )
        }
        shown.transliteration?.let {
            LyricRunText(
                run = LyricRun(
                    background = true,
                    text = it.text,
                    words = it.words,
                ),
                state = state,
                positionMs = positionMs,
                singerColor = singerColor,
                hasSinger = hasSinger,
                align = align,
            )
        }
        val originals = if (runs.size == shown.translation.size) runs.map { it.text } else null
        shown.translation.forEachIndexed { i, segment ->
            val original = originals?.get(i) ?: shown.text
            if (!segment.equals(original, ignoreCase = true)) {
                SecondaryLyricText(segment, align)
            }
        }
    }
}

@Composable
private fun SecondaryLyricText(text: String, align: TextAlign) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TuiDim,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun InterludeRow(
    line: LyricLine,
    state: LineState,
    positionMs: Long,
    modifier: Modifier,
) {
    val annotated = if (state == LineState.ACTIVE) {
        val span = (line.endMs - line.startMs).coerceAtLeast(1)
        val fraction = ((positionMs - line.startMs).toFloat() / span).coerceIn(0f, 1f)
        val filled = ceil(fraction * line.text.length).toInt().coerceIn(0, line.text.length)
        buildAnnotatedString {
            append(line.text)
            addStyle(SpanStyle(color = TuiAccent), 0, filled)
            addStyle(SpanStyle(color = TuiFaint), filled, line.text.length)
        }
    } else {
        AnnotatedString(line.text)
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.headlineSmall,
        color = if (state == LineState.PASSED) TuiFaint else TuiDim,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

private data class SweepColors(
    val sung: Color,
    val unsung: Color,
)

private fun runState(
    run: LyricRun,
    positionMs: Long,
    fallback: LineState,
): LineState {
    if (run.words.isEmpty()) return fallback
    val start = run.words.minOf { it.startMs }
    val end = run.words.maxOf { it.endMs }
    return when {
        positionMs < start -> LineState.UPCOMING
        positionMs >= end -> LineState.PASSED
        else -> LineState.ACTIVE
    }
}

@Composable
private fun LyricRunText(
    run: LyricRun,
    state: LineState,
    positionMs: Long,
    singerColor: Color,
    hasSinger: Boolean,
    align: TextAlign,
) {
    val sweepState = runState(run, positionMs, state)
    val karaoke = sweepState == LineState.ACTIVE && run.words.isNotEmpty()
    val sweepColors = if (run.background) {
        SweepColors(
            sung = TuiFg,
            unsung = TuiFaint,
        )
    } else {
        SweepColors(
            sung = singerColor,
            unsung = TuiDim,
        )
    }

    val annotated: AnnotatedString = if (karaoke) {
        buildAnnotatedString {
            append(run.text)
            run.words.forEach { word ->
                when {
                    positionMs >= word.endMs ->
                        addStyle(SpanStyle(color = sweepColors.sung), word.start, word.end)

                    positionMs >= word.startMs -> {
                        val span = (word.endMs - word.startMs).coerceAtLeast(1)
                        val fraction =
                            ((positionMs - word.startMs).toFloat() / span).coerceIn(0f, 1f)
                        val length = word.end - word.start
                        val exact = fraction * length
                        val sungEnd = word.start + exact.toInt().coerceIn(0, length - 1)
                        if (sungEnd > word.start) {
                            addStyle(SpanStyle(color = sweepColors.sung), word.start, sungEnd)
                        }
                        val edge = lerp(sweepColors.unsung, sweepColors.sung, exact - exact.toInt())
                        addStyle(SpanStyle(color = edge), sungEnd, sungEnd + 1)
                        if (sungEnd + 1 < word.end) {
                            addStyle(SpanStyle(color = sweepColors.unsung), sungEnd + 1, word.end)
                        }
                    }

                    else ->
                        addStyle(SpanStyle(color = sweepColors.unsung), word.start, word.end)
                }
            }
        }
    } else {
        AnnotatedString(run.text)
    }

    val lineColor = when {
        run.background && sweepState == LineState.ACTIVE -> TuiFg
        run.background && sweepState == LineState.PASSED -> TuiFaint
        run.background -> TuiDim
        sweepState == LineState.ACTIVE && karaoke -> TuiDim
        sweepState == LineState.ACTIVE -> singerColor
        sweepState == LineState.PASSED && hasSinger -> lerp(singerColor, TuiFaint, 0.8f)
        sweepState == LineState.PASSED -> TuiFaint
        else -> TuiDim
    }

    val baseStyle = if (run.background) {
        MaterialTheme.typography.bodySmall
    } else {
        MaterialTheme.typography.headlineSmall
    }
    val arabic = remember(run.text) { isArabicScript(run.text) }

    Text(
        text = annotated,
        style = baseStyle.copy(
            fontSize = if (arabic) baseStyle.fontSize.scaledBy(1.18f) else baseStyle.fontSize,
            fontWeight = if (sweepState == LineState.ACTIVE && !run.background) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            letterSpacing = 0.sp,
            textDirection = TextDirection.Content,
        ),
        color = lineColor,
        textAlign = align,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun smoothPositionMs(positionMs: Long, isPlaying: Boolean): Long {
    var display by remember { mutableLongStateOf(positionMs) }
    LaunchedEffect(positionMs, isPlaying) {
        if (!isPlaying) {
            display = positionMs
            return@LaunchedEffect
        }
        val anchorPositionMs = positionMs
        var anchorNanos = 0L

        while (true) {
            withFrameNanos { frameNanos ->
                if (anchorNanos == 0L) anchorNanos = frameNanos

                val elapsedMs = (frameNanos - anchorNanos) / 1_000_000
                val interpolatedMs = anchorPositionMs + elapsedMs
                val wouldJumpBackSlightly =
                    interpolatedMs < display && display - interpolatedMs < 300

                display = if (wouldJumpBackSlightly) display else interpolatedMs
            }
        }
    }
    return display
}
