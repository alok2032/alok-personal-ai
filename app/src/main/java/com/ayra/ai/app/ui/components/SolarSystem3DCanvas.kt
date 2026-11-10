package com.ayra.ai.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class RealPlanetSpec(
    val name: String,
    val distanceRatio: Float,
    val orbitalSpeed: Float,
    val baseRadius: Float,
    val coreColor: Color,
    val surfaceColor: Color,
    val atmosphereColor: Color = Color.Transparent,
    val hasRings: Boolean = false,
    val ringInnerRatio: Float = 1.35f,
    val ringOuterRatio: Float = 2.85f,
    val ringColor: Color = Color.Transparent,
    val hasMoon: Boolean = false,
    val hasBands: Boolean = false,
    val hasIceCaps: Boolean = false
)

private data class AsteroidParticle(
    val distanceRatio: Float,
    val initialAngle: Float,
    val speed: Float,
    val size: Float,
    val brightness: Float,
    val color: Color
)

@Composable
fun SolarSystem3DCanvas(
    modifier: Modifier = Modifier
) {
    var pitchAngle by remember { mutableFloatStateOf(65f) }
    var yawAngle by remember { mutableFloatStateOf(20f) }

    val infiniteTransition = rememberInfiniteTransition(label = "SolarSystemAnimation")
    val orbitTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbitTime"
    )

    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SunPulse"
    )

    val shootingStarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShootingStarProgress"
    )

    // Accurate Solar System planet specifications
    val planets = remember {
        listOf(
            RealPlanetSpec(
                name = "Mercury",
                distanceRatio = 0.16f,
                orbitalSpeed = 4.15f,
                baseRadius = 4.0f,
                coreColor = Color(0xFFE5E7EB),
                surfaceColor = Color(0xFF6B7280)
            ),
            RealPlanetSpec(
                name = "Venus",
                distanceRatio = 0.25f,
                orbitalSpeed = 1.62f,
                baseRadius = 6.2f,
                coreColor = Color(0xFFFEF3C7),
                surfaceColor = Color(0xFFD97706),
                atmosphereColor = Color(0x66F59E0B)
            ),
            RealPlanetSpec(
                name = "Earth",
                distanceRatio = 0.36f,
                orbitalSpeed = 1.00f,
                baseRadius = 7.0f,
                coreColor = Color(0xFF93C5FD),
                surfaceColor = Color(0xFF1D4ED8),
                atmosphereColor = Color(0x8838BDF8),
                hasMoon = true
            ),
            RealPlanetSpec(
                name = "Mars",
                distanceRatio = 0.47f,
                orbitalSpeed = 0.53f,
                baseRadius = 5.0f,
                coreColor = Color(0xFFFCA5A5),
                surfaceColor = Color(0xFFB91C1C),
                atmosphereColor = Color(0x55EF4444),
                hasIceCaps = true
            ),
            RealPlanetSpec(
                name = "Jupiter",
                distanceRatio = 0.65f,
                orbitalSpeed = 0.24f,
                baseRadius = 14.5f,
                coreColor = Color(0xFFFED7AA),
                surfaceColor = Color(0xFFC2410C),
                atmosphereColor = Color(0x44F97316),
                hasBands = true
            ),
            RealPlanetSpec(
                name = "Saturn",
                distanceRatio = 0.79f,
                orbitalSpeed = 0.12f,
                baseRadius = 12.0f,
                coreColor = Color(0xFFFEF9C3),
                surfaceColor = Color(0xFFB45309),
                hasRings = true,
                ringInnerRatio = 1.35f,
                ringOuterRatio = 2.95f,
                ringColor = Color(0xDDEAB308)
            ),
            RealPlanetSpec(
                name = "Uranus",
                distanceRatio = 0.90f,
                orbitalSpeed = 0.06f,
                baseRadius = 8.2f,
                coreColor = Color(0xFFA7F3D0),
                surfaceColor = Color(0xFF0D9488),
                atmosphereColor = Color(0x662DD4BF),
                hasRings = true,
                ringInnerRatio = 1.25f,
                ringOuterRatio = 1.95f,
                ringColor = Color(0x552DD4BF)
            ),
            RealPlanetSpec(
                name = "Neptune",
                distanceRatio = 0.99f,
                orbitalSpeed = 0.035f,
                baseRadius = 7.8f,
                coreColor = Color(0xFF93C5FD),
                surfaceColor = Color(0xFF1E3A8A),
                atmosphereColor = Color(0x773B82F6)
            )
        )
    }

    // Dense Asteroid Belt between Mars (0.47) and Jupiter (0.65)
    val asteroids = remember {
        List(140) {
            val dist = 0.52f + (Math.random() * 0.09f).toFloat()
            val initAngle = (Math.random() * 360f).toFloat()
            val speed = 0.38f + (Math.random() * 0.12f).toFloat()
            val sz = (0.8f + Math.random() * 1.6f).toFloat()
            val bright = (0.35f + Math.random() * 0.55f).toFloat()
            val col = if (Math.random() > 0.4) Color(0xFFE5E7EB) else Color(0xFFB45309)
            AsteroidParticle(dist, initAngle, speed, sz, bright, col)
        }
    }

    // Distant background starfield
    val stars = remember {
        List(95) {
            val sx = (Math.random() * 1200).toFloat()
            val sy = (Math.random() * 2200).toFloat()
            val sr = (0.6f + Math.random() * 1.8f).toFloat()
            val sa = (0.25f + Math.random() * 0.65f).toFloat()
            Triple(sx, sy, Pair(sr, sa))
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    yawAngle += dragAmount.x * 0.25f
                    pitchAngle = (pitchAngle + dragAmount.y * 0.25f).coerceIn(25f, 85f)
                }
            }
    ) {
        val centerX = size.width / 2f
        val centerY = size.height * 0.44f
        val maxOrbitRadius = size.width * 0.48f

        val pitchRad = Math.toRadians(pitchAngle.toDouble())
        val yawRad = Math.toRadians(yawAngle.toDouble())
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        // 1. Distant Cosmic Nebula Clouds
        drawNebulaGlows(size, orbitTime)

        // 1b. Radiant Shooting Stars across deep space
        drawShootingStars(size, shootingStarProgress)

        // 2. Distant twinkling stars
        stars.forEach { (sx, sy, props) ->
            val actualX = (sx % size.width)
            val actualY = (sy % size.height)
            drawCircle(
                color = Color.White.copy(alpha = props.second),
                radius = props.first,
                center = Offset(actualX, actualY)
            )
        }

        // 3. Render Orbital Ellipses with delicate cosmic glow
        planets.forEach { planet ->
            val orbitRadius = maxOrbitRadius * planet.distanceRatio
            val orbitPath = Path()
            val steps = 72
            for (i in 0..steps) {
                val theta = (i * 2 * PI / steps)
                val ex = centerX + (orbitRadius * cos(theta)).toFloat()
                val ey = centerY + (orbitRadius * sin(theta) * cosPitch).toFloat()
                if (i == 0) orbitPath.moveTo(ex, ey) else orbitPath.lineTo(ex, ey)
            }
            drawPath(
                path = orbitPath,
                color = Color(0x2200FFFF),
                style = Stroke(width = 0.85f)
            )
        }

        // 4. Render Asteroid Belt particles between Mars and Jupiter
        asteroids.forEach { asteroid ->
            val aRadius = maxOrbitRadius * asteroid.distanceRatio
            val aAngleDeg = (asteroid.initialAngle + orbitTime * asteroid.speed * 2.2f) % 360f
            val aAngleRad = Math.toRadians(aAngleDeg.toDouble() + yawRad)
            val ax = (aRadius * cos(aAngleRad)).toFloat()
            val ay = (aRadius * sin(aAngleRad)).toFloat()
            val aScreenX = centerX + ax
            val aScreenY = centerY + (ay * cosPitch)
            val aDepth = ay * sinPitch
            val alphaFactor = (0.2f + 0.6f * ((aDepth + maxOrbitRadius) / (maxOrbitRadius * 2f))).coerceIn(0.15f, 0.9f)

            drawCircle(
                color = asteroid.color.copy(alpha = asteroid.brightness * alphaFactor),
                radius = asteroid.size,
                center = Offset(aScreenX, aScreenY)
            )
        }

        // 5. Calculate 3D Depth Positions for all planets
        data class PlanetDrawItem(
            val spec: RealPlanetSpec,
            val screenPos: Offset,
            val depthZ: Float,
            val scaledRadius: Float,
            val orbitAngleRad: Double
        )

        val planetDrawList = planets.map { planet ->
            val orbitRadius = maxOrbitRadius * planet.distanceRatio
            val angleDeg = (orbitTime * planet.orbitalSpeed * 8.0f) % 360f
            val angleRad = Math.toRadians(angleDeg.toDouble() + yawRad)

            val px = (orbitRadius * cos(angleRad)).toFloat()
            val py = (orbitRadius * sin(angleRad)).toFloat()

            val screenX = centerX + px
            val screenY = centerY + (py * cosPitch)
            val depthZ = py * sinPitch

            val depthScale = (0.75f + 0.35f * ((depthZ + maxOrbitRadius) / (maxOrbitRadius * 2f))).coerceIn(0.65f, 1.25f)
            val scaledR = planet.baseRadius * depthScale

            PlanetDrawItem(
                spec = planet,
                screenPos = Offset(screenX, screenY),
                depthZ = depthZ,
                scaledRadius = scaledR,
                orbitAngleRad = angleRad
            )
        }

        // Sort by depth (behind sun first, then sun, then in front of sun)
        val behindSun = planetDrawList.filter { it.depthZ < 0 }.sortedBy { it.depthZ }
        val inFrontSun = planetDrawList.filter { it.depthZ >= 0 }.sortedBy { it.depthZ }

        // Draw behind-sun planets
        behindSun.forEach {
            drawRealPlanet(it.screenPos, it.spec, it.scaledRadius, orbitTime, cosPitch, centerX, centerY)
        }

        // Draw Sun at Center with realistic solar flares and corona
        drawRealisticSun(centerX, centerY, sunPulse)

        // Draw in-front planets
        inFrontSun.forEach {
            drawRealPlanet(it.screenPos, it.spec, it.scaledRadius, orbitTime, cosPitch, centerX, centerY)
        }
    }
}

