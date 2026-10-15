package com.mvrk.vrka

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
internal fun JobsScreen(
    jobs: List<DownloadJob>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteRecord: ((String) -> Unit)? = null,
    onDeleteWithFile: ((String) -> Unit)? = null,
    onDeleteRecords: ((Collection<String>) -> Unit)? = null,
    onDeleteJobsWithFiles: ((Collection<String>) -> Unit)? = null,
    onClearAll: ((Boolean) -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    onShowFallback: ((String) -> Unit)? = null,
    onSetDestination: ((String, String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val isHistory = onClear != null || onClearAll != null
    var pendingFolderJobId by remember { mutableStateOf<String?>(null) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedJobIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    var singleDeleteTarget by remember { mutableStateOf<DownloadJob?>(null) }
    var showMultiDeleteRecordsDialog by remember { mutableStateOf(false) }
    var showMultiDeleteFilesDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var clearAllDeleteFilesChecked by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val id = pendingFolderJobId
        if (uri != null && id != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            onSetDestination?.invoke(id, uri.toString())
        }
        pendingFolderJobId = null
    }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode) {
                Text(
                    text = "${selectedJobIds.size} selected",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VrkaTokens.TextPrimary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allSelected = jobs.isNotEmpty() && selectedJobIds.size == jobs.size
                    VrkaTextButton(
                        text = if (allSelected) "Deselect all" else "Select all",
                        onClick = {
                            selectedJobIds = if (allSelected) emptySet() else jobs.map { it.id }.toSet()
                        },
                        color = VrkaTokens.AccentLight,
                    )
                    VrkaTextButton(
                        text = "Done",
                        onClick = {
                            isSelectionMode = false
                            selectedJobIds = emptySet()
                        },
                        color = VrkaTokens.TextSecondary,
                    )
                }
            } else {
                Text(
                    text = if (isHistory) "History" else "Queue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = VrkaTokens.TextPrimary,
                )
                if (isHistory && jobs.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VrkaTextButton(
                            text = "Select",
                            onClick = {
                                isSelectionMode = true
                                selectedJobIds = emptySet()
                            },
                            color = VrkaTokens.AccentLight,
                        )
                        VrkaTextButton(
                            text = "Clear history",
                            onClick = { showClearAllDialog = true },
                            color = VrkaTokens.TextSecondary,
                        )
                    }
                }
            }
        }

        if (isSelectionMode && selectedJobIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VrkaOutlinedButton(
                    text = "Remove records (${selectedJobIds.size})",
                    onClick = { showMultiDeleteRecordsDialog = true },
                    modifier = Modifier.weight(1f),
                )
                VrkaOutlinedButton(
                    text = "Delete files (${selectedJobIds.size})",
                    onClick = { showMultiDeleteFilesDialog = true },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.vrka_logo_512),
                        contentDescription = "VRKA",
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        modifier = Modifier.size(68.dp),
                    )
                    Text(
                        text = if (!isHistory) "Queue is empty" else "No download history",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = VrkaMonoFamily),
                        fontWeight = FontWeight.Bold,
                        color = VrkaTokens.TextPrimary,
                    )
                    Text(
                        text = if (!isHistory)
                            "Media you enqueue will appear here while downloading."
                        else
                            "Completed and archived downloads will be listed here.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = VrkaMonoFamily),
                        color = VrkaTokens.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 110.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(jobs, key = DownloadJob::id) { job ->
                    JobCard(
                        job = job,
                        isSelectionMode = isSelectionMode,
                        isSelected = job.id in selectedJobIds,
                        onToggleSelect = {
                            selectedJobIds = if (job.id in selectedJobIds) {
                                val remaining = selectedJobIds - job.id
                                if (remaining.isEmpty()) isSelectionMode = false
                                remaining
                            } else {
                                selectedJobIds + job.id
                            }
                        },
                        onEnterSelectionMode = {
                            isSelectionMode = true
                            selectedJobIds = setOf(job.id)
                        },
                        onCancel = { onCancel(job.id) },
                        onRetry = { onRetry(job.id) },
                        onOpen = onOpen,
                        onShare = onShare,
                        onDelete = { singleDeleteTarget = job },
                        onShowFallback = onShowFallback?.let { fn -> { fn(job.id) } },
                        onChooseFolder = {
                            pendingFolderJobId = job.id
                            folderPicker.launch(null)
                        },
                    )
                }
            }
        }
    }

    if (singleDeleteTarget != null) {
        val target = singleDeleteTarget!!
        val hasFiles = target.outputUris.isNotEmpty() && target.state == JobState.DONE
        AlertDialog(
            onDismissRequest = { singleDeleteTarget = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = VrkaTokens.SurfaceCard,
            title = {
                Text(
                    text = if (hasFiles) "Delete Download" else "Remove from History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VrkaTokens.TextPrimary,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = if (hasFiles) {
                            "Choose whether to remove this history record only, or also permanently delete the downloaded media file from your device storage."
                        } else {
                            "Remove \"${target.title.ifBlank { "this item" }}\" from your download history?"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = VrkaTokens.TextSecondary,
                    )

                    if (hasFiles) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    val id = target.id
                                    singleDeleteTarget = null
                                    (onDeleteWithFile ?: onDelete)(id)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VrkaTokens.Destructive),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                            ) {
                                Text(
                                    text = "Delete File & Record",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val id = target.id
                                        singleDeleteTarget = null
                                        (onDeleteRecord ?: onDelete)(id)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, VrkaTokens.BorderSubtle),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = VrkaTokens.TextPrimary),
                                    modifier = Modifier
                                        .weight(1.35f)
                                        .height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        text = "Remove Record Only",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }

                                TextButton(
                                    onClick = { singleDeleteTarget = null },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = VrkaTokens.TextSecondary),
                                    modifier = Modifier
                                        .weight(0.65f)
                                        .height(42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!hasFiles) {
                    Button(
                        onClick = {
                            val id = target.id
                            singleDeleteTarget = null
                            (onDeleteRecord ?: onDelete)(id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VrkaTokens.Destructive),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Remove")
                    }
                }
            },
            dismissButton = {
                if (!hasFiles) {
                    TextButton(
                        onClick = { singleDeleteTarget = null },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Cancel")
                    }
                }
            },
        )
    }

    if (showMultiDeleteRecordsDialog) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteRecordsDialog = false },
            title = {
                Text(
                    text = "Remove Records from History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Remove ${selectedJobIds.size} records from download history? Downloaded files on storage will remain untouched.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectedJobIds.toList()
                        showMultiDeleteRecordsDialog = false
                        selectedJobIds = emptySet()
                        isSelectionMode = false
                        (onDeleteRecords ?: { for (id in ids) onDelete(id) })(ids)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VrkaTokens.Destructive),
                ) {
                    Text("Remove Records")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteRecordsDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showMultiDeleteFilesDialog) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteFilesDialog = false },
            title = {
                Text(
                    text = "Permanently Delete Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Permanently delete ${selectedJobIds.size} downloaded files from your device storage and remove their history records? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val ids = selectedJobIds.toList()
                        showMultiDeleteFilesDialog = false
                        selectedJobIds = emptySet()
                        isSelectionMode = false
                        (onDeleteJobsWithFiles ?: onDeleteRecords ?: { for (id in ids) onDelete(id) })(ids)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VrkaTokens.Destructive),
                ) {
                    Text("Delete Files")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMultiDeleteFilesDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            title = {
                Text(
                    text = "Clear All History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Are you sure you want to clear your download history (${jobs.size} items)?",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { clearAllDeleteFilesChecked = !clearAllDeleteFilesChecked },
                    ) {
                        Checkbox(
                            checked = clearAllDeleteFilesChecked,
                            onCheckedChange = { clearAllDeleteFilesChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = VrkaTokens.Destructive),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Also delete downloaded files from device storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (clearAllDeleteFilesChecked) VrkaTokens.Destructive else VrkaTokens.TextPrimary,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val deleteFiles = clearAllDeleteFilesChecked
                        showClearAllDialog = false
                        clearAllDeleteFilesChecked = false
                        if (onClearAll != null) {
                            onClearAll(deleteFiles)
                        } else {
                            onClear?.invoke()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (clearAllDeleteFilesChecked) VrkaTokens.Destructive else VrkaTokens.Accent,
                    ),
                ) {
                    Text(if (clearAllDeleteFilesChecked) "Delete Files & History" else "Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JobCard(
    job: DownloadJob,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onEnterSelectionMode: (() -> Unit)? = null,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: () -> Unit,
    onShowFallback: (() -> Unit)? = null,
    onChooseFolder: (() -> Unit)? = null,
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (job.progress.coerceIn(0f, 100f) / 100f),
        label = "job_card_progress",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) VrkaTokens.SurfaceInset else VrkaTokens.SurfaceCard,
        border = if (isSelected) BorderStroke(1.5.dp, VrkaTokens.BorderActive) else BorderStroke(0.75.dp, VrkaTokens.BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelectionMode && onToggleSelect != null) {
                    Modifier.clickable(onClick = onToggleSelect)
                } else {
                    Modifier.combinedClickable(
                        onClick = {
                            when {
                                job.state == JobState.DONE && job.outputUris.isNotEmpty() -> onOpen(job.outputUris.first())
                                job.state == JobState.FAILED -> onRetry()
                                job.state == JobState.BROWSER_FALLBACK && onShowFallback != null -> onShowFallback()
                                job.state == JobState.WAITING_FOR_USER && onChooseFolder != null -> onChooseFolder()
                            }
                        },
                        onLongClick = {
                            if (isSelectionMode) {
                                onToggleSelect?.invoke()
                            } else {
                                onEnterSelectionMode?.invoke()
                            }
                        },
                    )
                }
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelectionMode && onToggleSelect != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = VrkaTokens.Accent),
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .padding(vertical = 4.dp)
                    .size(width = 82.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                AsyncThumbnail(
                    url = job.thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                )
                if (!job.durationText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xDD000000),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp),
                    ) {
                        Text(
                            text = job.durationText,
                            color = Color.White,
                            fontFamily = VrkaMonoFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    ) {
                        Text(
                            text = job.title.ifBlank { sourceLabel(job.request.url) },
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = VrkaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                            color = VrkaTokens.TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (job.uploader.isNotBlank()) {
                            Text(
                                text = job.uploader,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = VrkaSansFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = VrkaTokens.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    JobStatusCornerBadge(
                        job = job,
                        onRetry = onRetry,
                        onCancel = onCancel,
                        onShowFallback = onShowFallback,
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false),
                    ) {
                        val tags = remember(job) { jobMediaTags(job) }
                        tags.forEach { tag ->
                            MediaTagBadge(tag = tag)
                        }
                    }

                    if (job.state == JobState.DONE && job.outputUris.isNotEmpty()) {
                        FolderActionButton(job = job)
                    }
                }

                if (job.state == JobState.DOWNLOADING || job.state == JobState.POSTPROCESSING) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        color = VrkaTokens.Accent,
                        trackColor = VrkaTokens.SurfaceInset,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${job.progress.toInt()}%",
                            fontFamily = VrkaMonoFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = VrkaTokens.AccentLight,
                        )
                        Text(
                            text = listOfNotNull(
                                job.speed.takeIf(String::isNotBlank),
                                job.etaSeconds?.let(::formatEtaCompact),
                            ).joinToString(" • "),
                            fontFamily = VrkaMonoFamily,
                            fontSize = 11.sp,
                            color = VrkaTokens.TextSecondary,
                        )
                    }
                }

                if (job.state == JobState.FAILED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = friendlyFailureTitle(job),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = VrkaMonoFamily, fontSize = 11.sp),
                            color = VrkaTokens.Error,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "Tap to retry",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = VrkaSansFamily, fontSize = 10.sp),
                            color = VrkaTokens.AccentLight,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }

                if (job.state == JobState.WAITING_FOR_USER && onChooseFolder != null) {
                    VrkaOutlinedButton(
                        text = "Set Location",
                        onClick = onChooseFolder,
                        height = 28.dp,
                        modifier = Modifier.padding(top = 6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    )
                }
                if (job.state == JobState.BROWSER_FALLBACK && onShowFallback != null) {
                    VrkaOutlinedButton(
                        text = "Interact",
                        onClick = onShowFallback,
                        height = 28.dp,
                        modifier = Modifier.padding(top = 6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun JobStatusCornerBadge(
    job: DownloadJob,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onShowFallback: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (job.state) {
        JobState.DONE -> {
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(VrkaTokens.Success),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        JobState.DOWNLOADING, JobState.POSTPROCESSING -> {
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = (job.progress.coerceIn(0f, 100f) / 100f),
                label = "corner_progress",
            )
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(VrkaTokens.SurfaceInset)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    color = VrkaTokens.Accent,
                    trackColor = VrkaTokens.BorderSubtle,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Cancel download",
                    tint = VrkaTokens.TextSecondary,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
        JobState.FAILED -> {
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(VrkaTokens.Error)
                    .clickable(onClick = onRetry),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = "Retry",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
        JobState.CANCELLED -> {
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333742)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "Cancelled",
                    tint = VrkaTokens.TextSecondary,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        JobState.WAITING_FOR_USER, JobState.BROWSER_FALLBACK -> {
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(VrkaTokens.Warning)
                    .clickable { onShowFallback?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = VrkaSansFamily,
                )
            }
        }
        JobState.QUEUED, JobState.PREPARING -> {
            Box(
                modifier = modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262930)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = VrkaTokens.AccentLight,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun MediaTagBadge(
    tag: MediaTagInfo,
    modifier: Modifier = Modifier,
) {
    val isLight = LocalVrkaColors.current.isLight

    val (bg, textColor, borderColor) = when (tag.type) {
        MediaTagType.VIDEO -> if (isLight) {
            Triple(
                Color(0xFFEEF4FF),
                Color(0xFF1D58BD),
                Color(0x281D58BD),
            )
        } else {
            Triple(
                Color(0xFF151D2D),
                Color(0xFF8BB5FE),
                Color(0x358BB5FE),
            )
        }
        MediaTagType.AUDIO -> if (isLight) {
            Triple(
                Color(0xFFFFF4E5),
                Color(0xFFB45309),
                Color(0x28B45309),
            )
        } else {
            Triple(
                Color(0xFF281E14),
                Color(0xFFFFB865),
                Color(0x35FFB865),
            )
        }
        MediaTagType.QUALITY, MediaTagType.AUDIO_SPEC -> if (isLight) {
            Triple(
                Color(0xFFE6F8EF),
                Color(0xFF0F683B),
                Color(0x3016804A),
            )
        } else {
            Triple(
                COLOR_MINT.copy(alpha = 0.12f),
                COLOR_MINT,
                COLOR_MINT.copy(alpha = 0.28f),
            )
        }
        MediaTagType.GENERIC -> if (isLight) {
            Triple(
                Color(0xFFF1F3F5),
                Color(0xFF4B5563),
                Color(0x18000000),
            )
        } else {
            Triple(
                Color(0xFF202229),
                Color(0xFFD1D5DB),
                Color(0x14FFFFFF),
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(0.75.dp, borderColor),
        modifier = modifier,
    ) {
        Text(
            text = tag.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = VrkaSansFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun FolderActionButton(
    job: DownloadJob,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val (locationDisplay, treeUri) = remember(job) {
        val tree = job.request.destinationTreeUri?.takeIf { it.isNotBlank() }
        if (tree != null) {
            val uri = Uri.parse(tree)
            val path = uri.lastPathSegment?.substringAfterLast(":") ?: "Downloads"
            Pair(path, tree)
        } else {
            Pair("Downloads/VRKA", null)
        }
    }

    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                shape = RoundedCornerShape(6.dp),
                containerColor = VrkaTokens.SurfaceElevated,
                contentColor = VrkaTokens.TextPrimary,
            ) {
                Text(
                    text = "Saved in: $locationDisplay",
                    fontFamily = VrkaSansFamily,
                    fontSize = 11.sp,
                )
            }
        },
        state = tooltipState,
    ) {
        Box(
            modifier = modifier
                .size(28.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(VrkaTokens.SurfaceInset)
                .border(BorderStroke(0.75.dp, VrkaTokens.BorderSubtle), RoundedCornerShape(7.dp))
                .combinedClickable(
                    onClick = {
                        openDownloadFolder(context, treeUri)
                    },
                    onLongClick = {
                        Toast.makeText(context, "Location: $locationDisplay", Toast.LENGTH_SHORT).show()
                        scope.launch { tooltipState.show() }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_folder),
                contentDescription = "Open Folder",
                tint = VrkaTokens.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

private fun openDownloadFolder(context: Context, destinationTreeUri: String?) {
    val tree = destinationTreeUri?.takeIf { it.isNotBlank() }
    if (tree != null) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(tree), DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val launched = runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
        if (launched) return
    }

    val downloadsTreeUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FVRKA")
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(downloadsTreeUri, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    runCatching {
        context.startActivity(intent)
    }.recoverCatching {
        val dlIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(dlIntent)
    }.recoverCatching {
        val genericIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://media/external/file"), DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(genericIntent)
    }.onFailure {
        Toast.makeText(context, "Location: Downloads/VRKA", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun stateColor(state: JobState): Color = when (state) {
    JobState.DONE -> VrkaTokens.Success
    JobState.FAILED -> VrkaTokens.Error
    JobState.CANCELLED -> VrkaTokens.TextTertiary
    JobState.WAITING_FOR_USER, JobState.BROWSER_FALLBACK -> VrkaTokens.Warning
    else -> VrkaTokens.AccentLight
}

private fun sourceLabel(url: String): String =
    runCatching { Uri.parse(url).host }.getOrNull().orEmpty().ifBlank { "Untitled download" }
