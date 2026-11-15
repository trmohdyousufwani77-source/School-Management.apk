package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage

data class CoachStep(
    val titleEn: String,
    val titleUr: String,
    val descriptionEn: String,
    val descriptionUr: String,
    val icon: ImageVector
)

@Composable
fun TeacherCoachOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val lang = LocalAppLanguage.current
    var currentStepIndex by remember { mutableIntStateOf(0) }

    val steps = listOf(
        CoachStep(
            titleEn = "Step 1: Class & Section Allocation",
            titleUr = "مرحلہ ۱: جماعت اور سیکشن کا انتخاب",
            descriptionEn = "Use the top selector to pick your assigned class (1–8) and section (A/B/C). All records sync offline automatically.",
            descriptionUr = "اوپر دیے گئے ڈراپ ڈاؤن سے اپنی مقررہ جماعت اور سیکشن منتخب کریں۔ تمام ریکارڈز خود بخود آف لائن محفوظ ہوتے ہیں۔",
            icon = Icons.Default.School
        ),
        CoachStep(
            titleEn = "Step 2: 1-Tap 'Mark All Present'",
            titleUr = "مرحلہ ۲: ایک کلک میں تمام طلباء حاضر",
            descriptionEn = "Mark all students present in a single tap! If you make a mistake, tap 'Undo' on the recent action snackbar.",
            descriptionUr = "ایک ہی کلک میں تمام طلباء کی حاضری لگائیں۔ غلطی کی صورت میں 'پچھلا عمل منسوخ کریں' دبائیں۔",
            icon = Icons.Default.TouchApp
        ),
        CoachStep(
            titleEn = "Step 3: Bulk Parent Notification",
            titleUr = "مرحلہ ۳: والدین کو اجتماعی واٹس ایپ پیغام",
            descriptionEn = "Select multiple students to dispatch notices, attendance alerts, or bilingual PDF report cards straight to WhatsApp.",
            descriptionUr = "طلباء کو منتخب کر کے سیدھا والدین کے واٹس ایپ پر حاضری اور کارکردگی رپورٹس روانہ کریں۔",
            icon = Icons.Default.Send
        )
    )

    val currentStep = steps[currentStepIndex]

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume taps outside */ },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .testTag("coach_overlay_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentStep.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (lang.code == "ur") currentStep.titleUr else currentStep.titleEn,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (lang.code == "ur") currentStep.descriptionUr else currentStep.descriptionEn,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Step Dots Indicator
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.forEachIndexed { idx, _ ->
                            Box(
                                modifier = Modifier
                                    .size(if (idx == currentStepIndex) 20.dp else 8.dp, 8.dp)
                                    .background(
                                        if (idx == currentStepIndex) MaterialTheme.colorScheme.primary else Color.LightGray,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("coach_skip_button")
                        ) {
                            Text(if (lang.code == "ur") "چھوڑیں" else "Skip Tour")
                        }

                        Button(
                            onClick = {
                                if (currentStepIndex < steps.size - 1) {
                                    currentStepIndex++
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.testTag("coach_next_button")
                        ) {
                            Text(
                                if (currentStepIndex < steps.size - 1) {
                                    if (lang.code == "ur") "اگلا" else "Next Step"
                                } else {
                                    if (lang.code == "ur") "شروع کریں" else "Got It!"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
