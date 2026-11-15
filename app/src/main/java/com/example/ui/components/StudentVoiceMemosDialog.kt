package com.example.ui.components

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentVoiceMemoEntity
import com.example.domain.service.VoiceMemoAudioService
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentVoiceMemosDialog(
    student: StudentEntity,
    memos: List<StudentVoiceMemoEntity>,
    onSaveMemo: (tag: String, noteText: String, filePath: String, durationSeconds: Int) -> Unit,
    onDeleteMemo: (StudentVoiceMemoEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val audioService = remember(context) { VoiceMemoAudioService(context) }

    var hasMicPermission by remember {
        mutableStateOf(VoiceMemoAudioService.hasRecordAudioPermission(context))
    }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var playingMemoId by remember { mutableStateOf<Long?>(null) }
    var selectedTag by remember { mutableStateOf("Behavioral") }
    var noteText by remember { mutableStateOf("") }
    var recorderError by remember { mutableStateOf<String?>(null) }

    val tags = listOf("Behavioral", "Academic", "Attendance", "Parent Call", "Strength")
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }

    fun beginRecording() {
        recorderError = null
        val startResult = audioService.startRecording(student.admissionNumber)
        if (startResult.isSuccess) {
            isRecording = true
            elapsedSeconds = 0
        } else {
            val errMsg = startResult.exceptionOrNull()?.localizedMessage ?: "Microphone unavailable"
            recorderError = "Unable to start microphone: $errMsg"
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            beginRecording()
        } else {
            recorderError = AppStrings.get("mic_permission_required", lang)
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                delay(1000L)
                elapsedSeconds += 1
                if (elapsedSeconds >= 120) {
                    val (file, duration) = audioService.stopRecording()
                    isRecording = false
                    if (file != null) {
                        onSaveMemo(selectedTag, noteText.trim(), file.absolutePath, duration)
                        noteText = ""
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioService.releaseAll()
        }
    }

    AlertDialog(
        onDismissRequest = {
            audioService.releaseAll()
            onDismiss()
        },
        modifier = Modifier.testTag("student_voice_memos_dialog"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = AppStrings.get("voice_memos_title", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${student.nameEn} • Class ${student.className}-${student.section}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Observation Tag Selector
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { if (!isRecording) selectedTag = tag },
                            label = { Text(tag, fontSize = 11.sp) },
                            modifier = Modifier.testTag("voice_memo_tag_$tag")
                        )
                    }
                }

                // Short observation note text input
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(AppStrings.get("memo_note_label", lang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_memo_note_input")
                )

                // Microphone Recording Bar
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isRecording) Color(0xFFFDECEA) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.GraphicEq else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isRecording) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isRecording) {
                                        "Recording... ${VoiceMemoAudioService.formatDuration(elapsedSeconds)}"
                                    } else if (!hasMicPermission) {
                                        AppStrings.get("mic_permission_required", lang)
                                    } else {
                                        "Tap to record short voice note (max 2m)"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isRecording) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (isRecording) {
                                    val (file, duration) = audioService.stopRecording()
                                    isRecording = false
                                    if (file != null) {
                                        onSaveMemo(selectedTag, noteText.trim(), file.absolutePath, duration)
                                        noteText = ""
                                    } else {
                                        recorderError = "Recording too short or empty audio captured."
                                    }
                                } else {
                                    if (!VoiceMemoAudioService.hasRecordAudioPermission(context)) {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        hasMicPermission = true
                                        beginRecording()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecording) Color(0xFFC62828) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("voice_memo_record_btn")
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRecording) AppStrings.get("stop_recording", lang)
                                else if (!hasMicPermission) AppStrings.get("grant_mic_permission", lang)
                                else AppStrings.get("record_voice_memo", lang),
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (recorderError != null) {
                    Text(
                        text = recorderError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // Saved Voice Memos List
                if (memos.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AppStrings.get("no_voice_memos", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .testTag("voice_memos_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(memos, key = { it.id }) { memo ->
                            val isPlaying = playingMemoId == memo.id
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_memo_card_${memo.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                audioService.stopPlayback()
                                                playingMemoId = null
                                            } else {
                                                playingMemoId = memo.id
                                                audioService.startPlayback(
                                                    filePath = memo.filePath,
                                                    onCompletion = { playingMemoId = null },
                                                    onError = { msg ->
                                                        playingMemoId = null
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        },
                                        modifier = Modifier.testTag("play_voice_memo_${memo.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Stop Memo" else "Play Memo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = memo.tag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = VoiceMemoAudioService.formatDuration(memo.durationSeconds),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (memo.noteText.isNotBlank()) {
                                            Text(
                                                text = memo.noteText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            text = "${dateFormatter.format(Date(memo.recordedTimestamp))} • ${memo.recordedBy}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (isPlaying) {
                                                audioService.stopPlayback()
                                                playingMemoId = null
                                            }
                                            onDeleteMemo(memo)
                                        },
                                        modifier = Modifier.testTag("delete_voice_memo_${memo.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Voice Memo",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    audioService.releaseAll()
                    onDismiss()
                },
                modifier = Modifier.testTag("voice_memos_close_btn")
            ) {
                Text("Done")
            }
        }
    )
}
