package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage

@Composable
fun AttendanceCircularProgress(
    totalEnrolled: Int,
    presentCount: Int,
    absentCount: Int,
    lateCount: Int,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val percentage = if (totalEnrolled > 0) (presentCount.toFloat() / totalEnrolled.toFloat()) * 100f else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = percentage / 100f,
        animationSpec = tween(durationMillis = 800),
        label = "AttendanceProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_circular_progress_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                Canvas(modifier = Modifier.size(100.dp)) {
                    val stroke = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                    // Background track
                    drawCircle(
                        color = Color(0xFFE0E0E0),
                        style = stroke
                    )
                    // Animated Arc
                    val sweepAngle = animatedProgress * 360f
                    drawArc(
                        color = if (percentage >= 75f) Color(0xFF1E824C) else Color(0xFFD35400),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${percentage.toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AppStrings.get("attendance_rate", lang),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Stats Breakdown
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatRow(
                    label = AppStrings.get("total_enrolled", lang),
                    count = totalEnrolled,
                    color = MaterialTheme.colorScheme.primary
                )
                StatRow(
                    label = AppStrings.get("present_count", lang),
                    count = presentCount,
                    color = Color(0xFF27AE60)
                )
                StatRow(
                    label = AppStrings.get("absent_count", lang),
                    count = absentCount,
                    color = Color(0xFFE74C3C)
                )
                if (lateCount > 0) {
                    StatRow(
                        label = AppStrings.get("late_count", lang),
                        count = lateCount,
                        color = Color(0xFFF39C12)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
