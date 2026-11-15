package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.local.entities.StudentEntity
import com.example.domain.service.QrCodeService
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    title: String,
    students: List<StudentEntity>,
    onDismiss: () -> Unit,
    onQrScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionRequested by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var manualCode by remember { mutableStateOf("") }
    var lastDetectedCode by remember { mutableStateOf<String?>(null) }
    var activeCamera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("qr_scanner_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = AppStrings.get("qr_scan_subtitle", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (hasCameraPermission && cameraError == null) {
                        IconButton(
                            onClick = {
                                val next = !torchEnabled
                                torchEnabled = next
                                activeCamera?.cameraControl?.enableTorch(next)
                            },
                            modifier = Modifier.testTag("qr_torch_toggle_btn")
                        ) {
                            Icon(
                                imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = AppStrings.get("qr_flash_toggle", lang),
                                tint = if (torchEnabled) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("qr_scanner_close_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close scanner")
                    }
                }

                // Live CameraX Preview + ML Kit Analyzer Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && cameraError == null) {
                        CameraQrPreviewView(
                            onCameraBound = { cam -> activeCamera = cam },
                            onCameraError = { err -> cameraError = err },
                            onBarcodeDetected = { raw ->
                                if (lastDetectedCode != raw) {
                                    lastDetectedCode = raw
                                    onQrScanned(raw)
                                }
                            }
                        )

                        // Scanner Reticle Overlay
                        ScannerReticleOverlay()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Align Student QR ID inside the frame",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        // Camera permission denied or camera unavailable fallback UI
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = cameraError ?: AppStrings.get("qr_permission_required", lang),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!hasCameraPermission) {
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    modifier = Modifier.testTag("qr_request_camera_permission_btn")
                                ) {
                                    Text(AppStrings.get("qr_grant_camera", lang))
                                }
                            }
                        }
                    }
                }

                // Quick Student ID fallback / Manual QR input (for damaged IDs or emulator testing)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = manualCode,
                                onValueChange = { manualCode = it },
                                label = { Text(AppStrings.get("qr_manual_label", lang), fontSize = 12.sp) },
                                placeholder = { Text("ADM-2026-601", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("qr_manual_code_input")
                            )
                            Button(
                                onClick = {
                                    if (manualCode.isNotBlank()) {
                                        onQrScanned(manualCode.trim())
                                    }
                                },
                                enabled = manualCode.isNotBlank(),
                                modifier = Modifier.testTag("qr_manual_submit_btn")
                            ) {
                                Text(AppStrings.get("qr_verify_match", lang), fontSize = 12.sp)
                            }
                        }

                        if (students.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                students.take(6).forEach { stu ->
                                    AssistChip(
                                        onClick = {
                                            onQrScanned(QrCodeService.encodeStudentQrPayload(stu))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.QrCode2,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        label = { Text("${stu.admissionNumber} (${stu.nameEn.substringBefore(" ")})", fontSize = 11.sp) },
                                        modifier = Modifier.testTag("qr_quick_student_${stu.admissionNumber}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraQrPreviewView(
    onCameraBound: (Camera) -> Unit,
    onCameraError: (String) -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                barcodeScanner.close()
            } catch (_: Exception) {
            }
            analysisExecutor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    @Suppress("DEPRECATION")
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val inputImage = InputImage.fromMediaImage(
                                mediaImage,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            barcodeScanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val value = barcodes.firstNotNullOfOrNull { it.rawValue }
                                    if (!value.isNullOrBlank()) {
                                        onBarcodeDetected(value)
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                    onCameraBound(camera)
                } catch (e: Exception) {
                    onCameraError(
                        "Camera preview unavailable (${e.localizedMessage ?: "no back camera"}). Use manual Student ID below."
                    )
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun ScannerReticleOverlay() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        val boxSize = size.minDimension * 0.62f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val rect = Rect(left, top, left + boxSize, top + boxSize)

        drawRect(color = Color.Black.copy(alpha = 0.45f))
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(rect.left, rect.top),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24f, 24f),
            blendMode = BlendMode.Clear
        )
        drawRoundRect(
            color = Color(0xFF34D399),
            topLeft = Offset(rect.left, rect.top),
            size = androidx.compose.ui.geometry.Size(boxSize, boxSize),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 6f)
        )
    }
}

@Composable
fun StudentQrIdCardDialog(
    student: StudentEntity,
    onDismiss: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val payload = remember(student.uuid, student.admissionNumber) {
        QrCodeService.encodeStudentQrPayload(student)
    }
    val qrBitmap = remember(payload) {
        QrCodeService.generateQrBitmap(payload, 480)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("student_qr_id_dialog"),
        title = {
            Column {
                Text(
                    text = AppStrings.get("qr_student_card_title", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "${student.nameEn} (${student.nameUr})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        tonalElevation = 2.dp
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code for ${student.nameEn}",
                            modifier = Modifier
                                .size(210.dp)
                                .padding(12.dp)
                                .testTag("student_qr_bitmap_image")
                        )
                    }
                }
                Text(
                    text = "Class ${student.className}-${student.section} • Roll ${student.rollNumber}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = payload,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_student_qr_card_btn")
            ) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}
