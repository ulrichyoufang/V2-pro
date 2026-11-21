package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aistudio.examyfua.k7m2px.data.remote.AuthHelper
import com.aistudio.examyfua.k7m2px.data.remote.configureAppCheck
import com.aistudio.examyfua.k7m2px.data.repository.AppContainer
import com.aistudio.examyfua.k7m2px.ui.screens.AuthScreen
import com.aistudio.examyfua.k7m2px.ui.screens.YfuaAppShell
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import com.aistudio.examyfua.k7m2px.ui.viewmodel.MainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        val container = AppContainer(applicationContext)
        setContent {
            val darkModeEnabled by container.preferences.darkModeEnabled
                .collectAsStateWithLifecycle(initialValue = false)
            YfuaTheme(darkTheme = darkModeEnabled) {
                YfuaRootApp(container = container)
            }
        }
    }
}

@Composable
fun YfuaRootApp(container: AppContainer) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    val offlineModeEntered by container.preferences.offlineModeEntered
        .collectAsStateWithLifecycle(initialValue = false)
    val institutions by container.repository.activeInstitutions
        .collectAsStateWithLifecycle(initialValue = emptyList())

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        container.repository.ensureDefaultInstitution()
        if (Firebase.auth.currentUser == null && !offlineModeEntered) {
            AuthHelper.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                scope = scope,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser },
                onUnauthenticated = {}
            )
        }
    }

    if (currentUser == null && !offlineModeEntered) {
        AuthScreen(
            institutions = institutions,
            onGoogleAuthSuccess = { currentUser = Firebase.auth.currentUser },
            onEmailRegistered = { fullName, phoneNumber, institution, specialty, level ->
                val user = Firebase.auth.currentUser
                scope.launch {
                    container.repository.saveProfile(
                        fullName = fullName,
                        email = user?.email.orEmpty(),
                        phoneNumber = phoneNumber,
                        photoUri = null,
                        institution = institution,
                        specialty = specialty,
                        level = level,
                        remoteUid = user?.uid
                    )
                }
                currentUser = user
            },
            onContinueOffline = {
                scope.launch { container.preferences.setOfflineModeEntered(true) }
            }
        )
    } else {
        val uid = currentUser?.uid
        val sessionKey = uid ?: "offline_local"
        key(sessionKey) {
            val vm: MainViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        MainViewModel(
                            repository = container.repository,
                            firestoreService = container.firestoreService,
                            syncManager = container.syncManager,
                            networkMonitor = container.networkMonitor,
                            preferences = container.preferences,
                            driveClient = container.driveBackupClient,
                            aiAssistantRepository = container.aiAssistantRepository,
                            currentUid = uid,
                            currentEmail = currentUser?.email,
                            currentDisplayName = currentUser?.displayName,
                            currentEmailVerified = currentUser?.isEmailVerified == true
                        )
                    }
                }
            )
            YfuaAppShell(
                viewModel = vm,
                currentUid = uid,
                currentEmail = currentUser?.email,
                driveClient = container.driveBackupClient,
                onRequestSwitchToAuth = {
                    scope.launch { container.preferences.setOfflineModeEntered(false) }
                },
                onSignedOut = {
                    scope.launch { container.preferences.setOfflineModeEntered(false) }
                    currentUser = null
                }
            )
        }
    }
}
