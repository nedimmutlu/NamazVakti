package com.example.namazvakti

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

class CompassActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var rotationVectorSensor: Sensor? = null

    // Canlı Pusula Açısı (0 - 360)
    private var azimuthDegree = mutableFloatStateOf(0f)

    // İstanbul koordinatları (Kıble açısı: ~152°)
    private val userLat = 41.0082
    private val userLng = 28.9784
    private val kaabaLat = 21.4225
    private val kaabaLng = 39.8262

    private val qiblaAngle: Float by lazy {
        calculateQiblaBearing(userLat, userLng, kaabaLat, kaabaLng)
    }

    private val distanceToKaabaKm: Int by lazy {
        calculateDistanceKm(userLat, userLng, kaabaLat, kaabaLng)
    }

    private var hasVibratedForAlignment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        setContent {
            QiblaScreen(
                currentHeading = azimuthDegree.floatValue,
                qiblaAngle = qiblaAngle,
                distanceKm = distanceToKaabaKm,
                onBack = { finish() },
                onQiblaAligned = { triggerAlignmentVibration() }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

            val orientationAngles = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuth = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
            azimuthDegree.floatValue = azimuth
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun triggerAlignmentVibration() {
        if (!hasVibratedForAlignment) {
            hasVibratedForAlignment = true
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(80)
                }
            } catch (_: Exception) {}
        }
    }

    private fun calculateQiblaBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda)
        val x = cos(phi1) * tan(phi2) - sin(phi1) * cos(deltaLambda)
        var qibla = Math.toDegrees(atan2(y, x))
        qibla = (qibla + 360) % 360
        return qibla.toFloat()
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    currentHeading: Float,
    qiblaAngle: Float,
    distanceKm: Int,
    onBack: () -> Unit,
    onQiblaAligned: () -> Unit
) {
    val angleDiff = ((currentHeading - qiblaAngle + 540) % 360) - 180
    val isAligned = abs(angleDiff) <= 4.0f

    LaunchedEffect(isAligned) {
        if (isAligned) {
            onQiblaAligned()
        }
    }

    val animatedCompassRotation by animateFloatAsState(
        targetValue = -currentHeading,
        animationSpec = spring(stiffness = 300f),
        label = "compassAnim"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isAligned) Color(0xFF00E676) else Color(0xFFFFD54F),
        label = "ringColor"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Kıble Pusulası",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Kâbe Açısı: ${qiblaAngle.toInt()}° • Mesafe: $distanceKm km",
                            fontSize = 11.sp,
                            color = Color(0xFFC8E6C9)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F3E2C)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A2216),
                            Color(0xFF06140D)
                        )
                    )
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. DURUM ROZETİ
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isAligned) Color(0xFF00E676).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAligned) Color(0xFF00E676) else Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAligned) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Kıbleye Yöneldiniz ✓",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            fontSize = 13.sp
                        )
                    } else {
                        val turnDirection = if (angleDiff > 0) "Sola dönün" else "Sağa dönün"
                        Text(
                            text = "Kıbleye ${abs(angleDiff).toInt()}° kaldı ($turnDirection)",
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFD54F),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Pusulanın tam doğru göstermesi için telefonu düz tutup havada 8 çizin.",
                fontSize = 11.sp,
                color = Color(0xFF81C784).copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            // 2. GELİŞMİŞ PUSULA KADRANI
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(290.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF133B27),
                                    Color(0xFF0A1F15)
                                )
                            )
                        )
                        .border(
                            width = if (isAligned) 4.dp else 2.dp,
                            color = ringColor,
                            shape = CircleShape
                        )
                )

                // Dönen Disk
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedCompassRotation)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 24.dp.toPx()

                        for (degree in 0 until 360 step 15) {
                            val isMain = (degree % 90 == 0)
                            val tickLength = if (isMain) 14.dp.toPx() else 6.dp.toPx()
                            val strokeWidth = if (isMain) 2.5.dp.toPx() else 1.dp.toPx()
                            val color = if (degree == 0) Color(0xFFFF5252) else Color.White.copy(alpha = 0.4f)

                            val rad = Math.toRadians(degree.toDouble())
                            val startX = (center.x + (radius - tickLength) * sin(rad)).toFloat()
                            val startY = (center.y - (radius - tickLength) * cos(rad)).toFloat()
                            val endX = (center.x + radius * sin(rad)).toFloat()
                            val endY = (center.y - radius * cos(rad)).toFloat()

                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = strokeWidth
                            )
                        }
                    }

                    Text(
                        text = "K",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    )
                    Text(
                        text = "G",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                    )
                    Text(
                        text = "D",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = "B",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 8.dp)
                    )

                    // Kâbe Simgesi (Altın Rozet)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(qiblaAngle),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 34.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFD54F),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0F3E2C)),
                                shadowElevation = 4.dp
                            ) {
                                Text(
                                    text = "🕋",
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }

                // Sabit İbre
                Canvas(modifier = Modifier.size(100.dp)) {
                    val center = Offset(size.width / 2, size.height / 2)

                    val northPath = Path().apply {
                        moveTo(center.x, center.y - 44.dp.toPx())
                        lineTo(center.x + 8.dp.toPx(), center.y)
                        lineTo(center.x - 8.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(northPath, Color(0xFFFF5252))

                    val southPath = Path().apply {
                        moveTo(center.x, center.y + 44.dp.toPx())
                        lineTo(center.x + 8.dp.toPx(), center.y)
                        lineTo(center.x - 8.dp.toPx(), center.y)
                        close()
                    }
                    drawPath(southPath, Color.White.copy(alpha = 0.7f))

                    drawCircle(Color(0xFFFFD54F), radius = 6.dp.toPx(), center = center)
                    drawCircle(Color(0xFF0F3E2C), radius = 3.dp.toPx(), center = center)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. ALT BİLGİ KARTLARI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "KÂBE AÇISI", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${qiblaAngle.toInt()}°", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text(text = "Güneydoğu", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "PUSULA YÖNÜ", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "${currentHeading.toInt()}°", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = ringColor)
                        Text(text = getDirectionText(currentHeading), fontSize = 10.sp, color = Color.Gray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "KÂBE MESAFESİ", fontSize = 10.sp, color = Color(0xFF81C784), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "$distanceKm", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFD54F))
                        Text(text = "Kilometre", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

fun getDirectionText(degree: Float): String {
    val d = (degree + 360) % 360
    return when {
        d >= 337.5 || d < 22.5 -> "Kuzey"
        d < 67.5 -> "Kuzeydoğu"
        d < 112.5 -> "Doğu"
        d < 157.5 -> "Güneydoğu"
        d < 202.5 -> "Güney"
        d < 247.5 -> "Güneybatı"
        d < 292.5 -> "Batı"
        else -> "Kuzeybatı"
    }
}