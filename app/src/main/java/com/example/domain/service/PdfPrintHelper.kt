package com.example.domain.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import androidx.core.content.FileProvider

object PdfPrintHelper {

    fun printPdf(context: Context, pdfFile: File, jobName: String = pdfFile.nameWithoutExtension): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return false
        if (!pdfFile.exists()) return false

        val adapter = FilePdfPrintDocumentAdapter(pdfFile)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .build()
        printManager.print(jobName, adapter, attributes)
        return true
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Report Card PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun sharePdfToWhatsApp(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(sendIntent)
        } catch (_: Exception) {
            sharePdf(context, pdfFile)
        }
    }
}

private class FilePdfPrintDocumentAdapter(
    private val pdfFile: File
) : PrintDocumentAdapter() {

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder(pdfFile.name)
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
            .build()
        callback.onLayoutFinished(info, newAttributes != oldAttributes)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            FileInputStream(pdfFile).use { input ->
                FileOutputStream(destination.fileDescriptor).use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback.onWriteCancelled()
                            return
                        }
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.localizedMessage ?: "Failed to write PDF print stream")
        }
    }
}