private fun DrawScope.drawNebulaGlows(canvasSize: Size, orbitTime: Float) {
    val t1 = Math.toRadians(orbitTime.toDouble() * 0.4)
    val t2 = Math.toRadians(orbitTime.toDouble() * 0.7)
    val driftX1 = (sin(t1) * 35f).toFloat()
    val driftY1 = (cos(t1) * 20f).toFloat()
    val driftX2 = (cos(t2) * 30f).toFloat()
    val driftY2 = (sin(t2) * 25f).toFloat()

    // 1. Celestial Royal Violet & Deep Indigo Nebula (Upper Right)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x388B5CF6),
                Color(0x226366F1),
                Color(0x0C3B82F6),
                Color.Transparent
            ),
            center = Offset(canvasSize.width * 0.82f + driftX1, canvasSize.height * 0.18f + driftY1),
            radius = canvasSize.width * 0.65f
        ),
        radius = canvasSize.width * 0.65f,
        center = Offset(canvasSize.width * 0.82f + driftX1, canvasSize.height * 0.18f + driftY1)
    )

    // 2. Electric Cyan & Aqua Aurora Nebula (Lower Left)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x3506B6D4),
                Color(0x1E0EA5E9),
                Color(0x0A0284C7),
                Color.Transparent
            ),
            center = Offset(canvasSize.width * 0.18f - driftX2, canvasSize.height * 0.68f + driftY2),
            radius = canvasSize.width * 0.60f
        ),
        radius = canvasSize.width * 0.60f,
        center = Offset(canvasSize.width * 0.18f - driftX2, canvasSize.height * 0.68f + driftY2)
    )

    // 3. Cosmic Magenta & Rose Starlight Glow (Upper Left Horizon)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x28EC4899),
                Color(0x18F43F5E),
                Color(0x08881337),
                Color.Transparent
            ),
            center = Offset(canvasSize.width * 0.15f + driftX1 * 0.6f, canvasSize.height * 0.12f - driftY1 * 0.5f),
            radius = canvasSize.width * 0.45f
        ),
        radius = canvasSize.width * 0.45f,
        center = Offset(canvasSize.width * 0.15f + driftX1 * 0.6f, canvasSize.height * 0.12f - driftY1 * 0.5f)
    )

    // 4. Emerald Solar Corona Ambient Sheen (Center-Lower Ambience)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0x2010B981),
                Color(0x0E059669),
                Color.Transparent
            ),
            center = Offset(canvasSize.width * 0.72f - driftX2 * 0.5f, canvasSize.height * 0.82f - driftY2 * 0.5f),
            radius = canvasSize.width * 0.52f
        ),
        radius = canvasSize.width * 0.52f,
        center = Offset(canvasSize.width * 0.72f - driftX2 * 0.5f, canvasSize.height * 0.82f - driftY2 * 0.5f)
    )
}

