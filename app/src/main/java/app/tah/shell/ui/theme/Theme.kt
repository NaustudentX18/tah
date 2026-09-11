package app.tah.shell.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TahColorScheme = darkColorScheme(
    primary = TahPrimary,
    onPrimary = TahOnPrimary,
    surface = TahSurface,
    onSurface = TahOnSurface,
    onSurfaceVariant = TahOnSurfaceVariant,
    surfaceContainer = TahSurfaceContainer,
    surfaceBright = TahSurfaceBright,
    outline = TahOutline,
    error = TahReject,
    tertiary = TahNeedsYou,
    secondary = TahSuccess,
)

@Composable
fun TahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TahColorScheme,
        typography = TahTypography,
        content = content,
    )
}
