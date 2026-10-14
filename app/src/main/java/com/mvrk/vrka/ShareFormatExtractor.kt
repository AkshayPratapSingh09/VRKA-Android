package com.mvrk.vrka

import android.content.Context
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.URI
import java.util.Locale

/**
 * Format option presented to the user.
 */
data class ProbedFormatOption(
    val id: String,
    val mode: MediaMode,
    val label: String,
    val sublabel: String,
    val quality: VideoQuality = VideoQuality.BEST,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val mp3Bitrate: Int = 320,
    val estimatedSize: String? = null,
    val fps: Int? = null,
    val isRecommended: Boolean = false,
)

/**
 * Metadata and format options parsed from yt-dlp probe or fallback.
 */
data class ProbedMediaDetails(
    val url: String,
    val title: String,
    val author: String? = null,
    val durationSeconds: Int? = null,
    val durationText: String? = null,
    val thumbnailUrl: String? = null,
    val videoOptions: List<ProbedFormatOption> = emptyList(),
    val audioOptions: List<ProbedFormatOption> = emptyList(),
    val isFallback: Boolean = false,
    val warningMessage: String? = null,
)

/**
 * UI State for stream format probing.
 */
sealed class ShareProbeState {
    object Idle : ShareProbeState()
    data class Probing(val elapsedSeconds: Int, val stageText: String) : ShareProbeState()
    data class Ready(val details: ProbedMediaDetails) : ShareProbeState()
    data class Fallback(val details: ProbedMediaDetails, val warningMessage: String) : ShareProbeState()
}

object ShareFormatExtractor {
    private const val TAG = "ShareFormatExtractor"
    const val MAX_PROBE_TIMEOUT_MS = 50_000L // Safeguard within 1 minute

