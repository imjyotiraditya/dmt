package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A small flat block of short text: [color] text on [field]. */
@Composable
fun Tag(text: String, color: Color, field: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        color = color,
        maxLines = 1,
        modifier = modifier.background(field).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
