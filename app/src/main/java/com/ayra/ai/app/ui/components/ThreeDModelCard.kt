package com.ayra.ai.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayra.ai.app.ui.theme.GlassSurfaceDark
import com.ayra.ai.app.ui.theme.NeonCyan
import com.ayra.ai.app.ui.theme.NeonGreen
import com.ayra.ai.app.ui.theme.NeonPurple
import com.ayra.ai.app.ui.theme.NeonRose
import com.ayra.ai.app.ui.theme.TextPrimary
import com.ayra.ai.app.ui.theme.TextSecondary
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

enum class Model3dKind(val label: String, val icon: ImageVector) {
    CYBER_CORE("Core", Icons.Default.ViewInAr),
    CAR("Car", Icons.Default.DirectionsCar),
    SPACESHIP("Ship", Icons.Default.RocketLaunch),
    ROBOT("Robot", Icons.Default.SmartToy),
    DIAMOND("Diamond", Icons.Default.Diamond),
    SPHERE("Sphere", Icons.Default.Public)
}

@Composable
fun ThreeDModelCard(
    craftPayload: String? = null,
    title: String = "3D Holographic Geometry",
    modifier: Modifier = Modifier
) {
    // Parse title & kind from payload if provided
    var parsedTitle by remember(craftPayload) {
        val t = try {
            if (!craftPayload.isNullOrBlank()) {
                val json = JSONObject(craftPayload)
                json.optString("title", title)
            } else title
        } catch (_: Exception) {
            title
        }
        mutableStateOf(t)
    }

    var selectedKind by remember(craftPayload) {
        val lower = (parsedTitle + " " + (craftPayload ?: "")).lowercase()
        val kind = when {
            lower.contains("car") || lower.contains("গাড়ি") || lower.contains("sports") -> Model3dKind.CAR
            lower.contains("ship") || lower.contains("space") || lower.contains("rocket") || lower.contains("মহাকাশ") -> Model3dKind.SPACESHIP
            lower.contains("robot") || lower.contains("রোবট") || lower.contains("droid") -> Model3dKind.ROBOT
            lower.contains("diamond") || lower.contains("crystal") || lower.contains("হীরা") -> Model3dKind.DIAMOND
            lower.contains("sphere") || lower.contains("planet") || lower.contains("ball") || lower.contains("গোলক") -> Model3dKind.SPHERE
            else -> Model3dKind.CYBER_CORE
        }
        mutableStateOf(kind)
    }

    var rotX by remember { mutableFloatStateOf(20f) }
    var rotY by remember { mutableFloatStateOf(35f) }
    var userScale by remember { mutableFloatStateOf(1.0f) }
    var isAutoSpin by remember { mutableStateOf(true) }

    // Auto-spin transition
    val infiniteTransition = rememberInfiniteTransition(label = "3d_spin")
    val autoSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val currentYaw = if (isAutoSpin) rotY + autoSpinAngle else rotY

data class ModelFace(val indices: List<Int>, val tint: Color)

    // 3D Geometry definitions for each kind
    val (vertices, edges, faces) = remember(selectedKind) {
        when (selectedKind) {
            Model3dKind.CAR -> {
                // Cyber Sports Car wireframe
                val v = listOf(
                    // Base chassis
                    Triple(-1.4f, 0.4f, -0.7f), Triple(1.4f, 0.4f, -0.7f),
                    Triple(1.4f, 0.4f, 0.7f), Triple(-1.4f, 0.4f, 0.7f),
                    // Cabin bottom
                    Triple(-1.2f, -0.1f, -0.65f), Triple(1.2f, -0.1f, -0.65f),
                    Triple(1.2f, -0.1f, 0.65f), Triple(-1.2f, -0.1f, 0.65f),
                    // Roof
                    Triple(-0.4f, -0.7f, -0.5f), Triple(0.5f, -0.7f, -0.5f),
                    Triple(0.5f, -0.7f, 0.5f), Triple(-0.4f, -0.7f, 0.5f),
                    // Rear spoiler
                    Triple(-1.3f, -0.4f, -0.6f), Triple(-1.3f, -0.4f, 0.6f)
                )
                val e = listOf(
                    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
                    Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4),
                    Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7),
                    Pair(8, 9), Pair(9, 10), Pair(10, 11), Pair(11, 8),
                    Pair(4, 8), Pair(5, 9), Pair(6, 10), Pair(7, 11),
                    Pair(12, 13), Pair(4, 12), Pair(7, 13)
                )
                val f = listOf(
                    ModelFace(listOf(8, 9, 10, 11), NeonCyan),
                    ModelFace(listOf(4, 5, 9, 8), NeonPurple),
                    ModelFace(listOf(11, 10, 6, 7), NeonCyan),
                    ModelFace(listOf(0, 1, 5, 4), NeonPurple),
                    ModelFace(listOf(3, 2, 6, 7), NeonCyan),
                    ModelFace(listOf(0, 4, 8, 11, 7, 3), NeonRose),
                    ModelFace(listOf(1, 5, 9, 10, 6, 2), NeonRose)
                )
                Triple(v, e, f)
            }

            Model3dKind.SPACESHIP -> {
                // Delta-wing Space Fighter
                val v = listOf(
                    Triple(0f, 0f, 1.8f),      // Nose cone tip
                    Triple(-1.4f, 0.2f, -1.2f), // Left wingtip
                    Triple(1.4f, 0.2f, -1.2f),  // Right wingtip
                    Triple(0f, -0.6f, -1.0f),   // Dorsal fin top
                    Triple(0f, 0.3f, -1.1f),    // Cockpit base
                    Triple(-0.5f, -0.2f, 0.3f), // Cockpit left
                    Triple(0.5f, -0.2f, 0.3f),  // Cockpit right
                    Triple(0f, -0.4f, 0.8f)     // Cockpit canopy peak
                )
                val e = listOf(
                    Pair(0, 1), Pair(0, 2), Pair(1, 2),
                    Pair(0, 3), Pair(1, 3), Pair(2, 3),
                    Pair(0, 4), Pair(1, 4), Pair(2, 4), Pair(3, 4),
                    Pair(5, 6), Pair(6, 7), Pair(7, 5),
                    Pair(0, 7), Pair(1, 5), Pair(2, 6)
                )
                val f = listOf(
                    ModelFace(listOf(0, 1, 3), NeonPurple),
                    ModelFace(listOf(0, 2, 3), NeonPurple),
                    ModelFace(listOf(0, 1, 2), NeonCyan),
                    ModelFace(listOf(0, 5, 7), NeonCyan),
                    ModelFace(listOf(0, 6, 7), NeonCyan),
                    ModelFace(listOf(1, 4, 2), NeonRose)
                )
                Triple(v, e, f)
            }

            Model3dKind.ROBOT -> {
                // Android Mecha
                val v = listOf(
                    // Head
                    Triple(-0.35f, -1.2f, -0.35f), Triple(0.35f, -1.2f, -0.35f),
                    Triple(0.35f, -0.7f, -0.35f), Triple(-0.35f, -0.7f, -0.35f),
                    Triple(-0.35f, -1.2f, 0.35f), Triple(0.35f, -1.2f, 0.35f),
                    Triple(0.35f, -0.7f, 0.35f), Triple(-0.35f, -0.7f, 0.35f),
                    // Torso
                    Triple(-0.7f, -0.6f, -0.4f), Triple(0.7f, -0.6f, -0.4f),
                    Triple(0.6f, 0.4f, -0.35f), Triple(-0.6f, 0.4f, -0.35f),
                    Triple(-0.7f, -0.6f, 0.4f), Triple(0.7f, -0.6f, 0.4f),
                    Triple(0.6f, 0.4f, 0.35f), Triple(-0.6f, 0.4f, 0.35f),
                    // Arms
                    Triple(-1.1f, 0.2f, 0f), Triple(1.1f, 0.2f, 0f)
                )
                val e = listOf(
                    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
                    Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4),
                    Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7),
                    Pair(8, 9), Pair(9, 10), Pair(10, 11), Pair(11, 8),
                    Pair(12, 13), Pair(13, 14), Pair(14, 15), Pair(15, 12),
                    Pair(8, 12), Pair(9, 13), Pair(10, 14), Pair(11, 15),
                    Pair(8, 16), Pair(9, 17)
                )
                val f = listOf(
                    ModelFace(listOf(4, 5, 6, 7), NeonCyan),
                    ModelFace(listOf(0, 1, 2, 3), NeonPurple),
                    ModelFace(listOf(12, 13, 14, 15), NeonCyan),
                    ModelFace(listOf(8, 9, 10, 11), NeonPurple),
                    ModelFace(listOf(8, 9, 13, 12), NeonRose),
                    ModelFace(listOf(11, 10, 14, 15), NeonRose)
                )
                Triple(v, e, f)
            }

            Model3dKind.DIAMOND -> {
                // Multifaceted Gemstone
                val v = listOf(
                    Triple(0f, -1.3f, 0f),     // Crown peak
                    Triple(-0.7f, -0.5f, -0.7f), Triple(0.7f, -0.5f, -0.7f),
                    Triple(0.7f, -0.5f, 0.7f), Triple(-0.7f, -0.5f, 0.7f),
                    Triple(-1.1f, -0.2f, 0f), Triple(1.1f, -0.2f, 0f),
                    Triple(0f, -0.2f, -1.1f), Triple(0f, -0.2f, 1.1f),
                    Triple(0f, 1.4f, 0f)       // Culet bottom
                )
                val e = listOf(
                    Pair(0, 1), Pair(0, 2), Pair(0, 3), Pair(0, 4),
                    Pair(1, 2), Pair(2, 3), Pair(3, 4), Pair(4, 1),
                    Pair(1, 5), Pair(1, 7), Pair(2, 6), Pair(2, 7),
                    Pair(3, 6), Pair(3, 8), Pair(4, 5), Pair(4, 8),
                    Pair(5, 7), Pair(7, 6), Pair(6, 8), Pair(8, 5),
                    Pair(9, 5), Pair(9, 6), Pair(9, 7), Pair(9, 8),
                    Pair(9, 1), Pair(9, 2), Pair(9, 3), Pair(9, 4)
                )
                val f = listOf(
                    ModelFace(listOf(0, 1, 7), NeonCyan),
                    ModelFace(listOf(0, 7, 2), NeonPurple),
                    ModelFace(listOf(0, 2, 6), NeonCyan),
                    ModelFace(listOf(0, 6, 3), NeonPurple),
                    ModelFace(listOf(0, 3, 8), NeonCyan),
                    ModelFace(listOf(0, 8, 4), NeonPurple),
                    ModelFace(listOf(9, 5, 7), NeonCyan),
                    ModelFace(listOf(9, 7, 6), NeonPurple),
                    ModelFace(listOf(9, 6, 8), NeonCyan),
                    ModelFace(listOf(9, 8, 5), NeonPurple)
                )
                Triple(v, e, f)
            }

            Model3dKind.SPHERE -> {
                // Wireframe Geodesic Sphere
                val v = mutableListOf<Triple<Float, Float, Float>>()
                val e = mutableListOf<Pair<Int, Int>>()
                val f = mutableListOf<ModelFace>()
                val rings = 6
                val sectors = 8
                for (r in 0..rings) {
                    val phi = Math.PI * r / rings
                    for (s in 0 until sectors) {
                        val theta = 2 * Math.PI * s / sectors
                        val x = (sin(phi) * cos(theta)).toFloat() * 1.15f
                        val y = (cos(phi)).toFloat() * 1.15f
                        val z = (sin(phi) * sin(theta)).toFloat() * 1.15f
                        v.add(Triple(x, y, z))
                    }
                }
                for (r in 0 until rings) {
                    for (s in 0 until sectors) {
                        val current = r * sectors + s
                        val next = r * sectors + ((s + 1) % sectors)
                        val down = (r + 1) * sectors + s
                        val downNext = (r + 1) * sectors + ((s + 1) % sectors)
                        e.add(Pair(current, next))
                        e.add(Pair(current, down))
                        if (r % 2 == 0) {
                            f.add(ModelFace(listOf(current, next, downNext, down), if (s % 2 == 0) NeonCyan else NeonPurple))
                        }
                    }
                }
                Triple(v, e, f)
            }

            Model3dKind.CYBER_CORE -> {
                // Tesseract Hypercube / Cyber Core
                val v = listOf(
                    // Outer cube
                    Triple(-1f, -1f, -1f), Triple(1f, -1f, -1f),
                    Triple(1f, 1f, -1f), Triple(-1f, 1f, -1f),
                    Triple(-1f, -1f, 1f), Triple(1f, -1f, 1f),
                    Triple(1f, 1f, 1f), Triple(-1f, 1f, 1f),
                    // Inner hypercube
                    Triple(-0.5f, -0.5f, -0.5f), Triple(0.5f, -0.5f, -0.5f),
                    Triple(0.5f, 0.5f, -0.5f), Triple(-0.5f, 0.5f, -0.5f),
                    Triple(-0.5f, -0.5f, 0.5f), Triple(0.5f, -0.5f, 0.5f),
                    Triple(0.5f, 0.5f, 0.5f), Triple(-0.5f, 0.5f, 0.5f),
                    // Core nodes
                    Triple(0f, 0f, 1.4f), Triple(0f, 0f, -1.4f)
                )
                val e = listOf(
                    // Outer edges
                    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 0),
                    Pair(4, 5), Pair(5, 6), Pair(6, 7), Pair(7, 4),
                    Pair(0, 4), Pair(1, 5), Pair(2, 6), Pair(3, 7),
                    // Inner edges
                    Pair(8, 9), Pair(9, 10), Pair(10, 11), Pair(11, 8),
                    Pair(12, 13), Pair(13, 14), Pair(14, 15), Pair(15, 12),
                    Pair(8, 12), Pair(9, 13), Pair(10, 14), Pair(11, 15),
                    // Connectors
                    Pair(0, 8), Pair(1, 9), Pair(2, 10), Pair(3, 11),
                    Pair(4, 12), Pair(5, 13), Pair(6, 14), Pair(7, 15),
                    Pair(16, 12), Pair(16, 13), Pair(16, 14), Pair(16, 15),
                    Pair(17, 8), Pair(17, 9), Pair(17, 10), Pair(17, 11)
                )
                val f = listOf(
                    ModelFace(listOf(0, 1, 2, 3), NeonCyan),
                    ModelFace(listOf(4, 5, 6, 7), NeonPurple),
                    ModelFace(listOf(0, 1, 5, 4), NeonCyan),
                    ModelFace(listOf(2, 3, 7, 6), NeonPurple),
                    ModelFace(listOf(8, 9, 10, 11), NeonRose),
                    ModelFace(listOf(12, 13, 14, 15), NeonRose)
                )
                Triple(v, e, f)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GlassSurfaceDark)
            .border(
                1.3.dp,
                Brush.linearGradient(listOf(NeonCyan, NeonPurple, NeonRose)),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = selectedKind.icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parsedTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 13.5.sp
                )
                Text(
                    text = "Interactive 3D Mesh • ${vertices.size} Vertices • ${edges.size} Edges",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeonCyan,
                    fontSize = 10.5.sp
                )
            }

            // Auto-spin toggle chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isAutoSpin) NeonCyan.copy(alpha = 0.25f) else Color(0xFF131B2E))
                    .border(1.dp, if (isAutoSpin) NeonCyan else Color(0x3300FFFF), RoundedCornerShape(8.dp))
                    .clickable { isAutoSpin = !isAutoSpin }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Spin",
                        tint = if (isAutoSpin) NeonCyan else TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAutoSpin) "Auto Spin" else "Manual",
                        color = if (isAutoSpin) NeonCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Canvas Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF050811))
                .border(1.dp, Color(0x3300FFFF), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        isAutoSpin = false
                        rotY += dragAmount.x * 0.45f
                        rotX = (rotX - dragAmount.y * 0.45f).coerceIn(-85f, 85f)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseScale = size.minDimension * 0.26f * userScale

                // Background holographic spatial grid
                val gridSpacing = 28f
                for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color(0x0C00FFFF),
                        start = Offset(x.toFloat(), 0f),
                        end = Offset(x.toFloat(), size.height),
                        strokeWidth = 0.7f
                    )
                }
                for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
                    drawLine(
                        color = Color(0x0C00FFFF),
                        start = Offset(0f, y.toFloat()),
                        end = Offset(size.width, y.toFloat()),
                        strokeWidth = 0.7f
                    )
                }

                // Central pedestal halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(cx, cy + baseScale * 0.8f),
                        radius = baseScale * 1.4f
                    ),
                    radius = baseScale * 1.4f,
                    center = Offset(cx, cy + baseScale * 0.8f)
                )

                val radX = Math.toRadians(rotX.toDouble())
                val radY = Math.toRadians(currentYaw.toDouble())

                // 3D Matrix Rotation & Perspective Projection
                val projected = vertices.map { (vx, vy, vz) ->
                    // Rotate around Y
                    val x1 = vx * cos(radY) + vz * sin(radY)
                    val z1 = -vx * sin(radY) + vz * cos(radY)
                    // Rotate around X
                    val y2 = vy * cos(radX) - z1 * sin(radX)
                    val z2 = vy * sin(radX) + z1 * cos(radX)

                    val perspectiveFactor = 3.2f / (3.2f + z2.toFloat())
                    val sx = cx + (x1 * baseScale * perspectiveFactor).toFloat()
                    val sy = cy + (y2 * baseScale * perspectiveFactor).toFloat()
                    Triple(sx, sy, z2.toFloat())
                }

                // 1. Render Solid Shaded 3D Polygonal Faces (Depth-Sorted Painter's Algorithm)
                val sortedFaces = faces.mapNotNull { face ->
                    if (face.indices.all { it < projected.size }) {
                        val avgZ = face.indices.map { projected[it].third }.average().toFloat()
                        Pair(face, avgZ)
                    } else null
                }.sortedBy { it.second }

                sortedFaces.forEach { (face, avgZ) ->
                    val path = Path()
                    var faceCenterX = 0f
                    var faceCenterY = 0f
                    face.indices.forEachIndexed { idx, vIdx ->
                        val pt = projected[vIdx]
                        faceCenterX += pt.first
                        faceCenterY += pt.second
                        if (idx == 0) path.moveTo(pt.first, pt.second) else path.lineTo(pt.first, pt.second)
                    }
                    path.close()
                    faceCenterX /= face.indices.size
                    faceCenterY /= face.indices.size

                    val depthShade = ((avgZ + 2.5f) / 5f).coerceIn(0.25f, 1f)
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                face.tint.copy(alpha = 0.48f * depthShade),
                                Color(0xFF070B19).copy(alpha = 0.70f * depthShade)
                            ),
                            center = Offset(faceCenterX, faceCenterY),
                            radius = (baseScale * 0.95f).coerceAtLeast(10f)
                        )
                    )
                }

                // 2. Render 3D Edges with Depth Glow
                edges.forEach { (i1, i2) ->
                    if (i1 < projected.size && i2 < projected.size) {
                        val p1 = projected[i1]
                        val p2 = projected[i2]
                        val avgZ = (p1.third + p2.third) / 2f
                        val depthAlpha = ((avgZ + 2f) / 4f).coerceIn(0.25f, 1f)

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(NeonCyan.copy(alpha = depthAlpha), NeonPurple.copy(alpha = depthAlpha))
                            ),
                            start = Offset(p1.first, p1.second),
                            end = Offset(p2.first, p2.second),
                            strokeWidth = 1.8f * depthAlpha,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Render 3D Vertices (Glowing Nodes)
                projected.forEach { (sx, sy, z) ->
                    val nodeAlpha = ((z + 2f) / 4f).coerceIn(0.35f, 1f)
                    drawCircle(
                        color = NeonRose.copy(alpha = nodeAlpha),
                        radius = 3.6f * nodeAlpha,
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = nodeAlpha * 0.8f),
                        radius = 1.6f,
                        center = Offset(sx, sy)
                    )
                }
            }

            // HUD Overlays
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Yaw: ${(currentYaw.toInt() % 360)}° • Pitch: ${rotX.toInt()}°",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Zoom in / Zoom out buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { userScale = (userScale - 0.15f).coerceIn(0.6f, 2.0f) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
                IconButton(
                    onClick = { userScale = (userScale + 0.15f).coerceIn(0.6f, 2.0f) },
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.7f))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = NeonCyan, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Model Geometry Switcher Tabs
        Text(
            text = "Switch 3D Geometry Object (মডেল রূপান্তর):",
            color = TextSecondary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Model3dKind.entries.forEach { kind ->
                val isSelected = selectedKind == kind
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color(0xFF131B2E))
                        .border(1.dp, if (isSelected) NeonCyan else Color(0x2200FFFF), RoundedCornerShape(8.dp))
                        .clickable {
                            selectedKind = kind
                            parsedTitle = "3D ${kind.label} Hologram"
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = kind.label,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
