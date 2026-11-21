package com.aistudio.examyfua.k7m2px.data.remote

import com.aistudio.examyfua.k7m2px.domain.model.DriveBackupMetadata
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GoogleDriveBackupClient {
    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_FILE_NAME = "yfua_room_backup.json.gz"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun buildAuthorizationRequest(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()

    private fun gzipCompress(text: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(text.toByteArray(Charsets.UTF_8))
        }
        return bos.toByteArray()
    }

    private fun gzipDecompressOrPlain(bytes: ByteArray): String {
        val isGzip = bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()
        return if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            bytes.toString(Charsets.UTF_8)
        }
    }

    suspend fun getLatestBackupMetadata(accessToken: String): Result<DriveBackupMetadata?> =
        withContext(Dispatchers.IO) {
            try {
                val result = withTimeoutOrNull(15_000L) {
                    val url = "$DRIVE_FILES_URL?spaces=appDataFolder&q=name%3D%27$BACKUP_FILE_NAME%27&fields=files(id,name,modifiedTime,size)"
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $accessToken")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            return@withTimeoutOrNull Result.failure<DriveBackupMetadata?>(
                                IllegalStateException("Erreur Google Drive (${response.code}) : vérifiez l'activation de Drive API.")
                            )
                        }
                        val files = JSONObject(body).optJSONArray("files")
                        if (files == null || files.length() == 0) {
                            Result.success(null)
                        } else {
                            val first = files.getJSONObject(0)
                            Result.success(
                                DriveBackupMetadata(
                                    fileId = first.getString("id"),
                                    modifiedTimeIso = first.optString("modifiedTime", ""),
                                    sizeBytes = first.optString("size", "0").toLongOrNull() ?: 0L
                                )
                            )
                        }
                    }
                }
                result ?: Result.failure(IllegalStateException("Délai dépassé lors de l'accès à Google Drive."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun uploadBackupJson(accessToken: String, jsonPayload: String): Result<DriveBackupMetadata> =
        withContext(Dispatchers.IO) {
            try {
                val existingMeta = getLatestBackupMetadata(accessToken).getOrNull()
                val compressedBytes = gzipCompress(jsonPayload)
                val result = withTimeoutOrNull(15_000L) {
                    val metadataJson = if (existingMeta == null) {
                        JSONObject().apply {
                            put("name", BACKUP_FILE_NAME)
                            put("parents", org.json.JSONArray(listOf("appDataFolder")))
                        }.toString()
                    } else {
                        JSONObject().apply {
                            put("name", BACKUP_FILE_NAME)
                        }.toString()
                    }

                    val multipartBody = MultipartBody.Builder()
                        .setType("multipart/related".toMediaType())
                        .addPart(metadataJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .addPart(compressedBytes.toRequestBody("application/gzip".toMediaType()))
                        .build()

                    val reqBuilder = Request.Builder()
                        .addHeader("Authorization", "Bearer $accessToken")

                    val request = if (existingMeta == null) {
                        reqBuilder
                            .url("$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id,modifiedTime,size")
                            .post(multipartBody)
                            .build()
                    } else {
                        reqBuilder
                            .url("$DRIVE_UPLOAD_URL/${existingMeta.fileId}?uploadType=multipart&fields=id,modifiedTime,size")
                            .patch(multipartBody)
                            .build()
                    }

                    client.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (!response.isSuccessful) {
                            return@withTimeoutOrNull Result.failure<DriveBackupMetadata>(
                                IllegalStateException("Échec de l'envoi Drive (${response.code}).")
                            )
                        }
                        val obj = JSONObject(body)
                        Result.success(
                            DriveBackupMetadata(
                                fileId = obj.getString("id"),
                                modifiedTimeIso = obj.optString("modifiedTime", ""),
                                sizeBytes = obj.optString("size", "0").toLongOrNull() ?: compressedBytes.size.toLong()
                            )
                        )
                    }
                }
                result ?: Result.failure(IllegalStateException("Délai dépassé lors de la sauvegarde Drive."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun downloadBackupJson(accessToken: String, fileId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val result = withTimeoutOrNull(15_000L) {
                    val request = Request.Builder()
                        .url("$DRIVE_FILES_URL/$fileId?alt=media")
                        .addHeader("Authorization", "Bearer $accessToken")
                        .get()
                        .build()
                    client.newCall(request).execute().use { response ->
                        val bytes = response.body?.bytes()
                        if (!response.isSuccessful || bytes == null) {
                            return@withTimeoutOrNull Result.failure<String>(
                                IllegalStateException("Échec du téléchargement Drive (${response.code}).")
                            )
                        }
                        Result.success(gzipDecompressOrPlain(bytes))
                    }
                }
                result ?: Result.failure(IllegalStateException("Délai dépassé lors de la restauration Drive."))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
