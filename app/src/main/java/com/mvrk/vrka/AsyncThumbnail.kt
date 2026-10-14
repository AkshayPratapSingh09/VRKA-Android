package com.mvrk.vrka

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ThumbnailCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)
    private val cache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    fun get(url: String): ImageBitmap? = synchronized(cache) { cache.get(url) }
    fun put(url: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(url, bitmap) }
    }
}

@Composable
fun AsyncThumbnail(
    url: String?,
    modifier: Modifier = Modifier,
    placeholderIconRes: Int = R.drawable.ic_download,
) {
    var bitmap by remember(url) {
        mutableStateOf(url?.let { ThumbnailCache.get(it) })
    }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank() && bitmap == null) {
            val cached = ThumbnailCache.get(url)
            if (cached != null) {
                bitmap = cached
            } else {
                val loaded = withContext(Dispatchers.IO) {
                    runCatching {
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.connectTimeout = 6000
                        connection.readTimeout = 6000
                        connection.inputStream.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }.getOrNull()
                }
                if (loaded != null) {
                    ThumbnailCache.put(url, loaded)
                    bitmap = loaded
                }
            }
        }
    }

    Box(
        modifier = modifier.background(Color(0xFF22242B)),
        contentAlignment = Alignment.Center,
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(placeholderIconRes),
                contentDescription = null,
                tint = Color(0xFF4C4E57),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
