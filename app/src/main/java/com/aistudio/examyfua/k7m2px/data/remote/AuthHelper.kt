package com.aistudio.examyfua.k7m2px.data.remote

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object AuthHelper {
    private const val TAG = "Auth"
    private const val AUTH_TIMEOUT_MS = 10_000L

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }
        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onUnauthenticated()
            return
        }
        if (clientId.isBlank() || clientId == "fallback-offline-client-id") {
            onUnauthenticated()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                    val cred = credentialManager.getCredential(context, request).credential
                    if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                        true
                    } else {
                        false
                    }
                }
                if (result == true) onAuthSuccess() else onUnauthenticated()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    fun onGoogleSignInClicked(
        activity: Activity,
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        onAuthCancelled: () -> Unit
    ) {
        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (_: Exception) {
            onAuthError("Configuration Google Sign-In manquante.")
            return
        }
        if (clientId.isBlank() || clientId == "fallback-offline-client-id") {
            onAuthError("Configuration Google Sign-In absente. Continuez en mode hors-ligne.")
            return
        }

        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        scope.launch {
            try {
                val completed = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                    val cred = credentialManager.getCredential(activity, request).credential
                    if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                        Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                        true
                    } else {
                        onAuthError("Type d'identifiant Google inattendu.")
                        false
                    }
                }
                if (completed == null) {
                    onAuthError("Délai d'attente dépassé (10 s). Vérifiez votre connexion ou utilisez le mode hors-ligne.")
                } else if (completed) {
                    onAuthSuccess()
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(
                    e.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Échec de la connexion Google (erreur réseau ou empreinte SHA-1)."
                )
            }
        }
    }

    fun registerWithEmailAndPassword(
        email: String,
        password: String,
        fullName: String,
        scope: CoroutineScope,
        onSuccess: (uid: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            onError("Veuillez saisir une adresse e-mail valide.")
            return
        }
        if (password.length < 6) {
            onError("Le mot de passe doit contenir au moins 6 caractères.")
            return
        }
        if (fullName.trim().length < 2) {
            onError("Veuillez saisir votre nom complet.")
            return
        }

        scope.launch {
            try {
                val result = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                    val authResult = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    val user = authResult.user
                    if (user != null) {
                        user.updateProfile(
                            UserProfileChangeRequest.Builder()
                                .setDisplayName(fullName.trim())
                                .build()
                        ).await()
                    }
                    user?.uid
                }
                if (result == null) {
                    onError("Délai d'attente dépassé (10 s) lors de l'inscription.")
                } else {
                    onSuccess(result)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthException) {
                val msg = when (e.errorCode) {
                    "ERROR_OPERATION_NOT_ALLOWED" ->
                        "Inscription par e-mail non activée dans la console Firebase. Utilisez Google Sign-In ou activez le fournisseur E-mail/Mot de passe."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Un compte existe déjà avec cette adresse e-mail."
                    "ERROR_WEAK_PASSWORD" -> "Le mot de passe est trop faible (6 caractères minimum)."
                    "ERROR_INVALID_EMAIL" -> "Format d'adresse e-mail invalide."
                    else -> e.localizedMessage ?: "Échec de la création du compte."
                }
                onError(msg)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erreur réseau lors de la création du compte.")
            }
        }
    }

    fun sendPasswordReset(
        email: String,
        scope: CoroutineScope,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            onError("Saisissez une adresse e-mail valide.")
            return
        }
        scope.launch {
            try {
                val completed = withTimeoutOrNull(AUTH_TIMEOUT_MS) {
                    Firebase.auth.sendPasswordResetEmail(cleanEmail).await()
                    true
                }
                if (completed == true) {
                    onSuccess("Un e-mail de réinitialisation a été envoyé à $cleanEmail.")
                } else {
                    onError("Délai d'attente dépassé (10 s). Vérifiez votre connexion.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthException) {
                val msg = when (e.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "Aucun compte trouvé pour cette adresse e-mail."
                    "ERROR_INVALID_EMAIL" -> "Adresse e-mail invalide."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "Activez le fournisseur E-mail/Mot de passe dans la console Firebase."
                    else -> e.localizedMessage ?: "Impossible d'envoyer l'e-mail de réinitialisation."
                }
                onError(msg)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erreur réseau lors de l'envoi.")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