private fun DrawScope.drawRealisticSun(cx: Float, cy: Float, pulseAngle: Float) {
    val pulse = (1f + 0.05f * sin(Math.toRadians(pulseAngle.toDouble() * 2.0))).toFloat()

    // Solar Prominence Flares / Heat Rays (8 radiant loops)
    for (i in 0 until 8) {
        val angle = Math.toRadians((i * 45.0 + pulseAngle * 0.8) % 360.0)
        val rayLen = (32f + 8f * sin(Math.toRadians((i * 45.0 + pulseAngle * 3.0) % 360.0))).toFloat()
        val rx = cx + (rayLen * cos(angle)).toFloat()
        val ry = cy + (rayLen * sin(angle)).toFloat()

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFB703).copy(alpha = 0.7f), Color(0xFFFF0055).copy(alpha = 0.1f), Color.Transparent),
                start = Offset(cx, cy),
                end = Offset(rx, ry)
            ),
            start = Offset(cx, cy),
            end = Offset(rx, ry),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // Solar Corona outermost flare halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFDD00).copy(alpha = 0.85f),
                Color(0xFFFF6600).copy(alpha = 0.45f),
                Color(0xFFFF0044).copy(alpha = 0.15f),
                Color.Transparent
            ),
            center = Offset(cx, cy),
            radius = 68f * pulse
        ),
        radius = 68f * pulse,
        center = Offset(cx, cy)
    )

    // Sun core sphere with radiant light
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFF9DB),
                Color(0xFFFFB703),
                Color(0xFFFB8500),
                Color(0xFFD00000)
            ),
            center = Offset(cx - 3f, cy - 3f),
            radius = 24f
        ),
        radius = 24f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawRealPlanet(
    screenPos: Offset,
    spec: RealPlanetSpec,
    scaledRadius: Float,
    orbitTime: Float,
    cosPitch: Float,
    sunCenterX: Float,
    sunCenterY: Float
) {
    val cx = screenPos.x
    val cy = screenPos.y

    // Saturn / Uranus Rings (back layer)
    if (spec.hasRings) {
        val ringRx = scaledRadius * spec.ringOuterRatio
        val ringRy = scaledRadius * spec.ringOuterRatio * 0.38f * (cosPitch.coerceAtLeast(0.3f))

        // Outer ring
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    spec.ringColor.copy(alpha = 0.4f),
                    spec.ringColor,
                    Color(0x33000000), // Cassini Division gap
                    spec.ringColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = ringRx
            ),
            topLeft = Offset(cx - ringRx, cy - ringRy),
            size = Size(ringRx * 2, ringRy * 2),
            style = Stroke(width = scaledRadius * 0.55f)
        )
    }

    // Atmospheric Glow (Earth blue halo, Venus amber halo, Mars red halo)
    if (spec.atmosphereColor != Color.Transparent) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(spec.atmosphereColor, Color.Transparent),
                center = Offset(cx, cy),
                radius = scaledRadius * 1.35f
            ),
            radius = scaledRadius * 1.35f,
            center = screenPos
        )
    }

    // Dynamic light direction vector from Sun to Planet
    val sunAngle = atan2((cy - sunCenterY).toDouble(), (cx - sunCenterX).toDouble())
    val lightOffsetDist = scaledRadius * 0.38f
    val lightX = cx - (lightOffsetDist * cos(sunAngle)).toFloat()
    val lightY = cy - (lightOffsetDist * sin(sunAngle)).toFloat()

    // Realistic shaded planet sphere with day/night terminator
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                spec.coreColor,
                spec.surfaceColor,
                Color(0xFF030712)
            ),
            center = Offset(lightX, lightY),
            radius = scaledRadius * 1.15f
        ),
        radius = scaledRadius,
        center = screenPos
    )

    // Jupiter's cloud storm bands
    if (spec.hasBands) {
        val bandCount = 3
        for (b in -bandCount..bandCount) {
            val bandY = cy + (b * scaledRadius * 0.22f)
            val bandWidth = (scaledRadius * cos(Math.asin(((b * 0.22).coerceIn(-0.9, 0.9))))).toFloat() * 1.8f
            if (bandWidth > 0) {
                drawLine(
                    color = if (b % 2 == 0) Color(0x44D97706) else Color(0x33FEF3C7),
                    start = Offset(cx - bandWidth / 2f, bandY),
                    end = Offset(cx + bandWidth / 2f, bandY),
                    strokeWidth = scaledRadius * 0.16f
                )
            }
        }
        // Great Red Spot
        drawCircle(
            color = Color(0xFFDC2626).copy(alpha = 0.85f),
            radius = scaledRadius * 0.18f,
            center = Offset(cx + scaledRadius * 0.3f, cy + scaledRadius * 0.25f)
        )
    }

    // Mars polar ice caps
    if (spec.hasIceCaps) {
        drawCircle(
            color = Color(0xFFF8FAFC).copy(alpha = 0.85f),
            radius = scaledRadius * 0.22f,
            center = Offset(cx, cy - scaledRadius * 0.75f)
        )
    }

    // Earth's orbiting Moon
    if (spec.hasMoon) {
        val moonDist = scaledRadius * 2.3f
        val moonAngle = Math.toRadians((orbitTime * 14f % 360).toDouble())
        val moonX = cx + (moonDist * cos(moonAngle)).toFloat()
        val moonY = cy + (moonDist * sin(moonAngle) * cosPitch).toFloat()

        drawCircle(
            color = Color(0xFFE5E7EB),
            radius = (scaledRadius * 0.26f).coerceIn(1.5f, 3.2f),
            center = Offset(moonX, moonY)
        )
    }
}

