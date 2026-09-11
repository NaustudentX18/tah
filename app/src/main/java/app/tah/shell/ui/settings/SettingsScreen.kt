package app.tah.shell.ui.settings

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.tah.shell.TahApplication
import app.tah.shell.data.InputMode
import app.tah.shell.data.PermissionMode
import app.tah.shell.ui.TahVmFactory
import app.tah.shell.ui.components.HarnessGlyph
import app.tah.shell.ui.theme.TahOutline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as TahApplication
    val vm: SettingsViewModel = viewModel(factory = TahVmFactory(app.container))
    val settings by vm.settings.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Permission mode", style = MaterialTheme.typography.titleMedium)
            Text(
                "Ask is default. Allow edits auto-allows filesystem writes only — shell exec stays Ask. No silent full-bypass.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.permissionMode == PermissionMode.Ask,
                    onClick = { vm.setPermissionMode(PermissionMode.Ask) },
                    label = { Text("Ask") },
                )
                FilterChip(
                    selected = settings.permissionMode == PermissionMode.AllowReads,
                    onClick = { vm.setPermissionMode(PermissionMode.AllowReads) },
                    label = { Text("Allow reads") },
                )
                FilterChip(
                    selected = settings.permissionMode == PermissionMode.AllowEdits,
                    onClick = { vm.setPermissionMode(PermissionMode.AllowEdits) },
                    label = { Text("Allow edits") },
                )
            }

            HorizontalDivider()
            Text("Input mode", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.inputMode == InputMode.TouchSteer,
                    onClick = { vm.setInputMode(InputMode.TouchSteer) },
                    label = { Text("Touch Steer") },
                )
                FilterChip(
                    selected = settings.inputMode == InputMode.Keys,
                    onClick = { vm.setInputMode(InputMode.Keys) },
                    label = { Text("Keys") },
                )
            }
            Text(
                "Touch Steer is default. Keys is the same session model with a keyboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Needs-you notifications", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Dismiss never approves. Deep-link opens the permission card.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = vm::setNotifications,
                )
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    })
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("System notification settings") }
            Text(
                "If the system denied notifications, Needs-you may miss you — open system settings above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()
            Text("About / OEM wake", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HarnessGlyph(size = 40.dp)
                Column {
                    Text("TAH · Signal Deck", style = MaterialTheme.typography.titleSmall)
                    Text("app.tah.shell · 0.3.0-m2", style = MaterialTheme.typography.bodySmall)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TahOutline, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Foreground / wake (honest)", style = MaterialTheme.typography.titleSmall)
                Text(
                    "While a run is active, TAH starts a lightweight foreground service " +
                        "(ongoing “Active runs” notification) to raise process priority. " +
                        "That is not immortality — OEM battery killers, force-stop, and " +
                        "aggressive Doze can still end the process.\n\n" +
                        "Session metadata, timeline snapshot, and Needs-you gates survive " +
                        "in local prefs. Re-open TAH to resume awareness of what was waiting; " +
                        "the loop does not silently restart mid-tool after death.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("OEM battery tips", style = MaterialTheme.typography.titleSmall)
                Text(
                    "• Pixel / stock: Settings → Apps → TAH → Battery → Unrestricted (optional).\n" +
                        "• Samsung: Apps → TAH → Battery → Allow background activity; disable sleeping apps for TAH.\n" +
                        "• Xiaomi / HyperOS: Autostart + no battery saver for TAH; lock in Recents.\n" +
                        "• Oppo / Vivo / OnePlus: similar “allow background” + autostart toggles.\n\n" +
                        "Even unrestricted, OEMs can kill. Plan for Needs-you notifications, not daemons.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Device: ${Build.MANUFACTURER} ${Build.MODEL} · API ${Build.VERSION.SDK_INT}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "No OFH / Claude / Warp / Termux / OpenClaw marks.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
