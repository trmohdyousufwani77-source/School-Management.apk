package com.example.domain.service

import com.example.data.local.entities.StudentEntity
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

object CsvStudentParserService {

    data class CsvRowError(
        val rowNumber: Int,
        val rawLine: String,
        val reason: String
    )

    data class CsvParseResult(
        val validStudents: List<StudentEntity>,
        val errors: List<CsvRowError>,
        val totalRowsProcessed: Int
    ) {
        val hasErrors: Boolean get() = errors.isNotEmpty()
    }

    private val canonicalHeaders = mapOf(
        "admission_number" to "admissionNumber",
        "admission_no" to "admissionNumber",
        "adm_no" to "admissionNumber",
        "roll_number" to "rollNumber",
        "roll_no" to "rollNumber",
        "roll" to "rollNumber",
        "name_en" to "nameEn",
        "student_name" to "nameEn",
        "name" to "nameEn",
        "name_ur" to "nameUr",
        "urdu_name" to "nameUr",
        "class_name" to "className",
        "class" to "className",
        "grade" to "className",
        "section" to "section",
        "sec" to "section",
        "gender" to "gender",
        "dob" to "dob",
        "date_of_birth" to "dob",
        "category" to "category",
        "father_name" to "fatherName",
        "guardian_name" to "fatherName",
        "mother_name" to "motherName",
        "primary_contact" to "primaryContact",
        "phone" to "primaryContact",
        "contact" to "primaryContact",
        "whatsapp_number" to "whatsAppNumber",
        "whatsapp" to "whatsAppNumber",
        "residential_address" to "residentialAddress",
        "address" to "residentialAddress",
        "academic_session" to "academicSession",
        "session" to "academicSession"
    )

    fun parse(inputStream: InputStream): CsvParseResult {
        val content = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
        return parse(content)
    }

    fun parse(csvContent: String): CsvParseResult {
        val normalizedContent = csvContent.removePrefix("\uFEFF").trim()
        if (normalizedContent.isEmpty()) {
            return CsvParseResult(emptyList(), listOf(CsvRowError(1, "", "CSV file is empty.")), 0)
        }

        val lines = splitCsvLines(normalizedContent).filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            return CsvParseResult(emptyList(), listOf(CsvRowError(1, "", "No rows found in CSV.")), 0)
        }

        val headerTokens = parseCsvLine(lines.first()).map { it.trim().lowercase().replace(" ", "_") }
        val headerMap = mutableMapOf<String, Int>()
        headerTokens.forEachIndexed { idx, token ->
            val canonical = canonicalHeaders[token]
            if (canonical != null && !headerMap.containsKey(canonical)) {
                headerMap[canonical] = idx
            }
        }

        // Verify required columns exist
        val requiredFields = listOf("admissionNumber", "rollNumber", "nameEn", "className")
        val missingRequired = requiredFields.filter { !headerMap.containsKey(it) }
        if (missingRequired.isNotEmpty()) {
            return CsvParseResult(
                validStudents = emptyList(),
                errors = listOf(
                    CsvRowError(
                        rowNumber = 1,
                        rawLine = lines.first(),
                        reason = "Missing required header column(s): ${missingRequired.joinToString(", ")}"
                    )
                ),
                totalRowsProcessed = 0
            )
        }

        val validStudents = mutableListOf<StudentEntity>()
        val errors = mutableListOf<CsvRowError>()
        val seenAdmissionNumbers = mutableSetOf<String>()

        for (i in 1 until lines.size) {
            val rowNumber = i + 1
            val rawLine = lines[i]
            val fields = parseCsvLine(rawLine)

            fun field(key: String, default: String = ""): String {
                val idx = headerMap[key] ?: return default
                return fields.getOrNull(idx)?.trim().orEmpty().ifEmpty { default }
            }

            val admissionNumber = field("admissionNumber")
            val rollRaw = field("rollNumber")
            val nameEn = field("nameEn")
            val nameUr = field("nameUr", nameEn)
            val classRaw = field("className")
            val section = field("section", "A").uppercase().take(3).ifEmpty { "A" }
            val gender = field("gender", "Male")
            val dob = field("dob", "2016-01-01")
            val category = field("category", "GENERAL").uppercase()
            val fatherName = field("fatherName", "Guardian")
            val motherName = field("motherName", "Mother")
            val primaryContact = field("primaryContact")
            val whatsAppNumber = field("whatsAppNumber", primaryContact)
            val address = field("residentialAddress", "Sopore")
            val session = field("academicSession", "2026-2027")

            val rollNumber = rollRaw.toIntOrNull()
            val className = classRaw.toIntOrNull()

            val rowFailure = when {
                admissionNumber.isBlank() -> "Admission number is required."
                !seenAdmissionNumbers.add(admissionNumber.uppercase()) -> "Duplicate admission number '$admissionNumber' in CSV."
                nameEn.isBlank() -> "Student English name is required."
                rollNumber == null || rollNumber <= 0 -> "Invalid roll number '$rollRaw'."
                className == null || className !in 1..8 -> "Class must be between 1 and 8 (found '$classRaw')."
                primaryContact.isBlank() -> "Primary guardian contact is required."
                else -> null
            }

            if (rowFailure != null) {
                errors.add(CsvRowError(rowNumber = rowNumber, rawLine = rawLine, reason = rowFailure))
                continue
            }

            val tokenSuffix = (1000..9999).random()
            validStudents.add(
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = admissionNumber,
                    rollNumber = rollNumber!!,
                    nameEn = nameEn,
                    nameUr = nameUr,
                    className = className!!,
                    section = section,
                    gender = gender,
                    dob = dob,
                    category = category,
                    fatherName = fatherName,
                    motherName = motherName,
                    primaryContact = primaryContact,
                    whatsAppNumber = whatsAppNumber,
                    residentialAddress = address,
                    academicSession = session,
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-CSV-$admissionNumber",
                    maskedAadhaar = "XXXX-XXXX-$tokenSuffix",
                    aadhaarStatus = "PENDING",
                    consentRecorded = false
                )
            )
        }

        return CsvParseResult(
            validStudents = validStudents,
            errors = errors,
            totalRowsProcessed = (lines.size - 1).coerceAtLeast(0)
        )
    }

    fun generateSampleCsvTemplate(): String {
        return """
            admission_number,roll_number,name_en,name_ur,class_name,section,gender,dob,category,father_name,mother_name,primary_contact,whatsapp_number,residential_address,academic_session
            ADM-2026-401,1,Ayesha Tariq,عائشہ طارق,4,A,Female,2017-04-12,GENERAL,Tariq Ahmad Mir,Rukhsana,9419088101,9419088101,"Model Town, Sopore",2026-2027
            ADM-2026-402,2,Bilal Yousuf,بلال یوسف,4,A,Male,2017-09-21,OBC,Yousuf Bhat,Shaheena,9419088102,9419088102,"Noorbagh, Sopore",2026-2027
            ADM-2026-701,1,Hadiya Showkat,ہادیہ شوکت,7,A,Female,2014-02-15,GENERAL,Showkat Dar,Fehmida,9419088103,9419088103,"Chinkipora, Sopore",2026-2027
        """.trimIndent()
    }

    private fun splitCsvLines(content: String): List<String> {
        val lines = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < content.length) {
            val ch = content[i]
            when {
                ch == '"' -> {
                    inQuotes = !inQuotes
                    current.append(ch)
                }
                (ch == '\n' || ch == '\r') && !inQuotes -> {
                    if (ch == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    lines.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
            i++
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
        return lines
    }

    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
