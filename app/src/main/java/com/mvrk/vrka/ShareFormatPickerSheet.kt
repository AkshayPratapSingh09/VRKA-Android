package com.mvrk.vrka

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val SheetBackground = Color(0xFF17181C)
private val SurfaceCardColor = Color(0xFF202227)
private val SurfacePillColor = Color(0xFF282A31)
private val MintAccent = Color(0xFF97E7BA)
private val CyberLimeAccent = Color(0xFFFBFB78)
private val BorderColor = Color(0xFF2E3139)
private val TextPrimaryColor = Color(0xFFFFFFFF)
private val TextSecondaryColor = Color(0xFF9EA0A8)
private val TextTertiaryColor = Color(0xFF6B6D74)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFormatPickerSheet(
    url: String,
    manager: VrkaDownloadManager,
    titleHint: String? = null,
    onDismiss: () -> Unit,
    onEnqueue: (DownloadRequest, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var probeState by remember(url) {
        mutableStateOf<ShareProbeState>(
            ShareProbeState.Probing(0, "Connecting to media source...")
        )
    }

    var selectedTab by remember { mutableStateOf(MediaMode.VIDEO) }

    LaunchedEffect(url) {
        val result = manager.probeMediaFormats(url) { elapsed, stage ->
            probeState = ShareProbeState.Probing(elapsed, stage)
        }
        result.onSuccess { details ->
            probeState = ShareProbeState.Ready(details)
        }.onFailure { error ->
            val fallback = ShareFormatExtractor.createFallbackDetails(
                url = url,
                warningMessage = error.message ?: "Format probe timed out or failed.",
                titleHint = titleHint,
            )
            probeState = ShareProbeState.Fallback(
                fallback,
                error.message ?: "Format probing timed out (< 1 min). Standard formats ready below:"
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetBackground,
        contentColor = TextPrimaryColor,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(Color(0xFF383B44), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Choose Format",
                        color = TextPrimaryColor,
                        fontFamily = VrkaSansFamily,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Select quality to start download",
                        color = TextSecondaryColor,
                        fontFamily = VrkaSansFamily,
                        fontSize = 12.sp,
                    )
                }

                IconButton(
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurfacePillColor, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = TextSecondaryColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = probeState) {
                is ShareProbeState.Probing -> {
                    ProbingLoaderView(
                        elapsedSeconds = state.elapsedSeconds,
                        stageText = state.stageText,
                        onSkipProbe = {
                            val fallback = ShareFormatExtractor.createFallbackDetails(
                                url = url,
                                titleHint = titleHint,
                            )
                            probeState = ShareProbeState.Ready(fallback)
                        }
                    )
                }

                is ShareProbeState.Ready -> {
                    FormatSelectorView(
                        details = state.details,
                        selectedTab = selectedTab,
                        onTabChange = { selectedTab = it },
                        onSelectOption = { option ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val request = when (option.mode) {
                                MediaMode.VIDEO -> DownloadRequest(
                                    url = url,
                                    mode = MediaMode.VIDEO,
                                    quality = option.quality,
                                    embedMetadata = true,
                                )
                                MediaMode.AUDIO -> DownloadRequest(
                                    url = url,
                                    mode = MediaMode.AUDIO,
                                    audioFormat = option.audioFormat,
                                    mp3Bitrate = option.mp3Bitrate,
                                    embedMetadata = true,
                                    embedThumbnail = true,
                                )
                            }
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onEnqueue(request, state.details.title)
                                onDismiss()
                            }
                        }
                    )
                }

                is ShareProbeState.Fallback -> {
                    Column {
                        // Warning badge
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF2C2415),
                            border = BorderStroke(1.dp, Color(0xFF6B5324)),
                        ) {
                            Text(
                                text = "Probe took > 45s. Standard profiles available below:",
                                color = CyberLimeAccent,
                                fontSize = 12.sp,
                                fontFamily = VrkaSansFamily,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }

                        FormatSelectorView(
                            details = state.details,
                            selectedTab = selectedTab,
                            onTabChange = { selectedTab = it },
                            onSelectOption = { option ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val request = when (option.mode) {
                                    MediaMode.VIDEO -> DownloadRequest(
                                        url = url,
                                        mode = MediaMode.VIDEO,
                                        quality = option.quality,
                                        embedMetadata = true,
                                    )
                                    MediaMode.AUDIO -> DownloadRequest(
                                        url = url,
                                        mode = MediaMode.AUDIO,
                                        audioFormat = option.audioFormat,
                                        mp3Bitrate = option.mp3Bitrate,
                                        embedMetadata = true,
                                        embedThumbnail = true,
                                    )
                                }
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    onEnqueue(request, state.details.title)
                                    onDismiss()
                                }
                            }
                        )
                    }
                }

                ShareProbeState.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProbingLoaderView(
    elapsedSeconds: Int,
    stageText: String,
    onSkipProbe: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceCardColor,
        border = BorderStroke(1.dp, BorderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MintAccent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(38.dp),
                    color = MintAccent,
                    strokeWidth = 3.dp,
                    strokeCap = StrokeCap.Round,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Inspecting Available Streams",
                color = TextPrimaryColor,
                fontFamily = VrkaSansFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stageText,
                color = CyberLimeAccent.copy(alpha = alphaAnim),
                fontFamily = VrkaSansFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(14.dp))

            val progress = (elapsedSeconds / 50f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(4.dp)
                    .clip(CircleShape),
                color = MintAccent,
                trackColor = SurfacePillColor,
                strokeCap = StrokeCap.Round,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Elapsed: ${elapsedSeconds}s / 50s max",
                color = TextTertiaryColor,
                fontFamily = VrkaSansFamily,
                fontSize = 11.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = onSkipProbe,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
            ) {
                Text(
                    text = "Skip wait & choose standard format",
                    color = TextSecondaryColor,
                    fontFamily = VrkaSansFamily,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun FormatSelectorView(
    details: ProbedMediaDetails,
    selectedTab: MediaMode,
    onTabChange: (MediaMode) -> Unit,
    onSelectOption: (ProbedFormatOption) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Media Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceCardColor,
            border = BorderStroke(1.dp, BorderColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!details.thumbnailUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .size(width = 90.dp, height = 56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        AsyncThumbnail(
                            url = details.thumbnailUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (!details.durationText.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = details.durationText,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = VrkaSansFamily,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = details.title,
                        color = TextPrimaryColor,
                        fontFamily = VrkaSansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!details.author.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = details.author,
                            color = TextSecondaryColor,
                            fontFamily = VrkaSansFamily,
                            fontSize = 11.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mode Switcher Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCardColor, RoundedCornerShape(12.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ModeTabItem(
                mode = MediaMode.VIDEO,
                label = "Video (with Audio)",
                isSelected = selectedTab == MediaMode.VIDEO,
                onSelect = { onTabChange(MediaMode.VIDEO) },
                modifier = Modifier.weight(1f),
            )
            ModeTabItem(
                mode = MediaMode.AUDIO,
                label = "Audio Only",
                isSelected = selectedTab == MediaMode.AUDIO,
                onSelect = { onTabChange(MediaMode.AUDIO) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Format options list
        val currentOptions = if (selectedTab == MediaMode.VIDEO) {
            details.videoOptions
        } else {
            details.audioOptions
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            currentOptions.forEach { option ->
                FormatOptionCard(
                    option = option,
                    onClick = { onSelectOption(option) }
                )
            }
        }
    }
}

@Composable
private fun FormatOptionCard(
    option: ProbedFormatOption,
    onClick: () -> Unit,
) {
    val borderColor = if (option.isRecommended) MintAccent.copy(alpha = 0.8f) else BorderColor
    val badgeBg = if (option.isRecommended) MintAccent else SurfacePillColor
    val badgeText = if (option.isRecommended) Color(0xFF17181C) else TextPrimaryColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceCardColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                // Resolution / format badge
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label.split(" ").firstOrNull() ?: option.label,
                        color = badgeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VrkaSansFamily,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = option.label,
                            color = TextPrimaryColor,
                            fontFamily = VrkaSansFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (option.isRecommended) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "RECOMMENDED",
                                color = MintAccent,
                                fontFamily = VrkaSansFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = option.sublabel,
                            color = TextSecondaryColor,
                            fontFamily = VrkaSansFamily,
                            fontSize = 11.sp,
                        )
                        if (!option.estimatedSize.isNullOrBlank()) {
                            Text(
                                text = "•  ${option.estimatedSize}",
                                color = CyberLimeAccent,
                                fontFamily = VrkaSansFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action puck
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MintAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_download),
                    contentDescription = "Enqueue",
                    tint = Color(0xFF17181C),
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
fun AsyncThumbnail(
    url: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (!url.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 6000
                    connection.readTimeout = 6000
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }?.let { bitmap = it }
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
                painter = painterResource(R.drawable.ic_download),
                contentDescription = null,
                tint = Color(0xFF4C4E57),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ModeTabItem(
    mode: MediaMode,
    label: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (isSelected) MintAccent else Color.Transparent,
        label = "tabBg_${mode.name}",
    )
    val textCol by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF17181C) else TextSecondaryColor,
        label = "tabText_${mode.name}",
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable { onSelect() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textCol,
            fontFamily = VrkaSansFamily,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