    fun formatDuration(seconds: Int?): String? {
        if (seconds == null || seconds <= 0) return null
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%d:%02d", m, s)
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun estimateVideoSize(height: Int?, durationSeconds: Int?): String? {
        if (durationSeconds == null || durationSeconds <= 0) return null
        val avgBitrateKbps = when (height) {
            2160 -> 18000L
            1440 -> 8000L
            1080 -> 3500L
            720 -> 1800L
            480 -> 900L
            360 -> 500L
            240 -> 300L
            144 -> 150L
            120 -> 100L
            else -> 2500L
        }
        val estimatedBytes = (avgBitrateKbps * 1000L / 8L) * durationSeconds
        return "~${formatFileSize(estimatedBytes)}"
    }

    fun estimateAudioSize(bitrateKbps: Int, durationSeconds: Int?): String? {
        if (durationSeconds == null || durationSeconds <= 0) return null
        val bytes = (bitrateKbps.toLong() * 1000L / 8L) * durationSeconds
        return "~${formatFileSize(bytes)}"
    }

    fun createFallbackDetails(url: String, warningMessage: String? = null, titleHint: String? = null): ProbedMediaDetails {
        val host = runCatching { URI(url).host?.removePrefix("www.") }.getOrNull() ?: "media"
        return ProbedMediaDetails(
            url = url,
            title = titleHint ?: "Media link from $host",
            author = host,
            durationSeconds = null,
            durationText = null,
            thumbnailUrl = null,
            videoOptions = listOf(
                ProbedFormatOption(
                    id = "video_1080p",
                    mode = MediaMode.VIDEO,
                    label = "1080p Full HD",
                    sublabel = "MP4 • Best Video + Audio",
                    quality = VideoQuality.P1080,
                    isRecommended = true,
                ),
                ProbedFormatOption(
                    id = "video_720p",
                    mode = MediaMode.VIDEO,
                    label = "720p HD",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P720,
                ),
                ProbedFormatOption(
                    id = "video_480p",
                    mode = MediaMode.VIDEO,
                    label = "480p SD",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P480,
                ),
                ProbedFormatOption(
                    id = "video_360p",
                    mode = MediaMode.VIDEO,
                    label = "360p Standard",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P360,
                ),
                ProbedFormatOption(
                    id = "video_240p",
                    mode = MediaMode.VIDEO,
                    label = "240p Compact",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P240,
                ),
                ProbedFormatOption(
                    id = "video_144p",
                    mode = MediaMode.VIDEO,
                    label = "144p Compact",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P144,
                ),
                ProbedFormatOption(
                    id = "video_120p",
                    mode = MediaMode.VIDEO,
                    label = "120p Ultra Low",
                    sublabel = "MP4 • Video + Audio",
                    quality = VideoQuality.P120,
                ),
            ),
            audioOptions = listOf(
                ProbedFormatOption(
                    id = "audio_mp3_320",
                    mode = MediaMode.AUDIO,
                    label = "MP3 (320 kbps)",
                    sublabel = "High Quality Audio • MP3",
                    audioFormat = AudioFormat.MP3,
                    mp3Bitrate = 320,
                    isRecommended = true,
                ),
                ProbedFormatOption(
                    id = "audio_mp3_192",
                    mode = MediaMode.AUDIO,
                    label = "MP3 (192 kbps)",
                    sublabel = "Standard Quality Audio • MP3",
                    audioFormat = AudioFormat.MP3,
                    mp3Bitrate = 192,
                ),
                ProbedFormatOption(
                    id = "audio_opus",
                    mode = MediaMode.AUDIO,
                    label = "Opus Native",
                    sublabel = "Best Quality Native Opus Stream",
                    audioFormat = AudioFormat.OPUS,
                ),
                ProbedFormatOption(
                    id = "audio_wav",
                    mode = MediaMode.AUDIO,
                    label = "WAV Lossless",
                    sublabel = "Uncompressed Source Audio (PCM)",
                    audioFormat = AudioFormat.WAV,
                ),
            ),
            isFallback = true,
            warningMessage = warningMessage,
        )
    }

    fun parseVideoInfo(url: String, info: VideoInfo): ProbedMediaDetails {
        val title = info.title?.take(200)?.ifBlank { null }
            ?: runCatching { URI(url).host?.removePrefix("www.") }.getOrNull()
            ?: "Media Download"
        val author = info.uploader?.take(80) ?: info.uploaderId?.take(80)
        val duration = info.duration.takeIf { it > 0 }
        val durationText = formatDuration(duration)
        val thumbnail = info.thumbnail

        val formats = info.formats.orEmpty()
        val videoStreams = formats.filter {
            (it.vcodec != null && it.vcodec != "none") || (it.height > 0)
        }
        val audioStreams = formats.filter {
            (it.acodec != null && it.acodec != "none") && (it.vcodec == null || it.vcodec == "none")
        }

        val bestAudioStream = audioStreams.maxByOrNull { it.abr }
        val bestAudioSize = bestAudioStream?.let {
            it.fileSize.takeIf { s -> s > 0 } ?: it.fileSizeApproximate.takeIf { s -> s > 0 }
        } ?: 0L

        val availableHeights = videoStreams.map { it.height }.filter { it > 0 }.toSet()

        val videoOptions = mutableListOf<ProbedFormatOption>()

        val tiers = listOf(
            Triple(VideoQuality.P2160, "2160p 4K", 2000..4320),
            Triple(VideoQuality.P1440, "1440p 2K", 1400..1999),
            Triple(VideoQuality.P1080, "1080p Full HD", 1000..1399),
            Triple(VideoQuality.P720, "720p HD", 700..999),
            Triple(VideoQuality.P480, "480p SD", 450..699),
            Triple(VideoQuality.P360, "360p Standard", 300..449),
            Triple(VideoQuality.P240, "240p Compact", 180..299),
            Triple(VideoQuality.P144, "144p Compact", 130..179),
            Triple(VideoQuality.P120, "120p Ultra Low", 60..129),
        )

        for ((quality, label, range) in tiers) {
            val matchingStreams = videoStreams.filter { it.height in range }
            if (matchingStreams.isNotEmpty() || (availableHeights.isEmpty() && quality.height in listOf(1080, 720, 480, 360, 240, 144, 120))) {
                val bestStream = matchingStreams.maxByOrNull { it.fps }
                val vSize = bestStream?.let {
                    it.fileSize.takeIf { s -> s > 0 } ?: it.fileSizeApproximate.takeIf { s -> s > 0 }
                } ?: 0L
                val totalBytes = if (vSize > 0) vSize + bestAudioSize else 0L
                val sizeText = if (totalBytes > 0) {
                    formatFileSize(totalBytes)
                } else {
                    estimateVideoSize(quality.height, duration)
                }
                val fpsText = if (bestStream?.fps != null && bestStream.fps >= 60) " • 60fps" else ""
                val isRecommended = quality == VideoQuality.P1080 || (quality == VideoQuality.P720 && !availableHeights.any { it >= 1000 })

                videoOptions.add(
                    ProbedFormatOption(
                        id = "video_${quality.name.lowercase()}",
                        mode = MediaMode.VIDEO,
                        label = label,
                        sublabel = "MP4 • Video + Audio$fpsText",
                        quality = quality,
                        estimatedSize = sizeText,
                        fps = bestStream?.fps,
                        isRecommended = isRecommended,
                    )
                )
            }
        }

        if (videoOptions.isEmpty()) {
            listOf(
                Pair(VideoQuality.P1080, "1080p Full HD"),
                Pair(VideoQuality.P720, "720p HD"),
                Pair(VideoQuality.P480, "480p SD"),
                Pair(VideoQuality.P360, "360p Standard"),
                Pair(VideoQuality.P240, "240p Compact"),
                Pair(VideoQuality.P144, "144p Compact"),
                Pair(VideoQuality.P120, "120p Ultra Low"),
            ).forEach { (quality, label) ->
                videoOptions.add(
                    ProbedFormatOption(
                        id = "video_${quality.name.lowercase()}",
                        mode = MediaMode.VIDEO,
                        label = label,
                        sublabel = "MP4 • Video + Audio",
                        quality = quality,
                        estimatedSize = estimateVideoSize(quality.height, duration),
                        isRecommended = quality == VideoQuality.P1080,
                    )
                )
            }
        }

        val audioOptions = listOf(
            ProbedFormatOption(
                id = "audio_mp3_320",
                mode = MediaMode.AUDIO,
                label = "MP3 (320 kbps)",
                sublabel = "High Quality Audio • MP3",
                audioFormat = AudioFormat.MP3,
                mp3Bitrate = 320,
                estimatedSize = estimateAudioSize(320, duration),
                isRecommended = true,
            ),
            ProbedFormatOption(
                id = "audio_mp3_192",
                mode = MediaMode.AUDIO,
                label = "MP3 (192 kbps)",
                sublabel = "Standard Quality Audio • MP3",
                audioFormat = AudioFormat.MP3,
                mp3Bitrate = 192,
                estimatedSize = estimateAudioSize(192, duration),
            ),
            ProbedFormatOption(
                id = "audio_opus",
                mode = MediaMode.AUDIO,
                label = "Opus Native",
                sublabel = "Best Quality Native Opus Stream",
                audioFormat = AudioFormat.OPUS,
                estimatedSize = if (bestAudioSize > 0) formatFileSize(bestAudioSize) else estimateAudioSize(128, duration),
            ),
            ProbedFormatOption(
                id = "audio_wav",
                mode = MediaMode.AUDIO,
                label = "WAV Lossless",
                sublabel = "Uncompressed Source Audio (PCM)",
                audioFormat = AudioFormat.WAV,
                estimatedSize = if (duration != null) "~${formatFileSize(duration * 176400L)}" else null,
            ),
        )

        return ProbedMediaDetails(
            url = url,
            title = title,
            author = author,
            durationSeconds = duration,
            durationText = durationText,
            thumbnailUrl = thumbnail,
            videoOptions = videoOptions,
            audioOptions = audioOptions,
            isFallback = false,
        )
    }

    suspend fun probeUrl(
        context: Context,
        url: String,
        onProgress: (elapsedSec: Int, stage: String) -> Unit = { _, _ -> },
    ): Result<ProbedMediaDetails> = withContext(Dispatchers.IO) {
        runCatching {
            try {
                YoutubeDL.getInstance().init(context)
                DownloadRequestFactory.initNativeLibraryDir(context.applicationInfo.nativeLibraryDir)
            } catch (t: Throwable) {
                Log.w(TAG, "YoutubeDL init note: ${t.message}")
            }

            val request = YoutubeDLRequest(url).apply {
                addOption("--no-warnings")
                addOption("--legacy-server-connect")
                addOption("--no-playlist")
                DownloadRequestFactory.getQuickJsPath()?.let { qjs ->
                    addOption("--js-runtimes", "quickjs:$qjs")
                }
                addOption("--remote-components", "ejs:github")
            }

            var elapsed = 0
            val tickerJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive) {
                    val stage = when {
                        elapsed < 3 -> "Connecting to media source..."
                        elapsed < 8 -> "Extracting stream manifests..."
                        elapsed < 16 -> "Resolving video & audio quality variants..."
                        elapsed < 30 -> "Analyzing available codecs & bitrates..."
                        else -> "Finishing probe (< 1 min limit)..."
                    }
                    onProgress(elapsed, stage)
                    delay(1000L)
                    elapsed++
                }
            }

            try {
                val info = withTimeout(MAX_PROBE_TIMEOUT_MS) {
                    YoutubeDL.getInstance().getInfo(request)
                }
                parseVideoInfo(url, info)
            } finally {
                tickerJob.cancel()
            }
        }
    }
}
