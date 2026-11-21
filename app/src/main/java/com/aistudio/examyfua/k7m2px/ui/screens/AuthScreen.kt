package com.aistudio.examyfua.k7m2px.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.data.remote.AuthHelper
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.ui.theme.DeepPetrolTeal
import com.aistudio.examyfua.k7m2px.ui.theme.HeadlineLight
import com.aistudio.examyfua.k7m2px.ui.theme.PureWhite
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.example.R

private enum class AuthTabMode(val label: String) {
    CONNEXION("Connexion"),
    INSCRIPTION("Créer un compte"),
    MOT_DE_PASSE_OUBLIE("Mot de passe oublié")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    institutions: List<InstitutionEntity>,
    onGoogleAuthSuccess: () -> Unit,
    onEmailRegistered: (
        fullName: String,
        phoneNumber: String,
        institution: String,
        specialty: AcademicSpecialty,
        level: AcademicLevel
    ) -> Unit,
    onContinueOffline: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val institutionOptions = remember(institutions) {
        val list = institutions.filter { it.isActive }.map { it.name }.distinct()
        if (list.isEmpty()) listOf(DEFAULT_INSTITUTION) else list
    }

    var mode by remember { mutableStateOf(AuthTabMode.CONNEXION) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedInstitution by remember(institutionOptions) {
        mutableStateOf(institutionOptions.firstOrNull() ?: DEFAULT_INSTITUTION)
    }
    var selectedLevel by remember { mutableStateOf(AcademicLevel.BTS_1) }
    var selectedSpecialty by remember { mutableStateOf(AcademicSpecialty.MECATRONIQUE) }

    var instExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }
    var specExpanded by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isFeedbackError by remember { mutableStateOf(true) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = YfuaTheme.colors.headline,
        unfocusedTextColor = YfuaTheme.colors.headline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = YfuaTheme.colors.cardBorder,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_iup),
                        contentDescription = "Logo Institut Universitaire de la Pointe",
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "YFUA",
                            style = MaterialTheme.typography.headlineMedium,
                            color = HeadlineLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = DEFAULT_INSTITUTION,
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepPetrolTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, YfuaTheme.colors.cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (mode != AuthTabMode.MOT_DE_PASSE_OUBLIE) {
                        val tabIndex = if (mode == AuthTabMode.CONNEXION) 0 else 1
                        TabRow(
                            selectedTabIndex = tabIndex,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { positions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(positions[tabIndex]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            Tab(
                                selected = mode == AuthTabMode.CONNEXION,
                                onClick = { mode = AuthTabMode.CONNEXION; feedbackMessage = null },
                                text = { Text("Connexion") },
                                modifier = Modifier.testTag("auth_tab_login")
                            )
                            Tab(
                                selected = mode == AuthTabMode.INSCRIPTION,
                                onClick = { mode = AuthTabMode.INSCRIPTION; feedbackMessage = null },
                                text = { Text("Créer un compte") },
                                modifier = Modifier.testTag("auth_tab_register")
                            )
                        }
                    }

                    if (feedbackMessage != null) {
                        Surface(
                            color = if (isFeedbackError) MaterialTheme.colorScheme.errorContainer else YfuaTheme.colors.successContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isFeedbackError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else YfuaTheme.colors.success.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isFeedbackError) MaterialTheme.colorScheme.error else YfuaTheme.colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feedbackMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isFeedbackError) MaterialTheme.colorScheme.onErrorContainer else YfuaTheme.colors.onSuccessContainer
                                )
                            }
                        }
                    }

                    AnimatedContent(targetState = mode, label = "auth_mode_content") { currentMode ->
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            when (currentMode) {
                                AuthTabMode.CONNEXION -> {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it; feedbackMessage = null },
                                        label = { Text("Adresse e-mail") },
                                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input")
                                    )
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it; feedbackMessage = null },
                                        label = { Text("Mot de passe") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { mode = AuthTabMode.MOT_DE_PASSE_OUBLIE; feedbackMessage = null }) {
                                            Text("Mot de passe oublié ?", color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            if (email.isBlank() || password.length < 6) {
                                                isFeedbackError = true
                                                feedbackMessage = "Saisissez un e-mail valide et un mot de passe (6+ caractères)."
                                            } else {
                                                isFeedbackError = true
                                                feedbackMessage = "La connexion par mot de passe requiert l'activation manuelle du fournisseur E-mail/Mot de passe dans la console Firebase. Utilisez Google Sign-In ou le Mode Hors-Ligne."
                                            }
                                        },
                                        enabled = !isLoading,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_email_login_button")
                                    ) {
                                        Text("Se connecter par e-mail", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                AuthTabMode.INSCRIPTION -> {
                                    OutlinedTextField(
                                        value = fullName,
                                        onValueChange = { fullName = it; feedbackMessage = null },
                                        label = { Text("Nom complet *") },
                                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("register_fullname_input")
                                    )
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it; feedbackMessage = null },
                                        label = { Text("Adresse e-mail *") },
                                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("register_email_input")
                                    )
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it; feedbackMessage = null },
                                        label = { Text("Mot de passe (6+ car.) *") },
                                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                                        visualTransformation = PasswordVisualTransformation(),
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("register_password_input")
                                    )
                                    OutlinedTextField(
                                        value = phoneNumber,
                                        onValueChange = { if (it.length <= 32) { phoneNumber = it; feedbackMessage = null } },
                                        label = { Text("Téléphone (optionnel, sans vérification)") },
                                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth().testTag("register_phone_input")
                                    )

                                    ExposedDropdownMenuBox(expanded = instExpanded, onExpandedChange = { instExpanded = !instExpanded }) {
                                        OutlinedTextField(
                                            value = selectedInstitution,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Établissement") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = instExpanded) },
                                            colors = fieldColors,
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = instExpanded,
                                            onDismissRequest = { instExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            institutionOptions.forEach { inst ->
                                                DropdownMenuItem(
                                                    text = { Text(inst, color = YfuaTheme.colors.headline) },
                                                    onClick = { selectedInstitution = inst; instExpanded = false }
                                                )
                                            }
                                        }
                                    }

                                    ExposedDropdownMenuBox(expanded = levelExpanded, onExpandedChange = { levelExpanded = !levelExpanded }) {
                                        OutlinedTextField(
                                            value = selectedLevel.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Niveau académique") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                                            colors = fieldColors,
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = levelExpanded,
                                            onDismissRequest = { levelExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            AcademicLevel.entries.forEach { lvl ->
                                                DropdownMenuItem(
                                                    text = { Text(lvl.displayName, color = YfuaTheme.colors.headline) },
                                                    onClick = { selectedLevel = lvl; levelExpanded = false }
                                                )
                                            }
                                        }
                                    }

                                    ExposedDropdownMenuBox(expanded = specExpanded, onExpandedChange = { specExpanded = !specExpanded }) {
                                        OutlinedTextField(
                                            value = selectedSpecialty.displayName,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Filière académique") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specExpanded) },
                                            colors = fieldColors,
                                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        )
                                        ExposedDropdownMenu(
                                            expanded = specExpanded,
                                            onDismissRequest = { specExpanded = false },
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            AcademicSpecialty.entries.forEach { spec ->
                                                DropdownMenuItem(
                                                    text = { Text(spec.displayName, color = YfuaTheme.colors.headline) },
                                                    onClick = { selectedSpecialty = spec; specExpanded = false }
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            isLoading = true
                                            feedbackMessage = null
                                            AuthHelper.registerWithEmailAndPassword(
                                                email = email,
                                                password = password,
                                                fullName = fullName,
                                                scope = scope,
                                                onSuccess = {
                                                    isLoading = false
                                                    onEmailRegistered(
                                                        fullName.trim(),
                                                        phoneNumber.trim(),
                                                        selectedInstitution,
                                                        selectedSpecialty,
                                                        selectedLevel
                                                    )
                                                },
                                                onError = { err ->
                                                    isLoading = false
                                                    isFeedbackError = true
                                                    feedbackMessage = err
                                                }
                                            )
                                        },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("auth_register_submit_button")
                                    ) {
                                        Text("Créer mon compte", fontWeight = FontWeight.Bold)
                                    }
                                }

                                AuthTabMode.MOT_DE_PASSE_OUBLIE -> {
                                    Text(
                                        text = "Réinitialisation du mot de passe",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = YfuaTheme.colors.headline
                                    )
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it; feedbackMessage = null },
                                        label = { Text("Adresse e-mail") },
                                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                                        singleLine = true,
                                        colors = fieldColors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            feedbackMessage = null
                                            AuthHelper.sendPasswordReset(
                                                email = email,
                                                scope = scope,
                                                onSuccess = { msg ->
                                                    isLoading = false
                                                    isFeedbackError = false
                                                    feedbackMessage = msg
                                                },
                                                onError = { err ->
                                                    isLoading = false
                                                    isFeedbackError = true
                                                    feedbackMessage = err
                                                }
                                            )
                                        },
                                        enabled = !isLoading,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("Envoyer le lien", fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { mode = AuthTabMode.CONNEXION; feedbackMessage = null },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Retour à la connexion", color = YfuaTheme.colors.bodySlate)
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (activity == null) {
                                isFeedbackError = true
                                feedbackMessage = "Activité indisponible."
                                return@Button
                            }
                            isLoading = true
                            feedbackMessage = null
                            AuthHelper.onGoogleSignInClicked(
                                activity = activity,
                                credentialManager = credentialManager,
                                scope = scope,
                                onAuthSuccess = {
                                    isLoading = false
                                    onGoogleAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    isFeedbackError = true
                                    feedbackMessage = msg
                                },
                                onAuthCancelled = {
                                    isLoading = false
                                    isFeedbackError = true
                                    feedbackMessage = "Connexion Google annulée."
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Connexion…", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.School, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Continuer avec Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onContinueOffline,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("continue_offline_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continuer en Mode Hors-Ligne",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
