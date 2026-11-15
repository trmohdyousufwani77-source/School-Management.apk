package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.core.content.FileProvider
import com.example.ui.components.PdfPreviewDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DocumentManagerScreen() {
    val context = LocalContext.current
    var filesList by remember { mutableStateOf<List<File>>(emptyList()) }
    var previewingPdfFile by remember { mutableStateOf<File?>(null) }

    fun refreshFiles() {
        val reportsDir = File(context.filesDir, "reports")
        val backupsDir = File(context.filesDir, "backups")
        val list = mutableListOf<File>()
        if (reportsDir.exists()) {
            reportsDir.listFiles()?.let { list.addAll(it) }
        }
        if (backupsDir.exists()) {
            backupsDir.listFiles()?.let { list.addAll(it) }
        }
        list.sortByDescending { it.lastModified() }
        filesList = list
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Generated Documents & Archives",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${filesList.size} files available locally on device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { refreshFiles() }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        if (filesList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No documents generated yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Generate PDF Report Cards or export Attendance Registers from the respective screens.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        } else {
            items(filesList, key = { it.absolutePath }) { file ->
                val isPdf = file.name.endsWith(".pdf", ignoreCase = true)
                val isCsv = file.name.endsWith(".csv", ignoreCase = true)
                val isDb = file.name.endsWith(".db", ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isPdf) { previewingPdfFile = file }
                        .testTag("doc_file_${file.name.take(15)}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when {
                                        isPdf -> Color(0xFFD32F2F).copy(alpha = 0.15f)
                                        isCsv -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                                        else -> Color(0xFF1976D2).copy(alpha = 0.15f)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isPdf -> Icons.Default.PictureAsPdf
                                    isCsv -> Icons.Default.TableChart
                                    else -> Icons.Default.Description
                                },
                                contentDescription = null,
                                tint = when {
                                    isPdf -> Color(0xFFD32F2F)
                                    isCsv -> Color(0xFF2E7D32)
                                    else -> Color(0xFF1976D2)
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${file.length() / 1024} KB | ${dateFormat.format(Date(file.lastModified()))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // In-App PDF Preview Action
                        if (isPdf) {
                            IconButton(
                                onClick = { previewingPdfFile = file },
                                modifier = Modifier.testTag("preview_pdf_file_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Preview PDF",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Share Action
                        IconButton(
                            onClick = {
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (isPdf) "application/pdf" else "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                            },
                            modifier = Modifier.testTag("share_file_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // WhatsApp Action
                        IconButton(
                            onClick = {
                                val uri: Uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = if (isPdf) "application/pdf" else "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    setPackage("com.whatsapp")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(sendIntent)
                                } catch (e: Exception) {
                                    // Fallback to share chooser
                                    context.startActivity(Intent.createChooser(sendIntent, "Share via WhatsApp"))
                                }
                            },
                            modifier = Modifier.testTag("whatsapp_file_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "WhatsApp",
                                tint = Color(0xFF25D366)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (previewingPdfFile != null) {
        PdfPreviewDialog(
            pdfFile = previewingPdfFile!!,
            onDismiss = { previewingPdfFile = null }
        )
    }
}
