package com.aistudio.examyfua.k7m2px.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.aistudio.examyfua.k7m2px.data.remote.AuthHelper
import com.aistudio.examyfua.k7m2px.data.remote.GoogleDriveBackupClient
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.ui.components.StudentAvatar
import com.aistudio.examyfua.k7m2px.ui.theme.DeepPetrolTeal
import com.aistudio.examyfua.k7m2px.ui.theme.HeadlineLight
import com.aistudio.examyfua.k7m2px.ui.theme.PureWhite
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.example.R
import com.google.android.gms.auth.api.identity.Identity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    profile: StudentProfile?,
    currentUid: String?,
    currentEmail: String?,
    isOnline: Boolean,
    pendingDirtyCount: Int,
    lastSyncEpochMs: Long,
    lastDriveBackupEpochMs: Long,
    isDriveBusy: Boolean,
    isSuperAdmin: Boolean,
    driveClient: GoogleDriveBackupClient,
    onEditProfile: () -> Unit,
    onUpdateProfilePhoto: (Uri?) -> Unit,
    onTriggerSync: () -> Unit,
    onOpenSyncLogs: () -> Unit,
    onUploadDriveBackup: (String) -> Unit,
    onRestoreDriveBackup: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSupport: () -> Unit,
    onAccountLinked: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    var pendingDriveAction by remember { mutableStateOf<String?>(null) }
    var isLinkingGoogle by remember { mutableStateOf(false) }

    val avatarPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfilePhoto(uri)
        }
    }

    val driveAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
                val token = authResult.accessToken
                if (!token.isNullOrBlank()) {
                    if (pendingDriveAction == "UPLOAD") onUploadDriveBackup(token)
                    else if (pendingDriveAction == "RESTORE") onRestoreDriveBackup(token)
                } else {
                    onShowMessage("Jeton Google Drive introuvable.")
                }
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Échec d'autorisation Google Drive.")
            }
        } else {
            onShowMessage("Autorisation Google Drive annulée.")
        }
        pendingDriveAction = null
    }

    fun launchDriveAction(actionType: String) {
        if (activity == null) return
        pendingDriveAction = actionType
        Identity.getAuthorizationClient(activity)
            .authorize(driveClient.buildAuthorizationRequest())
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    val intentSender = authResult.pendingIntent?.intentSender
                    if (intentSender != null) {
                        driveAuthLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                } else {
                    val token = authResult.accessToken
                    if (!token.isNullOrBlank()) {
                        if (actionType == "UPLOAD") onUploadDriveBackup(token)
                        else onRestoreDriveBackup(token)
                    } else {
                        onShowMessage("Jeton d'accès Google Drive indisponible.")
                    }
                    pendingDriveAction = null
                }
            }
            .addOnFailureListener { e ->
                pendingDriveAction = null
                onShowMessage(e.localizedMessage ?: "Google Drive indisponible sur cet appareil.")
            }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header_logo") {
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_iup),
                        contentDescription = "Logo Institut Universitaire de la Pointe",
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "YFUA • Espace Académique",
                            style = MaterialTheme.typography.titleMedium,
                            color = HeadlineLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = profile?.institution?.ifBlank { DEFAULT_INSTITUTION } ?: DEFAULT_INSTITUTION,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepPetrolTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item(key = "identity_card") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clickable {
                                    avatarPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_picker_trigger")
                        ) {
                            StudentAvatar(
                                initials = profile?.initials ?: "YF",
                                size = 64.dp,
                                photoPath = profile?.photoUri?.takeIf { it.isNotBlank() }
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.align(Alignment.BottomEnd)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Changer la photo de profil",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(4.dp).size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.fullName?.ifBlank { "Étudiant YFUA" } ?: "Étudiant YFUA",
                                style = MaterialTheme.typography.titleLarge,
                                color = YfuaTheme.colors.headline
                            )
                            Text(
                                text = currentEmail?.takeIf { it.isNotBlank() } ?: "Mode Hors-Ligne",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentUid != null) YfuaTheme.colors.success else YfuaTheme.colors.warning
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = YfuaTheme.colors.bodySlate,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = profile?.phoneNumber?.takeIf { it.isNotBlank() }
                                        ?: "Téléphone non renseigné (sans vérification)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YfuaTheme.colors.bodySlate
                                )
                            }
                        }
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier.testTag("profile_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifier le profil", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Filière : ${profile?.specialty?.displayName ?: "Mécatronique"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Niveau : ${profile?.level?.displayName ?: "Brevet de Technicien Supérieur - Niveau 1"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        if (isSuperAdmin) {
            item(key = "super_admin_entry") {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Espace Super-Administrateur", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer, fontWeight = FontWeight.Bold)
                                Text("KPIs, IA/API, emplois du temps, instituts & support", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        Button(
                            onClick = onOpenAdminPanel,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor = MaterialTheme.colorScheme.onTertiary
                            ),
                            modifier = Modifier.testTag("open_super_admin_button")
                        ) {
                            Text("Ouvrir")
                        }
                    }
                }
            }
        }

        item(key = "navigation_shortcuts") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSettings)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("profile_open_settings"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Paramètres", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline, fontWeight = FontWeight.SemiBold)
                            Text("Thème, sons, haptique, rappels, exports JSON/CSV", style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = YfuaTheme.colors.bodySlate)
                    }

                    HorizontalDivider(color = YfuaTheme.colors.cardBorder)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenSupport)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("profile_open_support"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Support & Aide", style = MaterialTheme.typography.titleSmall, color = YfuaTheme.colors.headline, fontWeight = FontWeight.SemiBold)
                            Text("Contacter l'administration et suivre vos demandes", style = MaterialTheme.typography.bodySmall, color = YfuaTheme.colors.bodySlate)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = YfuaTheme.colors.bodySlate)
                    }
                }
            }
        }

        item(key = "sync_and_drive") {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Synchronisation & Sauvegarde (€0)", style = MaterialTheme.typography.titleMedium, color = YfuaTheme.colors.headline)
                    Text(
                        text = if (lastSyncEpochMs > 0L) {
                            "Dernière sync : ${dateFormat.format(Date(lastSyncEpochMs))} • $pendingDirtyCount en attente"
                        } else {
                            "$pendingDirtyCount élément(s) locaux en attente"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = YfuaTheme.colors.bodySlate
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onTriggerSync,
                            enabled = currentUid != null && isOnline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f).testTag("profile_sync_now_button")
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchroniser")
                        }
                        OutlinedButton(
                            onClick = onOpenSyncLogs,
                            border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Journal", color = YfuaTheme.colors.headline)
                        }
                    }

                    Text(
                        text = if (lastDriveBackupEpochMs > 0L) {
                            "Google Drive AppData : ${dateFormat.format(Date(lastDriveBackupEpochMs))}"
                        } else {
                            "Google Drive AppData : aucune sauvegarde"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = YfuaTheme.colors.bodySlate
                    )

                    if (isDriveBusy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Opération Google Drive en cours…", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { launchDriveAction("UPLOAD") },
                            enabled = !isDriveBusy && isOnline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.weight(1f).testTag("drive_backup_now_button")
                        ) {
                            Icon(Icons.Filled.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sauvegarder")
                        }
                        OutlinedButton(
                            onClick = { launchDriveAction("RESTORE") },
                            enabled = !isDriveBusy && isOnline,
                            border = BorderStroke(1.dp, YfuaTheme.colors.success),
                            modifier = Modifier.weight(1f).testTag("drive_restore_button")
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = YfuaTheme.colors.success, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restaurer", color = YfuaTheme.colors.success)
                        }
                    }
                }
            }
        }

        item(key = "account_actions") {
            if (currentUid == null) {
                Button(
                    onClick = {
                        if (activity == null) return@Button
                        isLinkingGoogle = true
                        AuthHelper.onGoogleSignInClicked(
                            activity = activity,
                            credentialManager = credentialManager,
                            scope = scope,
                            onAuthSuccess = {
                                isLinkingGoogle = false
                                onAccountLinked()
                            },
                            onAuthError = { msg ->
                                isLinkingGoogle = false
                                onShowMessage(msg)
                            },
                            onAuthCancelled = {
                                isLinkingGoogle = false
                                onShowMessage("Liaison Google annulée.")
                            }
                        )
                    },
                    enabled = !isLinkingGoogle,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("link_google_account_button")
                ) {
                    Text(if (isLinkingGoogle) "Connexion en cours…" else "Lier un compte Google", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = {
                        AuthHelper.signOut(credentialManager, scope) {
                            onSignedOut()
                        }
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Se déconnecter", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
