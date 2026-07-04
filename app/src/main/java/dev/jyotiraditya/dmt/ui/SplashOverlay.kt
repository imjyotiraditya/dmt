package dev.jyotiraditya.dmt.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import kotlinx.coroutines.delay

@Composable
fun SplashOverlay(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(800)
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(LocalAccent.current)
                )
                Text(
                    text = " dmt",
                    style = MaterialTheme.typography.displaySmall,
                    color = TuiBright
                )
            }
            Text(
                text = "dear music, thanks",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
