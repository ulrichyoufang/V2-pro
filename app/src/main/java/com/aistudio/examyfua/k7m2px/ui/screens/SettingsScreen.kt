package com.aistudio.examyfua.k7m2px.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.worker.NotificationHelper
import com.aistudio.examyfua.k7m2px.worker.StudyReminderWorker
import com.example.BuildConfig
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    darkModeEnabled: Boolean,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    dailyReminderEnabled: Boolean,
    dailyReminderTime: Pair<Int, Int>,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleDailyReminder: (Boolean, Int, Int) -> Unit,
    onExportJson: suspend () -> String,
    onExportCsv: suspend () -> String,
    onClearCache: () -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val formattedTime = remember(dailyReminderTime) {
        String.format(Locale.FRANCE, "%02dh%02d", dailyReminderTime.first, dailyReminderTime.second)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleDailyReminder(true, dailyReminderTime.first, dailyReminderTime.second)
            StudyReminderWorker.scheduleDaily(context, dailyReminderTime.first, dailyReminderTime.second)
            onShowMessage("Rappel quotidien activé à $formattedTime.")
        } else {
            onShowMessage("Permission de notification refusée.")
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Paramètres", color = YfuaTheme.colors.headline) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Affichage & Retours sensoriels",
                            style = MaterialTheme.typography.titleMedium,
                            color = YfuaTheme.colors.headline
                        )

                        SettingsSwitchRow(
                            icon = Icons.Filled.DarkMode,
                            iconTint = MaterialTheme.colorScheme.secondary,
                            title = "Mode sombre",
                            subtitle = if (darkModeEnabled) "Palette nocturne activée" else "Palette institutionnelle claire",
                            checked = darkModeEnabled,
                            onCheckedChange = onToggleDarkMode,
                            testTag = "settings_dark_mode_switch"
                        )

                        HorizontalDivider(color = YfuaTheme.colors.cardBorder)

                        SettingsSwitchRow(
                            icon = Icons.Filled.VolumeUp,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Effets sonores",
                            subtitle = "Signal audio en fin de cycle Pomodoro",
                            checked = soundEnabled,
                            onCheckedChange = onToggleSound,
                            testTag = "settings_sound_switch"
                        )

                        HorizontalDivider(color = YfuaTheme.colors.cardBorder)

                        SettingsSwitchRow(
                            icon = Icons.Filled.Vibration,
                            iconTint = MaterialTheme.colorScheme.tertiary,
                            title = "Vibrations haptiques",
                            subtitle = "Retours tactiles sur les cartes et minuteurs",
                            checked = hapticsEnabled,
                            onCheckedChange = onToggleHaptics,
                            testTag = "settings_haptics_switch"
                        )
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Notifications & Rappels",
                            style = MaterialTheme.typography.titleMedium,
                            color = YfuaTheme.colors.headline
                        )

                        SettingsSwitchRow(
                            icon = Icons.Filled.Notifications,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Rappel d'étude quotidien",
                            subtitle = if (dailyReminderEnabled) "Programmé tous les jours à $formattedTime" else "Désactivé",
                            checked = dailyReminderEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !NotificationHelper.hasNotificationPermission(context)
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onToggleDailyReminder(true, dailyReminderTime.first, dailyReminderTime.second)
                                        StudyReminderWorker.scheduleDaily(context, dailyReminderTime.first, dailyReminderTime.second)
                                        onShowMessage("Rappel quotidien activé à $formattedTime.")
                                    }
                                } else {
                                    onToggleDailyReminder(false, dailyReminderTime.first, dailyReminderTime.second)
                                    StudyReminderWorker.cancelDaily(context)
                                    onShowMessage("Rappel quotidien désactivé.")
                                }
                            },
                            testTag = "settings_reminder_switch"
                        )

                        HorizontalDivider(color = YfuaTheme.colors.cardBorder)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePickerDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("settings_reminder_time_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Heure du rappel", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline)
                                    Text(
                                        "Appuyez pour modifier ($formattedTime)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = YfuaTheme.colors.bodySlate
                                    )
                                }
                            }
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Exportation & Cache local",
                            style = MaterialTheme.typography.titleMedium,
                            color = YfuaTheme.colors.headline
                        )

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val json = onExportJson()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Sauvegarde JSON YFUA")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Exporter en JSON"))
                                }
                            },
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_export_json_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporter JSON complet", color = YfuaTheme.colors.headline)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val csv = onExportCsv()
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_SUBJECT, "Suivi YFUA (CSV)")
                                        putExtra(Intent.EXTRA_TEXT, csv)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Exporter en CSV"))
                                }
                            },
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_export_csv_button")
                        ) {
                            Icon(Icons.Filled.TableView, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exporter CSV (Tâches & Pomodoro)", color = YfuaTheme.colors.headline)
                        }

                        OutlinedButton(
                            onClick = { showClearCacheConfirm = true },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("settings_clear_cache_button")
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vider le cache local")
                        }
                    }
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "YFUA • Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.titleSmall,
                                color = YfuaTheme.colors.headline,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                DEFAULT_INSTITUTION,
                                style = MaterialTheme.typography.bodySmall,
                                color = YfuaTheme.colors.bodySlate
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = dailyReminderTime.first,
            initialMinute = dailyReminderTime.second,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Heure du rappel quotidien", color = YfuaTheme.colors.headline) },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = timePickerState.hour
                        val m = timePickerState.minute
                        onToggleDailyReminder(dailyReminderEnabled, h, m)
                        if (dailyReminderEnabled) {
                            StudyReminderWorker.scheduleDaily(context, h, m)
                        }
                        val label = String.format(Locale.FRANCE, "%02dh%02d", h, m)
                        onShowMessage("Heure du rappel enregistrée : $label.")
                        showTimePickerDialog = false
                    }
                ) {
                    Text("Valider", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Annuler", color = YfuaTheme.colors.bodySlate)
                }
            }
        )
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Vider le cache local ?", color = YfuaTheme.colors.headline) },
            text = {
                Text(
                    "Supprime les communiqués, emplois du temps officiels et journaux de synchronisation en cache. Vos tâches personnelles et cartes restent intactes.",
                    color = YfuaTheme.colors.bodySlate
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearCache()
                        showClearCacheConfirm = false
                    }
                ) {
                    Text("Vider", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Annuler", color = YfuaTheme.colors.bodySlate)
                }
            }
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = iconTint)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
