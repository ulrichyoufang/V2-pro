package com.aistudio.examyfua.k7m2px.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.domain.model.AdminAiSecretsState
import com.aistudio.examyfua.k7m2px.domain.model.AiGeminiModel
import com.aistudio.examyfua.k7m2px.domain.model.AiProviderMode
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AdminAiApiTab(
    appConfig: AppConfigState,
    adminSecrets: AdminAiSecretsState,
    onToggleAiEnabled: (Boolean) -> Unit,
    onUpdateAppConfig: (AppConfigState) -> Unit,
    onUpdateAiSecrets: (AdminAiSecretsState) -> Unit
) {
    val fieldColors = adminFieldColors()

    var aiEnabled by remember(appConfig.aiEnabled) { mutableStateOf(appConfig.aiEnabled) }
    var aiProvider by remember(appConfig.aiProvider) { mutableStateOf(appConfig.aiProvider) }
    var aiModelName by remember(appConfig.aiModelName) { mutableStateOf(appConfig.aiModelName) }
    var aiSystemPrompt by remember(appConfig.aiSystemPrompt) { mutableStateOf(appConfig.aiSystemPrompt) }
    var aiTemperature by remember(appConfig.aiTemperature) { mutableFloatStateOf(appConfig.aiTemperature.toFloat()) }
    var aiDailyLimit by remember(appConfig.aiDailyLimit) { mutableIntStateOf(appConfig.aiDailyLimit) }
    var customApiBaseUrl by remember(appConfig.customApiBaseUrl) { mutableStateOf(appConfig.customApiBaseUrl) }
    var customApiModel by remember(appConfig.customApiModel) { mutableStateOf(appConfig.customApiModel) }

    var customAiApiKey by remember(adminSecrets.customAiApiKey) { mutableStateOf(adminSecrets.customAiApiKey) }
    var platformWebhookUrl by remember(adminSecrets.platformWebhookUrl) { mutableStateOf(adminSecrets.platformWebhookUrl) }
    var platformApiSecret by remember(adminSecrets.platformApiSecret) { mutableStateOf(adminSecrets.platformApiSecret) }
    var extraApiHeadersJson by remember(adminSecrets.extraApiHeadersJson) { mutableStateOf(adminSecrets.extraApiHeadersJson) }

    var showCustomKey by remember { mutableStateOf(false) }
    var showPlatformSecret by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "admin_ai_master_toggle") {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Assistant IA intégré",
                                style = MaterialTheme.typography.titleMedium,
                                color = YfuaTheme.colors.headline,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (aiEnabled) "Activé pour tous les étudiants" else "Désactivé sur toute la plateforme",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (aiEnabled) YfuaTheme.colors.success else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Switch(
                        checked = aiEnabled,
                        onCheckedChange = {
                            aiEnabled = it
                            onToggleAiEnabled(it)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = YfuaTheme.colors.success),
                        modifier = Modifier.testTag("admin_ai_enable_switch")
                    )
                }
            }
        }

        item(key = "admin_ai_engine_config") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Moteur IA & Paramètres modèle",
                        style = MaterialTheme.typography.titleMedium,
                        color = YfuaTheme.colors.headline,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiProviderMode.entries.forEach { mode ->
                            FilterChip(
                                selected = aiProvider == mode,
                                onClick = { aiProvider = mode },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    if (aiProvider == AiProviderMode.FIREBASE_AI_LOGIC) {
                        Text("Modèle Gemini actif", style = MaterialTheme.typography.labelMedium, color = YfuaTheme.colors.bodySlate)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiGeminiModel.entries.forEach { model ->
                                FilterChip(
                                    selected = aiModelName == model.modelId,
                                    onClick = { aiModelName = model.modelId },
                                    label = { Text(model.label) }
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = customApiBaseUrl,
                            onValueChange = { customApiBaseUrl = it },
                            label = { Text("URL Endpoint API personnalisé (HTTPS)") },
                            placeholder = { Text("https://api.votre-plateforme.com/v1/chat/completions") },
                            colors = fieldColors,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("admin_custom_api_url_input")
                        )
                        OutlinedTextField(
                            value = customApiModel,
                            onValueChange = { customApiModel = it },
                            label = { Text("Nom du modèle distant") },
                            placeholder = { Text("Ex. gpt-4o-mini ou mistral-large") },
                            colors = fieldColors,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = aiSystemPrompt,
                        onValueChange = { aiSystemPrompt = it },
                        label = { Text("Prompt système académique") },
                        minLines = 3,
                        maxLines = 6,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("admin_ai_system_prompt_input")
                    )

                    Column {
                        Text(
                            text = String.format(Locale.FRANCE, "Température : %.1f", aiTemperature),
                            style = MaterialTheme.typography.bodySmall,
                            color = YfuaTheme.colors.headline
                        )
                        Slider(
                            value = aiTemperature,
                            onValueChange = { aiTemperature = (it * 10f).roundToInt() / 10f },
                            valueRange = 0.0f..1.5f
                        )
                    }

                    Column {
                        Text(
                            text = "Quota journalier étudiant : $aiDailyLimit requête(s)/jour",
                            style = MaterialTheme.typography.bodySmall,
                            color = YfuaTheme.colors.headline
                        )
                        Slider(
                            value = aiDailyLimit.toFloat(),
                            onValueChange = { aiDailyLimit = it.roundToInt().coerceIn(5, 200) },
                            valueRange = 5f..200f
                        )
                    }

                    Button(
                        onClick = {
                            onUpdateAppConfig(
                                appConfig.copy(
                                    aiEnabled = aiEnabled,
                                    aiProvider = aiProvider,
                                    aiModelName = aiModelName,
                                    aiSystemPrompt = aiSystemPrompt.trim(),
                                    aiTemperature = aiTemperature.toDouble(),
                                    aiDailyLimit = aiDailyLimit,
                                    customApiBaseUrl = customApiBaseUrl.trim(),
                                    customApiModel = customApiModel.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_ai_config_button")
                    ) {
                        Text("Enregistrer la configuration IA", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item(key = "admin_api_secrets_panel") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Key, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clés API & Webhooks plateforme",
                                style = MaterialTheme.typography.titleMedium,
                                color = YfuaTheme.colors.headline,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (appConfig.hasCustomApiKey) {
                                    "Stockage isolé Super-Admin • Clé personnalisée active"
                                } else {
                                    "Stockage isolé Super-Admin"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customAiApiKey,
                        onValueChange = { customAiApiKey = it },
                        label = { Text("Clé API IA personnalisée (Bearer Token)") },
                        singleLine = true,
                        visualTransformation = if (showCustomKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showCustomKey = !showCustomKey }) {
                                Icon(
                                    imageVector = if (showCustomKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showCustomKey) "Masquer" else "Afficher"
                                )
                            }
                        },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().testTag("admin_custom_api_key_input")
                    )

                    OutlinedTextField(
                        value = platformWebhookUrl,
                        onValueChange = { platformWebhookUrl = it },
                        label = { Text("URL Webhook plateforme") },
                        placeholder = { Text("https://hooks.votre-plateforme.com/yfua") },
                        singleLine = true,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = platformApiSecret,
                        onValueChange = { platformApiSecret = it },
                        label = { Text("Secret API plateforme") },
                        singleLine = true,
                        visualTransformation = if (showPlatformSecret) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPlatformSecret = !showPlatformSecret }) {
                                Icon(
                                    imageVector = if (showPlatformSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPlatformSecret) "Masquer" else "Afficher"
                                )
                            }
                        },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = extraApiHeadersJson,
                        onValueChange = { extraApiHeadersJson = it },
                        label = { Text("En-têtes HTTP additionnels (JSON)") },
                        placeholder = { Text("{\"X-Org-Id\": \"iup-main\"}") },
                        minLines = 2,
                        maxLines = 4,
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onUpdateAiSecrets(
                                AdminAiSecretsState(
                                    customAiApiKey = customAiApiKey.trim(),
                                    platformWebhookUrl = platformWebhookUrl.trim(),
                                    platformApiSecret = platformApiSecret.trim(),
                                    extraApiHeadersJson = extraApiHeadersJson.trim()
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_save_ai_secrets_button")
                    ) {
                        Text("Enregistrer les clés & API", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
