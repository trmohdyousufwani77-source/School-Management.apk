package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceHeatmap(
    studentName: String,
    attendanceList: List<AttendanceEntity>,
    modifier: Modifier = Modifier
) {
    var selectedDayInfo by remember { mutableStateOf<String?>(null) }

    // Map attendance records by date
    val attendanceMap = remember(attendanceList) {
        attendanceList.associateBy { it.date }
    }

    // Generate past 28 days for clean 4-week calendar heatmap
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val displayFormat = SimpleDateFormat("dd MMM", Locale.US)

    val daysList = remember {
        val list = mutableListOf<Triple<String, String, Boolean>>() // dateStr, label, isSunday
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -27)
        for (i in 0 until 28) {
            val dateStr = dateFormat.format(cal.time)
            val label = displayFormat.format(cal.time)
            val isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            list.add(Triple(dateStr, label, isSunday))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_heatmap_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Heatmap: $studentName",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Past 4 Weeks",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Calendar Heatmap Blocks
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 7
            ) {
                daysList.forEach { (dateStr, label, isSunday) ->
                    val record = attendanceMap[dateStr]
                    val blockColor = when {
                        isSunday -> Color(0xFFEEEEEE)
                        record == null -> Color(0xFFF2F4F8)
                        record.status == "PRESENT" -> Color(0xFF2ECC71) // Bright Green
                        record.status == "ABSENT" -> Color(0xFFE74C3C) // Red
                        record.status == "LATE" -> Color(0xFFF39C12) // Amber
                        record.status == "LEAVE" -> Color(0xFF3498DB) // Blue
                        else -> Color(0xFFBDC3C7)
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(blockColor)
                            .border(
                                width = 1.dp,
                                color = if (selectedDayInfo?.startsWith(dateStr) == true) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                val statusText = when {
                                    isSunday -> "Sunday / Holiday"
                                    record != null -> "${record.status} ${if (record.remarks.isNotEmpty()) "(${record.remarks})" else ""}"
                                    else -> "No Record"
                                }
                                selectedDayInfo = "$dateStr: $statusText"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.take(2),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (blockColor == Color(0xFFEEEEEE) || blockColor == Color(0xFFF2F4F8)) Color.Gray else Color.White
                        )
                    }
                }
            }

            if (selectedDayInfo != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedDayInfo ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF2ECC71), label = "Present")
                LegendItem(color = Color(0xFFE74C3C), label = "Absent")
                LegendItem(color = Color(0xFFF39C12), label = "Late")
                LegendItem(color = Color(0xFF3498DB), label = "Leave")
                LegendItem(color = Color(0xFFEEEEEE), label = "Holiday")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(color, CircleShape)
                .border(0.5.dp, Color.LightGray, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