private fun DrawScope.drawShootingStars(canvasSize: Size, progress: Float) {
    if (progress < 0.45f) {
        val t = progress / 0.45f
        val startX = canvasSize.width * 0.88f - (canvasSize.width * 0.85f * t)
        val startY = canvasSize.height * 0.08f + (canvasSize.height * 0.32f * t)
        val tailLength = 110f
        val angleRad = Math.toRadians(25.0)
        val endX = startX + (tailLength * cos(angleRad)).toFloat()
        val endY = startY - (tailLength * sin(angleRad)).toFloat()

        val alpha = if (t < 0.15f) t / 0.15f else if (t > 0.82f) (1f - t) / 0.18f else 1.0f

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
                    Color(0xFF00FFFF).copy(alpha = (alpha * 0.8f).coerceIn(0f, 1f)),
                    Color(0xFF8B5CF6).copy(alpha = (alpha * 0.35f).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                start = Offset(startX, startY),
                end = Offset(endX, endY)
            ),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 2.4f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = Color.White.copy(alpha = alpha.coerceIn(0f, 1f)),
            radius = 2.8f,
            center = Offset(startX, startY)
        )
    }

    if (progress in 0.52f..0.94f) {
        val t2 = (progress - 0.52f) / 0.42f
        val startX2 = canvasSize.width * 0.38f - (canvasSize.width * 0.65f * t2)
        val startY2 = canvasSize.height * 0.22f + (canvasSize.height * 0.28f * t2)
        val tailLength2 = 80f
        val angleRad2 = Math.toRadians(30.0)
        val endX2 = startX2 + (tailLength2 * cos(angleRad2)).toFloat()
        val endY2 = startY2 - (tailLength2 * sin(angleRad2)).toFloat()

        val alpha2 = if (t2 < 0.15f) t2 / 0.15f else if (t2 > 0.82f) (1f - t2) / 0.18f else 0.85f

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF80AB).copy(alpha = alpha2.coerceIn(0f, 1f)),
                    Color(0xFFB388FF).copy(alpha = (alpha2 * 0.6f).coerceIn(0f, 1f)),
                    Color.Transparent
                ),
                start = Offset(startX2, startY2),
                end = Offset(endX2, endY2)
            ),
            start = Offset(startX2, startY2),
            end = Offset(endX2, endY2),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
    }
}

