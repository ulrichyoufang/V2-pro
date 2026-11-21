package com.aistudio.examyfua.k7m2px.data.remote

import android.util.Log
import com.aistudio.examyfua.k7m2px.domain.model.AdminAiSecretsState
import com.aistudio.examyfua.k7m2px.domain.model.AiChatMessage
import com.aistudio.examyfua.k7m2px.domain.model.AiGeminiModel
import com.aistudio.examyfua.k7m2px.domain.model.AiProviderMode
import com.aistudio.examyfua.k7m2px.domain.model.AppConfigState
import com.aistudio.examyfua.k7m2px.domain.model.StudentProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiAssistantRepository {

    private val defaultModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel(modelName = AiGeminiModel.GEMINI_3_8_FLASH.modelId)
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private var cachedSignature: String = ""
    private var cachedModel: GenerativeModel? = null

    private fun resolveGeminiModel(
        config: AppConfigState,
        studentProfile: StudentProfile?
    ): GenerativeModel {
        val modelId = AiGeminiModel.safeFromId(config.aiModelName).modelId
        val instruction = buildSystemInstruction(config.aiSystemPrompt, studentProfile)
        val temp = config.aiTemperature.toFloat().coerceIn(0.0f, 2.0f)
        val signature = "$modelId|$temp|$instruction"
        if (signature == cachedSignature && cachedModel != null) {
            return cachedModel!!
        }
        if (modelId == AiGeminiModel.GEMINI_3_8_FLASH.modelId && temp == 0.7f && instruction.isBlank()) {
            return defaultModel
        }
        return Firebase.ai.generativeModel(
            modelName = modelId,
            generationConfig = generationConfig {
                temperature = temp
            },
            systemInstruction = content {
                text(instruction)
            }
        ).also {
            cachedSignature = signature
            cachedModel = it
        }
    }

    private fun buildSystemInstruction(
        adminPrompt: String,
        profile: StudentProfile?
    ): String {
        val studentContext = if (profile != null) {
            "Profil étudiant : ${profile.fullName.ifBlank { "Étudiant YFUA" }}, " +
                "Établissement : ${profile.institution}, " +
                "Niveau : ${profile.level.displayName}, " +
                "Filière : ${profile.specialty.displayName}."
        } else {
            "Contexte : Étudiant technologique YFUA."
        }
        return "${adminPrompt.trim()}\n\n$studentContext"
    }

    fun streamReply(
        prompt: String,
        history: List<AiChatMessage>,
        config: AppConfigState,
        adminSecrets: AdminAiSecretsState?,
        studentProfile: StudentProfile?
    ): Flow<String> = flow {
        if (!config.aiEnabled) {
            throw IllegalStateException("L'assistant IA est actuellement désactivé par l'administration.")
        }

        if (config.aiProvider == AiProviderMode.CUSTOM_PROXY && config.customApiBaseUrl.isNotBlank()) {
            val reply = executeCustomApiRequest(
                prompt = prompt,
                history = history,
                config = config,
                adminSecrets = adminSecrets,
                studentProfile = studentProfile
            )
            emit(reply)
            return@flow
        }

        val model = resolveGeminiModel(config, studentProfile)
        val recentHistory: List<Content> = history.takeLast(8).map { msg ->
            content(role = if (msg.isFromUser) "user" else "model") {
                text(msg.text)
            }
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            if (recentHistory.isEmpty()) {
                model.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            } else {
                val chat = model.startChat(history = recentHistory)
                chat.sendMessageStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Le modèle IA a renvoyé une réponse vide.")
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun executeCustomApiRequest(
        prompt: String,
        history: List<AiChatMessage>,
        config: AppConfigState,
        adminSecrets: AdminAiSecretsState?,
        studentProfile: StudentProfile?
    ): String {
        return withTimeout(60_000L) {
            val baseUrl = config.customApiBaseUrl.trim()
            if (!baseUrl.startsWith("https://") && !baseUrl.startsWith("http://")) {
                throw IllegalArgumentException("URL de l'API personnalisée invalide.")
            }

            val token = adminSecrets?.customAiApiKey?.takeIf { it.isNotBlank() }
                ?: Firebase.auth.currentUser?.getIdToken(false)?.await()?.token.orEmpty()

            val messagesArray = JSONArray().apply {
                put(
                    JSONObject()
                        .put("role", "system")
                        .put("content", buildSystemInstruction(config.aiSystemPrompt, studentProfile))
                )
                history.takeLast(6).forEach { msg ->
                    put(
                        JSONObject()
                            .put("role", if (msg.isFromUser) "user" else "assistant")
                            .put("content", msg.text)
                    )
                }
                put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
                )
            }

            val payload = JSONObject()
                .put("model", config.customApiModel.ifBlank { config.aiModelName })
                .put("temperature", config.aiTemperature.coerceIn(0.0, 2.0))
                .put("messages", messagesArray)

            val requestBuilder = Request.Builder()
                .url(baseUrl)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (token.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val extraHeadersRaw = adminSecrets?.extraApiHeadersJson?.trim().orEmpty()
            if (extraHeadersRaw.startsWith("{")) {
                runCatching {
                    val obj = JSONObject(extraHeadersRaw)
                    obj.keys().forEach { key ->
                        val v = obj.optString(key, "")
                        if (key.isNotBlank() && v.isNotBlank()) {
                            requestBuilder.header(key, v)
                        }
                    }
                }
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Custom API HTTP ${response.code}: $rawBody")
                    throw IllegalStateException("Erreur API personnalisée (HTTP ${response.code}).")
                }
                val parsed = parseCustomApiText(rawBody)
                if (parsed.isBlank()) {
                    throw IllegalStateException("Réponse vide de l'API personnalisée.")
                }
                parsed
            }
        }
    }

    private fun parseCustomApiText(rawJson: String): String {
        if (rawJson.isBlank()) return ""
        return try {
            val root = JSONObject(rawJson)
            val choices = root.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.optJSONObject(0)
                val msg = first?.optJSONObject("message")?.optString("content").orEmpty()
                if (msg.isNotBlank()) return msg
            }
            root.optString("response")
                .ifBlank { root.optString("text") }
                .ifBlank { root.optString("output") }
                .ifBlank { rawJson }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            rawJson
        }
    }

    private companion object {
        const val TAG = "AiAssistantRepository"
    }
}
