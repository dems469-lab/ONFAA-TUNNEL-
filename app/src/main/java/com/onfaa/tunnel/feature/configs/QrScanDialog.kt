package com.onfaa.tunnel.feature.configs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.onfaa.tunnel.ui.components.GradientButton
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real camera QR scanner. The decoded payload is handed back as raw text and
 * goes through the exact same import path as a pasted link: share-link
 * conversion, legacy VMess base64 decoding or Xray JSON, then structure
 * checks and real engine validation. Nothing is accepted without validation,
 * and no image ever leaves the device.
 */
@Composable
fun QrScanDialog(
    onResult: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = OnfaaTheme.colors
    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A12).copy(alpha = 0.96f))
        ) {
            if (hasPermission) {
                CameraScanView(onResult = onResult)
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        tint = palette.textMuted,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (permissionDenied) {
                            "La caméra a été refusée. Le QR code d'un serveur contient " +
                                "les mêmes informations que son lien de partage ; sans " +
                                "caméra, collez le lien à la place."
                        } else {
                            "La caméra est nécessaire pour scanner le QR code " +
                                "d'un serveur. Aucune image ne quitte l'appareil."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    if (!permissionDenied) {
                        GradientButton(
                            text = "Autoriser la caméra",
                            icon = Icons.Filled.PhotoCamera,
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Scanner un QR code",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Fermer le scanner",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * CameraX preview plus a QR-only analyzer. The camera is unbound when the
 * view leaves composition, and the first successful decode is delivered once.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraScanView(onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val delivered = remember { AtomicBoolean(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            boundProvider?.unbindAll()
            executor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).also { previewView ->
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        try {
                            val provider = providerFuture.get()
                            boundProvider = provider
                            provider.unbindAll()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(
                                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .build()
                            analysis.setAnalyzer(executor) { image ->
                                analyzeFrame(image, delivered, onResult)
                            }
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (failure: Exception) {
                            // The preview stays black; the close button remains
                            // usable and the link can always be pasted instead.
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            }
        )

        Box(
            modifier = Modifier
                .size(248.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
        )
        Text(
            text = "Pointez la caméra vers le QR code du serveur",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(top = 160.dp)
        )
    }
}

/** QR-only decode of each frame, uprighted for the current sensor rotation. */
@OptIn(ExperimentalGetImage::class)
private fun analyzeFrame(
    image: ImageProxy,
    delivered: AtomicBoolean,
    onResult: (String) -> Unit
) {
    try {
        val mediaImage = image.image ?: return
        if (delivered.get()) return
        val outW = uprightWidth(image)
        val outH = uprightHeight(image)
        val luminance = uprightLuminance(image) ?: return
        val source = UprightLuminanceSource(luminance, outW, outH)
        val result = try {
            QRCodeReader().decode(
                BinaryBitmap(HybridBinarizer(source)),
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER to true
                )
            )
        } catch (absent: NotFoundException) {
            null
        }
        val text = result?.text?.trim().orEmpty()
        if (text.isNotEmpty() && delivered.compareAndSet(false, true)) {
            onResult(text)
        }
    } catch (failure: Exception) {
        // A single unreadable frame must never kill the analyzer thread.
    } finally {
        image.close()
    }
}

private fun normalizedRotation(image: ImageProxy): Int =
    ((image.imageInfo.rotationDegrees % 360) + 360) % 360

/** Read-only luminance source over the already uprighted Y buffer. */
private class UprightLuminanceSource(
    private val luma: ByteArray,
    width: Int,
    height: Int
) : LuminanceSource(width, height) {

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val target = row ?: ByteArray(width)
        System.arraycopy(luma, y * width, target, 0, width)
        return target
    }

    override fun getMatrix(): ByteArray = luma
}

private fun uprightWidth(image: ImageProxy): Int {
    val rotation = normalizedRotation(image)
    return if (rotation == 90 || rotation == 270) image.height else image.width
}

private fun uprightHeight(image: ImageProxy): Int {
    val rotation = normalizedRotation(image)
    return if (rotation == 90 || rotation == 270) image.width else image.height
}

/**
 * Copies the Y plane into an upright row-major luminance buffer so zxing sees
 * a correctly oriented image whatever the sensor rotation is.
 */
private fun uprightLuminance(image: ImageProxy): ByteArray? {
    val mediaImage = image.image ?: return null
    val plane = mediaImage.planes[0]
    val buffer = plane.buffer.duplicate()
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val srcW = mediaImage.width
    val srcH = mediaImage.height
    val rotation = normalizedRotation(image)
    val outW = uprightWidth(image)
    val outH = uprightHeight(image)
    val out = ByteArray(outW * outH)

    fun sample(ix: Int, iy: Int): Byte {
        val clampedX = ix.coerceIn(0, srcW - 1)
        val clampedY = iy.coerceIn(0, srcH - 1)
        return buffer.get(clampedY * rowStride + clampedX * pixelStride)
    }

    for (oy in 0 until outH) {
        val rowOffset = oy * outW
        when (rotation) {
            0 -> for (ox in 0 until outW) out[rowOffset + ox] = sample(ox, oy)
            90 -> for (ox in 0 until outW) out[rowOffset + ox] = sample(oy, srcH - 1 - ox)
            180 -> for (ox in 0 until outW) out[rowOffset + ox] =
                sample(srcW - 1 - ox, srcH - 1 - oy)
            270 -> for (ox in 0 until outW) out[rowOffset + ox] = sample(srcW - 1 - oy, ox)
        }
    }
    return out
}
