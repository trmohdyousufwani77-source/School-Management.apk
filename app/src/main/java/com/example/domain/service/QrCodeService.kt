package com.example.domain.service

import android.graphics.Bitmap
import android.graphics.Color
import com.example.data.local.entities.StudentEntity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.util.Locale

object QrCodeService {

    const val PREFIX = "SSM_STU"

    data class ParsedStudentQr(
        val rawValue: String,
        val admissionNumber: String?,
        val uuid: String?
    )

    fun encodeStudentQrPayload(student: StudentEntity): String {
        return "$PREFIX|${student.admissionNumber}|${student.uuid}"
    }

    fun parseStudentQrPayload(rawValue: String): ParsedStudentQr? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        // 1. Structured format: SSM_STU|<admissionNumber>|<uuid> or SSM_STU|<admissionNumber>
        if (trimmed.startsWith("$PREFIX|", ignoreCase = true)) {
            val parts = trimmed.split("|")
            val adm = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            val uuid = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            return ParsedStudentQr(rawValue = trimmed, admissionNumber = adm, uuid = uuid)
        }

        // 2. JSON format: {"admissionNumber":"ADM-2026-601","uuid":"..."}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                val obj = JSONObject(trimmed)
                val adm = obj.optString("admissionNumber", "").ifBlank {
                    obj.optString("adm", "")
                }.takeIf { it.isNotBlank() }
                val uuid = obj.optString("uuid", "").ifBlank {
                    obj.optString("id", "")
                }.takeIf { it.isNotBlank() }
                if (adm != null || uuid != null) {
                    return ParsedStudentQr(rawValue = trimmed, admissionNumber = adm, uuid = uuid)
                }
            } catch (_: Exception) {
                // Fall through to raw identifier parsing
            }
        }

        // 3. Direct admission number or UUID
        return ParsedStudentQr(
            rawValue = trimmed,
            admissionNumber = trimmed,
            uuid = trimmed
        )
    }

    fun findMatchingStudent(rawValue: String, students: List<StudentEntity>): StudentEntity? {
        val parsed = parseStudentQrPayload(rawValue) ?: return null
        val targetUuid = parsed.uuid?.lowercase(Locale.US)
        val targetAdm = parsed.admissionNumber?.lowercase(Locale.US)
        val targetRaw = parsed.rawValue.lowercase(Locale.US)

        return students.firstOrNull { student ->
            (targetUuid != null && student.uuid.lowercase(Locale.US) == targetUuid) ||
                (targetAdm != null && student.admissionNumber.lowercase(Locale.US) == targetAdm) ||
                student.uuid.lowercase(Locale.US) == targetRaw ||
                student.admissionNumber.lowercase(Locale.US) == targetRaw
        }
    }

    /**
     * Generates a crisp QR Code Bitmap for a student's ID card using ZXing.
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                val offset = y * sizePx
                for (x in 0 until sizePx) {
                    pixels[offset + x] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            bitmap
        } catch (_: Exception) {
            null
        }
    }
}
