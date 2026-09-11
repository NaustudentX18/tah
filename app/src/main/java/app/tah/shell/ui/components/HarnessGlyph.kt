package app.tah.shell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tah.shell.ui.theme.TahPrimary
import app.tah.shell.ui.theme.TahWorking

/** Placeholder harness glyph — brackets + amber chevron (Signal Deck). */
@Composable
fun HarnessGlyph(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * 0.08f, cap = StrokeCap.Round)
        val w = this.size.width
        val h = this.size.height
        // Left bracket
        drawLine(TahPrimary, Offset(w * 0.22f, h * 0.18f), Offset(w * 0.22f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(TahPrimary, Offset(w * 0.22f, h * 0.18f), Offset(w * 0.38f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(TahPrimary, Offset(w * 0.22f, h * 0.82f), Offset(w * 0.38f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        // Right bracket
        drawLine(TahPrimary, Offset(w * 0.78f, h * 0.18f), Offset(w * 0.78f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(TahPrimary, Offset(w * 0.78f, h * 0.18f), Offset(w * 0.62f, h * 0.18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        drawLine(TahPrimary, Offset(w * 0.78f, h * 0.82f), Offset(w * 0.62f, h * 0.82f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        // Amber chevron
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.28f)
            lineTo(w * 0.62f, h * 0.5f)
            lineTo(w * 0.5f, h * 0.72f)
            lineTo(w * 0.38f, h * 0.5f)
            close()
        }
        drawPath(path, TahWorking)
    }
}
