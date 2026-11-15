package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class DailyAttendanceTrendPoint(
    val dateStr: String,
    val dayShortLabel: String,
    val dayNumberLabel: String,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val leaveCount: Int,
    val markedTotal: Int,
    val presentRate: Float,
    val absentRate: Float
)

object WeeklyTrendCalculator {
    fun buildLast7SchoolDays(
        allStudents: List<StudentEntity>,
        attendanceRecords: List<AttendanceEntity>,
        referenceDate: String,
        language: AppLanguage = AppLanguage.ENGLISH
    ): List<DailyAttendanceTrendPoint> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val locale = if (language == AppLanguage.URDU) Locale("ur") else Locale.US
        val dayNameFormat = SimpleDateFormat("EEE", locale)
        val dayNumFormat = SimpleDateFormat("dd", Locale.US)

        val endCal = Calendar.getInstance().apply {
            time = try {
                dateFormat.parse(referenceDate) ?: Date()
            } catch (_: Exception) {
                Date()
            }
        }

        // Collect 7 school days (skipping Sundays) ending at referenceDate
        val dates = mutableListOf<Date>()
        val scanCal = endCal.clone() as Calendar
        while (dates.size < 7) {
            if (scanCal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                dates.add(0, scanCal.time)
            }
            scanCal.add(Calendar.DAY_OF_YEAR, -1)
        }

        val byDate = attendanceRecords.groupBy { it.date }
        val enrolledCount = allStudents.size

        return dates.map { day ->
            val dateStr = dateFormat.format(day)
            val records = byDate[dateStr].orEmpty()
            val present = records.count { it.status == "PRESENT" }
            val absent = records.count { it.status == "ABSENT" }
            val late = records.count { it.status == "LATE" }
            val leave = records.count { it.status == "LEAVE" }
            val markedTotal = records.size
            val denom = maxOf(enrolledCount, markedTotal)

            val presentRate = if (markedTotal > 0 && denom > 0) {
                (((present + late).toFloat() / denom.toFloat()) * 100f).coerceIn(0f, 100f)
            } else {
                0f
            }
            val absentRate = if (markedTotal > 0 && denom > 0) {
                ((absent.toFloat() / denom.toFloat()) * 100f).coerceIn(0f, 100f)
            } else {
                0f
            }

            DailyAttendanceTrendPoint(
                dateStr = dateStr,
                dayShortLabel = dayNameFormat.format(day),
                dayNumberLabel = dayNumFormat.format(day),
                presentCount = present,
                absentCount = absent,
                lateCount = late,
                leaveCount = leave,
                markedTotal = markedTotal,
                presentRate = presentRate,
                absentRate = absentRate
            )
        }
    }
}

