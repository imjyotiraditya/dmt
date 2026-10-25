package dev.jyotiraditya.dmt.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorProducer

/** Fills the background with [color], read while drawing so an animated colour only redraws. */
fun Modifier.background(color: ColorProducer): Modifier = drawBehind { drawRect(color()) }
