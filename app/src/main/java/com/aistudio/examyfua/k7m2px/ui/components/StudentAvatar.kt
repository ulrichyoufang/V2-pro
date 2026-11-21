package com.aistudio.examyfua.k7m2px.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.examyfua.k7m2px.ui.theme.PureWhite
import com.aistudio.examyfua.k7m2px.ui.theme.YfuaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun StudentAvatar(
    initials: String,
    size: Dp = 48.dp,
    photoPath: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fontSize = (size.value * 0.38f).sp

    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photoPath) {
        val raw = photoPath?.trim().orEmpty()
        value = if (raw.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    if (raw.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(raw))?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else {
                        val file = File(raw)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(YfuaTheme.colors.avatarTeal)
            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Photo de profil de l'étudiant",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials.take(2).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = PureWhite
            )
        }
    }
}
