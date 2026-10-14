package com.mvrk.vrka

import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareFormatExtractorTest {

    @Test
    fun testFormatDuration() {
        assertEquals("0:45", ShareFormatExtractor.formatDuration(45))
        assertEquals("3:34", ShareFormatExtractor.formatDuration(214))
        assertEquals("1:05:02", ShareFormatExtractor.formatDuration(3902))
        assertEquals(null, ShareFormatExtractor.formatDuration(null))
        assertEquals(null, ShareFormatExtractor.formatDuration(0))
    }

    @Test
    fun testFormatFileSize() {
        assertEquals("500 B", ShareFormatExtractor.formatFileSize(500))
        assertEquals("500 KB", ShareFormatExtractor.formatFileSize(500 * 1024))
        assertEquals("45.0 MB", ShareFormatExtractor.formatFileSize(45 * 1024 * 1024))
        assertEquals("1.5 GB", ShareFormatExtractor.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testCreateFallbackDetails() {
        val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        val fallback = ShareFormatExtractor.createFallbackDetails(url)

        assertTrue(fallback.isFallback)
        assertEquals("youtube.com", fallback.author)
        assertEquals(4, fallback.videoOptions.size)
        assertEquals("1080p Full HD", fallback.videoOptions[0].label)
        assertEquals(VideoQuality.P1080, fallback.videoOptions[0].quality)
        assertEquals(VideoQuality.P720, fallback.videoOptions[1].quality)
        assertEquals(VideoQuality.P480, fallback.videoOptions[2].quality)
        assertEquals(VideoQuality.P360, fallback.videoOptions[3].quality)

        assertEquals(4, fallback.audioOptions.size)
        assertEquals(AudioFormat.MP3, fallback.audioOptions[0].audioFormat)
        assertEquals(320, fallback.audioOptions[0].mp3Bitrate)
        assertEquals(AudioFormat.OPUS, fallback.audioOptions[2].audioFormat)
    }

    @Test
    fun testUrlExtractorWithTitle() {
        val sharedText = "Rick Astley - Never Gonna Give You Up (Official Music Video)\nhttps://youtu.be/dQw4w9WgXcQ?si=abcdef123"
        val extractedUrl = UrlExtractor.extractUrl(sharedText)
        val extractedTitle = UrlExtractor.extractTitleHint(sharedText)

        assertEquals("https://youtu.be/dQw4w9WgXcQ?si=abcdef123", extractedUrl)
        assertEquals("Rick Astley - Never Gonna Give You Up (Official Music Video)", extractedTitle)
    }

    @Test
    fun testUrlExtractorDirectUrlOnly() {
        val sharedText = "https://www.youtube.com/watch?v=test123"
        val extractedUrl = UrlExtractor.extractUrl(sharedText)
        val extractedTitle = UrlExtractor.extractTitleHint(sharedText)

        assertEquals("https://www.youtube.com/watch?v=test123", extractedUrl)
        assertEquals(null, extractedTitle)
    }
}
