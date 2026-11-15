package com.example.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceMemoAudioService(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentOutputFile: File? = null
    private var recordingStartTimeMs: Long = 0L

    val isRecording: Boolean
        get() = mediaRecorder != null

    fun getVoiceMemosDirectory(): File {
        val dir = File(context.filesDir, "voice_memos")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun startRecording(studentAdmissionNo: String): Result<File> {
        return try {
            stopPlayback()
            releaseRecorder()

            val safeAdm = studentAdmissionNo.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val outFile = File(getVoiceMemosDirectory(), "memo_${safeAdm}_$timestamp.m4a")

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(22_050)
                setMaxDuration(120_000) // Short behavioral memo cap: 2 minutes
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = outFile
            recordingStartTimeMs = System.currentTimeMillis()
            Result.success(outFile)
        } catch (e: Exception) {
            releaseRecorder()
            currentOutputFile?.delete()
            currentOutputFile = null
            Result.failure(e)
        }
    }

    fun stopRecording(): Pair<File?, Int> {
        val outFile = currentOutputFile
        val elapsedSec = (((System.currentTimeMillis() - recordingStartTimeMs) / 1000L).toInt()).coerceAtLeast(1)
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
            // Can happen on very short clips (< 300ms) or headless devices
        } finally {
            releaseRecorder()
            currentOutputFile = null
            recordingStartTimeMs = 0L
        }
        return if (outFile != null && outFile.exists() && outFile.length() > 0L) {
            Pair(outFile, elapsedSec)
        } else {
            outFile?.delete()
            Pair(null, 0)
        }
    }

    fun cancelRecording() {
        val outFile = currentOutputFile
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        } finally {
            releaseRecorder()
            currentOutputFile = null
            recordingStartTimeMs = 0L
            outFile?.delete()
        }
    }

    fun startPlayback(
        filePath: String,
        onCompletion: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            stopPlayback()
            val file = File(filePath)
            if (!file.exists()) {
                onError("Audio clip file not found.")
                return
            }
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    stopPlayback()
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    onError("Unable to play voice memo.")
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            stopPlayback()
            onError(e.localizedMessage ?: "Playback failed")
        }
    }

    fun stopPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
    }

    fun releaseAll() {
        cancelRecording()
        stopPlayback()
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {
        } finally {
            mediaRecorder = null
        }
    }

    companion object {
        fun hasRecordAudioPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun deleteMemoFile(filePath: String) {
            try {
                if (filePath.isNotBlank()) {
                    val f = File(filePath)
                    if (f.exists()) f.delete()
                }
            } catch (_: Exception) {
            }
        }

        fun formatDuration(seconds: Int): String {
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }
}
