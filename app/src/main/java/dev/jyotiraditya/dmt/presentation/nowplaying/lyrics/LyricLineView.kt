package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.theme.LyricType
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.TimedText
import dev.jyotiraditya.lyrics.Voice

private val SECTION_GAP = 28.dp
private val LINE_GAP = 8.dp
private const val ARABIC_SCALE = 1.18f
private const val RESTING_SCALE = 0.96f

/** One line of lyrics: its main text, then any backing vocals, reading or translation beneath. */
@Composable
internal fun LyricLineView(
    line: LyricLine,
    position: State<Long>,
    synced: Boolean,
    romanized: Boolean,
    colors: LyricColors,
    onSeek: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by
        remember(line, synced) {
            derivedStateOf { line.stateAt(position.value, synced) }
        }
    val shown = remember(line, romanized) { if (romanized) line.romanized() else line }
    val active = state == LineState.ACTIVE
    val scale by
        animateFloatAsState(
            targetValue = if (active || !synced) 1f else RESTING_SCALE,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "lineScale",
        )
    val align = shown.voice.align()
    val rightToLeft = remember(shown.text) { shown.text.isRightToLeft() }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .let { if (onSeek != null) it.pressable(onClick = onSeek, sink = 0.98f) else it }
                .padding(top = if (shown.sectionStart) SECTION_GAP else LINE_GAP, bottom = LINE_GAP)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = align.origin(rightToLeft)
                },
    ) {
        if (shown.interlude) {
            Interlude(shown, position, active, colors)
            return@Column
        }
        val runs =
            remember(shown) {
                val main = buildRuns(shown)
                main + secondaryRunsFor(shown, main)
            }
        val singer = colors.singer(shown)
        runs.forEach { run ->
            RunText(run, active, position, singer, colors, align)
        }
    }
}

@Composable
private fun RunText(
    run: LyricRun,
    lineActive: Boolean,
    position: State<Long>,
    singer: Color,
    colors: LyricColors,
    align: TextAlign,
) {
    val style = remember(run.text, run.background, align) { styleFor(run, align) }
    val sweeps = lineActive && run.words.isNotEmpty()
    if (sweeps) {
        SweepText(
            text = run.text,
            words = run.words,
            position = position,
            style = style,
            sung = if (run.background) colors.backing else singer,
            unsung = colors.unsung,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        val color =
            when {
                lineActive && run.background -> colors.backing
                lineActive -> singer
                else -> colors.unsung
            }
        BasicText(
            text = run.text,
            style = style.copy(color = color),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** An instrumental break, shown as dots that fill over its length. */
@Composable
private fun Interlude(
    line: LyricLine,
    position: State<Long>,
    active: Boolean,
    colors: LyricColors,
) {
    val words =
        remember(line) {
            listOf(
                LyricWord(
                    line.startMs,
                    line.endMs,
                    0,
                    line.text.length,
                    background = false,
                ),
            )
        }
    val style = LyricType.copy(textAlign = TextAlign.Center)
    if (active) {
        SweepText(
            text = line.text,
            words = words,
            position = position,
            style = style,
            sung = colors.lead,
            unsung = colors.unsung,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        BasicText(
            text = line.text,
            style = style.copy(color = colors.unsung),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun styleFor(run: LyricRun, align: TextAlign): TextStyle {
    val base =
        if (run.background) {
            LyricType.copy(
                fontSize = LyricType.fontSize * 0.66f,
                lineHeight = LyricType.lineHeight * 0.7f,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
            )
        } else {
            LyricType
        }
    val arabic =
        run.text.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.ARABIC }
    return base.copy(
        fontSize = if (arabic) (base.fontSize.value * ARABIC_SCALE).sp else base.fontSize,
        textAlign = align,
        textDirection = TextDirection.Content,
    )
}

private fun Voice.align(): TextAlign = when (this) {
    Voice.SECONDARY -> TextAlign.End
    Voice.GROUP -> TextAlign.Center
    else -> TextAlign.Start
}

/** The edge a line scales around: the one it is aligned to, mirrored for right-to-left text. */
private fun TextAlign.origin(rightToLeft: Boolean): TransformOrigin = when (this) {
    TextAlign.Center -> TransformOrigin.Center
    TextAlign.End -> if (rightToLeft) LEFT_EDGE else RIGHT_EDGE
    else -> if (rightToLeft) RIGHT_EDGE else LEFT_EDGE
}

private val LEFT_EDGE = TransformOrigin(0f, 0.5f)
private val RIGHT_EDGE = TransformOrigin(1f, 0.5f)

/** Whether the first strongly directional character of this text reads right to left. */
private fun String.isRightToLeft(): Boolean {
    for (char in this) {
        when (Character.getDirectionality(char)) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT,
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC,
            -> return true

            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
        }
    }
    return false
}

/** The line with its romanized reading as the main text and the original beneath. */
private fun LyricLine.romanized(): LyricLine {
    val reading = transliteration ?: return this
    return copy(
        text = reading.text,
        words = reading.words,
        transliteration = TimedText(text = text, words = words),
    )
}
