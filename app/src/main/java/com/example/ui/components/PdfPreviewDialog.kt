package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.service.PdfPrintHelper
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfPreviewDialog(
    pdfFile: File,
    onDismiss: () -> Unit,
    onWhatsAppClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current

    var pageCount by remember(pdfFile.absolutePath) { mutableIntStateOf(0) }
    var currentPageIndex by remember(pdfFile.absolutePath) { mutableIntStateOf(0) }
    var renderedBitmap by remember(pdfFile.absolutePath) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pdfFile.absolutePath) { mutableStateOf(true) }
    var renderError by remember(pdfFile.absolutePath) { mutableStateOf<String?>(null) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(pdfFile.absolutePath, currentPageIndex) {
        isLoading = true
        renderError = null
        val result = withContext(Dispatchers.IO) {
            renderPdfPageToBitmap(pdfFile, currentPageIndex)
        }
        pageCount = result.pageCount
        renderedBitmap = result.bitmap
        renderError = result.errorMessage
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("in_app_pdf_preview_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = AppStrings.get("pdf_preview_title", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = pdfFile.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("pdf_preview_close_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close preview")
                        }
                    }
                }

                // Zoom & Pagination Controls Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("pdf_prev_page_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous page"
                            )
                        }
                        val displayPage = if (pageCount > 0) currentPageIndex + 1 else 1
                        val displayTotal = pageCount.coerceAtLeast(1)
                        Text(
                            text = "${AppStrings.get("pdf_page_prefix", lang)} $displayPage ${AppStrings.get("pdf_of_label", lang)} $displayTotal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.testTag("pdf_page_indicator")
                        )
                        IconButton(
                            onClick = { if (currentPageIndex < pageCount - 1) currentPageIndex++ },
                            enabled = currentPageIndex < pageCount - 1,
                            modifier = Modifier.testTag("pdf_next_page_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next page"
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f) },
                            enabled = zoomScale > 1f,
                            modifier = Modifier.testTag("pdf_zoom_out_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ZoomOut, contentDescription = "Zoom out")
                        }
                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                            enabled = zoomScale < 2.5f,
                            modifier = Modifier.testTag("pdf_zoom_in_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ZoomIn, contentDescription = "Zoom in")
                        }
                    }
                }

                // Rendered PDF Page Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFFE2E8F0))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(modifier = Modifier.testTag("pdf_preview_loading"))
                        }
                        renderedBitmap != null -> {
                            val bmp = renderedBitmap!!
                            val vScroll = rememberScrollState()
                            val hScroll = rememberScrollState()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(vScroll)
                                    .horizontalScroll(hScroll),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Surface(
                                    shadowElevation = 6.dp,
                                    color = Color.White,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Rendered PDF Page ${currentPageIndex + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier
                                            .width((330 * zoomScale).dp)
                                            .testTag("pdf_rendered_page_image")
                                    )
                                }
                            }
                        }
                        else -> {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .testTag("pdf_preview_fallback_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = pdfFile.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Size: ${(pdfFile.length() / 1024).coerceAtLeast(1)} KB • Ready to Print or Share",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (!renderError.isNullOrBlank()) {
                                        Text(
                                            text = renderError!!,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action Bar (Print, Share, WhatsApp)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { PdfPrintHelper.printPdf(context, pdfFile) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_preview_print_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("pdf_print_btn", lang), fontSize = 12.sp)
                        }

                        Button(
                            onClick = { PdfPrintHelper.sharePdf(context, pdfFile) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_preview_share_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(AppStrings.get("pdf_share_btn", lang), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (onWhatsAppClick != null) {
                                    onWhatsAppClick()
                                } else {
                                    PdfPrintHelper.sharePdfToWhatsApp(context, pdfFile)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pdf_preview_whatsapp_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

data class PdfRenderPageResult(
    val pageCount: Int,
    val bitmap: Bitmap?,
    val errorMessage: String? = null
)

fun renderPdfPageToBitmap(file: File, pageIndex: Int): PdfRenderPageResult {
    if (!file.exists()) {
        return PdfRenderPageResult(0, null, "PDF file not found.")
    }
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    return try {
        pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(pfd)
        val count = renderer.pageCount
        if (count <= 0) {
            PdfRenderPageResult(0, null, "PDF has no pages.")
        } else {
            val safeIndex = pageIndex.coerceIn(0, count - 1)
            val page = renderer.openPage(safeIndex)
            try {
                // Render at 2x points resolution (1190x1684 for A4) for crisp in-app zooming
                val targetWidth = (page.width * 2).coerceIn(595, 1600)
                val targetHeight = (page.height * 2).coerceIn(842, 2200)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                PdfRenderPageResult(count, bitmap, null)
            } finally {
                page.close()
            }
        }
    } catch (e: Throwable) {
        PdfRenderPageResult(
            pageCount = 1,
            bitmap = null,
            errorMessage = e.localizedMessage ?: "Preview rasterizer unavailable in this environment."
        )
    } finally {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
    }
}