@Composable
fun WeeklyAttendanceTrendChart(
    allStudents: List<StudentEntity>,
    attendanceRecords: List<AttendanceEntity>,
    referenceDate: String,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val points = remember(allStudents, attendanceRecords, referenceDate, lang) {
        WeeklyTrendCalculator.buildLast7SchoolDays(
            allStudents = allStudents,
            attendanceRecords = attendanceRecords,
            referenceDate = referenceDate,
            language = lang
        )
    }

    var selectedIndex by remember(points) {
        mutableIntStateOf((points.size - 1).coerceAtLeast(0))
    }

    val activePoints = points.filter { it.markedTotal > 0 }
    val avgAttendancePct = if (activePoints.isNotEmpty()) {
        activePoints.map { it.presentRate }.average().roundToInt()
    } else {
        0
    }
    val peakAbsentPoint = activePoints.maxByOrNull { it.absentRate }

    val presentColor = Color(0xFF1E824C)
    val absentColor = Color(0xFFD32F2F)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_attendance_trend_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Series Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = AppStrings.get("weekly_attendance_trend", lang),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.get("weekly_trend_subtitle", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(presentColor, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.get("present_rate_label", lang),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(absentColor, CircleShape))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = AppStrings.get("absent_rate_label", lang),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Summary KPI Insight Pills (Weekly Avg & Peak Absenteeism)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE7F6EE),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weekly_avg_attendance_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("avg_weekly_attendance", lang),
                            fontSize = 11.sp,
                            color = Color(0xFF0A5C36),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$avgAttendancePct%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A5C36)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFDECEA),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("weekly_peak_absent_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.get("peak_absent_day", lang),
                                fontSize = 10.sp,
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (peakAbsentPoint != null) {
                                "${peakAbsentPoint.dayShortLabel} ${peakAbsentPoint.absentRate.roundToInt()}%"
                            } else {
                                "0%"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }

            if (activePoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.get("no_attendance_week", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                // Line Chart Area (Y-Axis Labels + Multi-Series Smooth Line Canvas)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(165.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .width(34.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                            Text(
                                text = tick,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 10.dp, horizontal = 8.dp)
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    if (points.size > 1 && size.width > 0) {
                                        val stepX = size.width.toFloat() / (points.size - 1)
                                        val nearest = (offset.x / stepX).roundToInt()
                                            .coerceIn(0, points.lastIndex)
                                        selectedIndex = nearest
                                    }
                                }
                            }
                            .testTag("weekly_trend_line_canvas")
                    ) {
                        val width = size.width
                        val height = size.height
                        if (width <= 0f || height <= 0f || points.isEmpty()) return@Canvas

                        // Draw horizontal reference gridlines (0%, 25%, 50%, 75%, 100%)
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        for (step in 0..4) {
                            val y = height - (step / 4f) * height
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (step in 1..3) dashEffect else null
                            )
                        }

                        val count = points.size
                        val stepX = if (count > 1) width / (count - 1) else width

                        fun pointOffsets(selector: (DailyAttendanceTrendPoint) -> Float): List<Offset> {
                            return points.mapIndexed { idx, pt ->
                                val x = if (count > 1) idx * stepX else width / 2f
                                val pct = selector(pt).coerceIn(0f, 100f) / 100f
                                val y = height - (pct * height)
                                Offset(x, y)
                            }
                        }

                        fun drawTrendSeries(offsets: List<Offset>, color: Color, fillAlpha: Float) {
                            if (offsets.isEmpty()) return
                            val linePath = Path().apply {
                                moveTo(offsets.first().x, offsets.first().y)
                                for (i in 0 until offsets.lastIndex) {
                                    val p0 = offsets[i]
                                    val p1 = offsets[i + 1]
                                    val ctrlX = (p0.x + p1.x) / 2f
                                    cubicTo(ctrlX, p0.y, ctrlX, p1.y, p1.x, p1.y)
                                }
                            }

                            val areaPath = Path().apply {
                                addPath(linePath)
                                lineTo(offsets.last().x, height)
                                lineTo(offsets.first().x, height)
                                close()
                            }

                            drawPath(
                                path = areaPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(color.copy(alpha = fillAlpha), Color.Transparent),
                                    startY = 0f,
                                    endY = height
                                )
                            )

                            drawPath(
                                path = linePath,
                                color = color,
                                style = Stroke(
                                    width = 2.8.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )

                            offsets.forEachIndexed { idx, pt ->
                                val isSel = idx == selectedIndex
                                if (isSel) {
                                    drawCircle(
                                        color = color.copy(alpha = 0.22f),
                                        radius = 8.dp.toPx(),
                                        center = pt
                                    )
                                }
                                drawCircle(
                                    color = Color.White,
                                    radius = if (isSel) 5.dp.toPx() else 3.8.dp.toPx(),
                                    center = pt
                                )
                                drawCircle(
                                    color = color,
                                    radius = if (isSel) 3.5.dp.toPx() else 2.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // Highlight selected day vertical guide
                        val selX = if (count > 1) selectedIndex * stepX else width / 2f
                        drawLine(
                            color = Color(0xFF90A4AE).copy(alpha = 0.45f),
                            start = Offset(selX, 0f),
                            end = Offset(selX, height),
                            strokeWidth = 1.2.dp.toPx()
                        )

                        val presentOffsets = pointOffsets { it.presentRate }
                        val absentOffsets = pointOffsets { it.absentRate }

                        drawTrendSeries(presentOffsets, presentColor, fillAlpha = 0.22f)
                        drawTrendSeries(absentOffsets, absentColor, fillAlpha = 0.16f)
                    }
                }

                // Interactive X-Axis Day Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 38.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEachIndexed { index, pt ->
                        val isSelected = index == selectedIndex
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("trend_day_chip_$index")
                        ) {
                            Text(
                                text = pt.dayShortLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = pt.dayNumberLabel,
                                fontSize = 9.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Selected Day Detail Strip
                points.getOrNull(selectedIndex)?.let { selected ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trend_selected_day_detail")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selected.dayShortLabel} (${selected.dateStr})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selected.presentRate.roundToInt()}% P (${selected.presentCount + selected.lateCount}) · ${selected.absentRate.roundToInt()}% A (${selected.absentCount})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selected.absentRate >= 20f) absentColor else presentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
