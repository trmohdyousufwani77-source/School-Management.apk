package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.domain.service.AttendanceAlertType
import com.example.domain.service.WhatsAppService
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceWhatsAppAlertDialog(
    selectedStudents: List<StudentEntity>,
    selectedDate: String,
    initialAlertType: AttendanceAlertType = AttendanceAlertType.ABSENCE,
    onDismiss: () -> Unit,
    onSendAlert: (alertType: AttendanceAlertType, message: String) -> Unit
) {
    val lang = LocalAppLanguage.current
    var selectedAlertType by remember { mutableStateOf(initialAlertType) }
    var messageText by remember(selectedStudents, selectedDate, lang) {
        mutableStateOf(
            WhatsAppService.buildAttendanceAlertMessage(
                alertType = initialAlertType,
                date = selectedDate,
                students = selectedStudents,
                language = lang
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("whatsapp_alert_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = Color(0xFF128C7E)
            )
        },
        title = {
            Text(
                text = AppStrings.get("whatsapp_alert_title", lang),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Selected Recipients Summary Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_recipients_summary")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${AppStrings.get("recipients_label", lang)} (${selectedStudents.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedStudents.joinToString(", ") { student ->
                                val name = if (lang == AppLanguage.URDU) student.nameUr else student.nameEn
                                val phone = student.whatsAppNumber.ifBlank { student.primaryContact }
                                "$name ($phone)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Specific Alert Type Preset Chips
                Text(
                    text = AppStrings.get("alert_type_label", lang),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AttendanceAlertType.entries.forEach { alertType ->
                        val isSelected = selectedAlertType == alertType
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAlertType = alertType
                                messageText = WhatsAppService.buildAttendanceAlertMessage(
                                    alertType = alertType,
                                    date = selectedDate,
                                    students = selectedStudents,
                                    language = lang
                                )
                            },
                            label = {
                                Text(
                                    text = AppStrings.get(alertType.stringKey, lang),
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD8F3DC),
                                selectedLabelColor = Color(0xFF08331A)
                            ),
                            modifier = Modifier.testTag("alert_type_chip_${alertType.name}")
                        )
                    }
                }

                // Editable Alert Message Preview
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(AppStrings.get("alert_message_label", lang)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("whatsapp_alert_message_input"),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendAlert(selectedAlertType, messageText)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF128C7E),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_whatsapp_alert_send_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(AppStrings.get("send_whatsapp_alert", lang))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_whatsapp_alert_btn")
            ) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}
