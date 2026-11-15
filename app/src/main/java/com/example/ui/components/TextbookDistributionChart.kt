package com.example.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TextbookAllocationEntity
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage

@Composable
fun TextbookDistributionChart(
    allocations: List<TextbookAllocationEntity>,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    // Group allocations by class 1 to 8
    val classStats = (1..8).map { cls ->
        val classItems = allocations.filter { it.className == cls }
        val distributed = classItems.count { it.status == "RECEIVED" }
        val pending = classItems.size - distributed
        Triple(cls, distributed, pending)
    }

    val totalDistributed = allocations.count { it.status == "RECEIVED" }
    val totalPending = allocations.size - totalDistributed

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("textbook_distribution_chart_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.get("textbook_dist_status", lang),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${AppStrings.get("distributed_label", lang)}: $totalDistributed | ${AppStrings.get("pending_label", lang)}: $totalPending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF27AE60), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("given_label", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFE67E22), CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(AppStrings.get("pending_label", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bars for Class 1 to 8
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                classStats.forEach { (cls, dist, pend) ->
                    val total = dist + pend
                    val distFraction = if (total > 0) dist.toFloat() / total.toFloat() else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${AppStrings.get("class_prefix", lang)} $cls",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(64.dp)
                        )

                        // Stacked horizontal progress bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFFF0F2F5))
                        ) {
                            if (total > 0) {
                                Row(modifier = Modifier.matchParentSize()) {
                                    if (distFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(distFraction)
                                                .height(14.dp)
                                                .background(Color(0xFF27AE60))
                                        )
                                    }
                                    if (1f - distFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f - distFraction)
                                                .height(14.dp)
                                                .background(Color(0xFFE67E22))
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$dist/$total",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }
            }
        }
    }
}
