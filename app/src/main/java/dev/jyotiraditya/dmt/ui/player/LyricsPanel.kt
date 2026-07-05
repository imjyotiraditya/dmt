package dev.jyotiraditya.dmt.ui.player

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
import dev.jyotiraditya.dmt.data.lyrics.LyricLine
import dev.jyotiraditya.dmt.data.lyrics.LyricWord
import dev.jyotiraditya.dmt.data.lyrics.Lyrics
import dev.jyotiraditya.dmt.data.lyrics.VOICE_GROUP
import dev.jyotiraditya.dmt.data.lyrics.VOICE_SECONDARY
import dev.jyotiraditya.dmt.ui.components.TuiPanel
import dev.jyotiraditya.dmt.ui.components.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import kotlin.math.ceil

private enum class LineState { ACTIVE, PASSED, UPCOMING }

@Composable
fun LyricsPanel(
    lyrics: Lyrics,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
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

    LaunchedEffect(scrollTarget) {
        if (lyrics.synced && scrollTarget >= 0) {
            listState.animateScrollToItem((scrollTarget - 2).coerceAtLeast(0))
        }
    }

    val palette = rememberSingerPalette()

    TuiPanel(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(contentAspect)
        ) {
            LazyColumn(state = listState) {
                itemsIndexed(lyrics.lines) { _, line ->
                    LyricLineRows(
                        line = line,
                        state = lineState(line, position, lyrics.synced),
                        positionMs = position,
                        palette = palette,
                        seekable = lyrics.synced && durationMs > 0 && line.startMs >= 0,
                        onClick = {
                            onSeekFraction(
                                (line.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun lineState(line: LyricLine, positionMs: Long, synced: Boolean): LineState = when {
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
    if (line.words.isEmpty()) return listOf(LyricRun(background = false, line.text, emptyList()))

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
        runs += LyricRun(group.first().background, trimmed, words)
        boundary = runEnd
    }
    return runs
}

private val arabicScriptRanges = listOf(
    0x0600..0x06FF, 0x0750..0x077F, 0x08A0..0x08FF, 0xFB50..0xFDFF, 0xFE70..0xFEFF,
)

private fun isArabicScript(text: String): Boolean =
    text.any { c -> arabicScriptRanges.any { range -> c.code in range } }

private fun TextUnit.scaledBy(factor: Float): TextUnit = (value * factor).sp

private fun singerColorFor(line: LyricLine, palette: List<Color>): Color = when {
    line.interlude -> palette.first()
    line.singer < 0 -> TuiBright
    else -> palette[line.singer % palette.size]
}

@Composable
private fun LyricLineRows(
    line: LyricLine,
    state: LineState,
    positionMs: Long,
    palette: List<Color>,
    seekable: Boolean,
    onClick: () -> Unit,
) {
    val singerColor = singerColorFor(line, palette)
    val hasSinger = !line.interlude && line.singer >= 0
    val align = when (line.voice) {
        VOICE_SECONDARY -> TextAlign.End
        VOICE_GROUP -> TextAlign.Center
        else -> TextAlign.Start
    }

    val rowModifier = Modifier
        .fillMaxWidth()
        .let { if (seekable) it.tuiClickable(onClick) else it }

    if (line.interlude) {
        InterludeRow(
            line = line,
            state = state,
            positionMs = positionMs,
            accent = palette.first(),
            modifier = rowModifier.padding(
                top = if (line.sectionStart) 18.dp else 6.dp,
                bottom = 6.dp,
            )
        )
        return
    }

    val runs = remember(line) { buildRuns(line) }
    Column(
        modifier = rowModifier
            .padding(top = if (line.sectionStart) 18.dp else 6.dp, bottom = 6.dp)
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
    }
}

@Composable
private fun InterludeRow(
    line: LyricLine,
    state: LineState,
    positionMs: Long,
    accent: Color,
    modifier: Modifier,
) {
    val annotated = if (state == LineState.ACTIVE) {
        val span = (line.endMs - line.startMs).coerceAtLeast(1)
        val fraction = ((positionMs - line.startMs).toFloat() / span).coerceIn(0f, 1f)
        val filled = ceil(fraction * line.text.length).toInt().coerceIn(0, line.text.length)
        buildAnnotatedString {
            append(line.text)
            addStyle(SpanStyle(color = accent), 0, filled)
            addStyle(SpanStyle(color = TuiFaint), filled, line.text.length)
        }
    } else {
        AnnotatedString(line.text)
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = if (state == LineState.PASSED) TuiFaint else TuiDim,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

private data class Track(val sung: Color, val sweepTail: Color, val unsung: Color)

private fun runState(run: LyricRun, positionMs: Long, fallback: LineState): LineState {
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
    val runState = runState(run, positionMs, state)
    val karaoke = runState == LineState.ACTIVE && run.words.isNotEmpty()
    val track = if (run.background) {
        Track(sung = TuiFg, sweepTail = TuiDim, unsung = TuiFaint)
    } else {
        Track(sung = singerColor, sweepTail = TuiFg, unsung = TuiDim)
    }

    val annotated: AnnotatedString = if (karaoke) {
        buildAnnotatedString {
            append(run.text)
            run.words.forEach { word ->
                when {
                    positionMs >= word.endMs ->
                        addStyle(SpanStyle(color = track.sung), word.start, word.end)

                    positionMs >= word.startMs -> {
                        val span = (word.endMs - word.startMs).coerceAtLeast(1)
                        val fraction =
                            ((positionMs - word.startMs).toFloat() / span).coerceIn(0f, 1f)
                        val length = word.end - word.start
                        val filled = word.start +
                            ceil(fraction * length).toInt().coerceIn(1, length)
                        addStyle(SpanStyle(color = track.sung), word.start, filled)
                        if (filled < word.end) {
                            addStyle(SpanStyle(color = track.sweepTail), filled, word.end)
                        }
                    }

                    else -> addStyle(SpanStyle(color = track.unsung), word.start, word.end)
                }
            }
        }
    } else {
        AnnotatedString(run.text)
    }

    val lineColor = when {
        run.background && runState == LineState.ACTIVE -> TuiFg
        run.background && runState == LineState.PASSED -> TuiFaint
        run.background -> TuiDim
        runState == LineState.ACTIVE && karaoke -> TuiBright
        runState == LineState.ACTIVE -> singerColor
        runState == LineState.PASSED && hasSinger -> lerp(singerColor, TuiFaint, 0.8f)
        runState == LineState.PASSED -> TuiFaint
        else -> TuiDim
    }

    val alreadyParenthesized = run.text.startsWith("(") && run.text.endsWith(")")
    val display = if (run.background && !alreadyParenthesized) {
        buildAnnotatedString {
            append("(")
            append(annotated)
            append(")")
        }
    } else {
        annotated
    }

    val baseStyle = if (run.background) {
        MaterialTheme.typography.labelSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val arabic = remember(run.text) { isArabicScript(run.text) }

    Text(
        text = display,
        style = baseStyle.copy(
            fontSize = if (arabic) baseStyle.fontSize.scaledBy(1.18f) else baseStyle.fontSize,
            fontWeight = if (runState == LineState.ACTIVE && !run.background) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            },
            letterSpacing = 0.sp,
            textDirection = TextDirection.Content,
        ),
        color = lineColor,
        textAlign = align,
        modifier = Modifier.fillMaxWidth()
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
        val anchorPosition = positionMs
        var anchorNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (anchorNanos == 0L) anchorNanos = frameNanos
                val interpolated = anchorPosition + (frameNanos - anchorNanos) / 1_000_000
                display = if (interpolated < display && display - interpolated < 300) {
                    display
                } else {
                    interpolated
                }
            }
        }
    }
    return display
}
