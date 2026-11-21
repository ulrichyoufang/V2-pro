package com.aistudio.examyfua.k7m2px.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aistudio.examyfua.k7m2px.data.local.InstitutionEntity
import com.aistudio.examyfua.k7m2px.domain.model.AcademicLevel
import com.aistudio.examyfua.k7m2px.domain.model.AcademicSpecialty
import com.aistudio.examyfua.k7m2px.domain.model.DEFAULT_INSTITUTION
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupBottomSheet(
    initialProfile: StudentProfile?,
    institutions: List<InstitutionEntity>,
    isMandatory: Boolean,
    onSave: (
        fullName: String,
        phoneNumber: String,
        photoUri: String,
        institution: String,
        specialty: AcademicSpecialty,
        level: AcademicLevel
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val institutionOptions = remember(institutions) {
        val names = institutions.filter { it.isActive }.map { it.name }.distinct()
        if (names.isEmpty()) listOf(DEFAULT_INSTITUTION) else names
    }

    var fullName by remember(initialProfile) { mutableStateOf(initialProfile?.fullName ?: "") }
    var phoneNumber by remember(initialProfile) { mutableStateOf(initialProfile?.phoneNumber ?: "") }
    var photoPath by remember(initialProfile) { mutableStateOf(initialProfile?.photoUri ?: "") }
    var selectedInstitution by remember(initialProfile, institutionOptions) {
        mutableStateOf(
            initialProfile?.institution?.takeIf { it.isNotBlank() }
                ?: institutionOptions.firstOrNull()
                ?: DEFAULT_INSTITUTION
        )
    }
    var selectedSpecialty by remember(initialProfile) {
        mutableStateOf(initialProfile?.specialty ?: AcademicSpecialty.MECATRONIQUE)
    }
    var selectedLevel by remember(initialProfile) {
        mutableStateOf(initialProfile?.level ?: AcademicLevel.BTS_1)
    }
    var institutionExpanded by remember { mutableStateOf(false) }
    var specialtyExpanded by remember { mutableStateOf(false) }
    var levelExpanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            photoPath = uri.toString()
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = YfuaTheme.colors.headline,
        unfocusedTextColor = YfuaTheme.colors.headline,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = YfuaTheme.colors.cardBorder,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = YfuaTheme.colors.bodySlate,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    ModalBottomSheet(
        onDismissRequest = { if (!isMandatory) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = YfuaTheme.colors.headline
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isMandatory) "Profil académique" else "Modifier le profil",
                style = MaterialTheme.typography.headlineSmall,
                color = YfuaTheme.colors.headline
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StudentAvatar(
                    initials = initialProfile?.initials ?: fullName.take(2).ifBlank { "YF" },
                    size = 64.dp,
                    photoPath = photoPath.takeIf { it.isNotBlank() }
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("profile_pick_photo_button")
                    ) {
                        Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (photoPath.isBlank()) "Choisir une photo" else "Changer la photo")
                    }
                    if (photoPath.isNotBlank()) {
                        TextButton(
                            onClick = { photoPath = "" },
                            modifier = Modifier.height(36.dp).testTag("profile_clear_photo_button")
                        ) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retirer la photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = {
                    fullName = it
                    validationError = null
                },
                label = { Text("Nom complet *") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_fullname_input")
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                    if (input.length <= 32) {
                        phoneNumber = input
                        validationError = null
                    }
                },
                label = { Text("Téléphone (optionnel, sans vérification)") },
                placeholder = { Text("Ex. +237 6XX XXX XXX") },
                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_phone_input")
            )

            ExposedDropdownMenuBox(
                expanded = institutionExpanded,
                onExpandedChange = { institutionExpanded = !institutionExpanded }
            ) {
                OutlinedTextField(
                    value = selectedInstitution,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Établissement *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = institutionExpanded) },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("profile_institution_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = institutionExpanded,
                    onDismissRequest = { institutionExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    institutionOptions.forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name, color = YfuaTheme.colors.headline) },
                            onClick = {
                                selectedInstitution = name
                                institutionExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = specialtyExpanded,
                onExpandedChange = { specialtyExpanded = !specialtyExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSpecialty.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Filière académique *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = specialtyExpanded) },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("profile_specialty_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = specialtyExpanded,
                    onDismissRequest = { specialtyExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AcademicSpecialty.entries.forEach { specialty ->
                        DropdownMenuItem(
                            text = { Text(specialty.displayName, color = YfuaTheme.colors.headline) },
                            onClick = {
                                selectedSpecialty = specialty
                                specialtyExpanded = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = !levelExpanded }
            ) {
                OutlinedTextField(
                    value = selectedLevel.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Niveau académique *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    colors = fieldColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("profile_level_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AcademicLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.displayName, color = YfuaTheme.colors.headline) },
                            onClick = {
                                selectedLevel = level
                                levelExpanded = false
                            }
                        )
                    }
                }
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (fullName.trim().length < 2) {
                        validationError = "Veuillez saisir votre nom complet (au moins 2 caractères)."
                    } else {
                        onSave(
                            fullName.trim(),
                            phoneNumber.trim(),
                            photoPath,
                            selectedInstitution.ifBlank { DEFAULT_INSTITUTION },
                            selectedSpecialty,
                            selectedLevel
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_profile_button")
            ) {
                Text("Enregistrer", style = MaterialTheme.typography.labelLarge)
            }

            if (!isMandatory) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Annuler", color = YfuaTheme.colors.bodySlate)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
