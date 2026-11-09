package com.example.comfyui.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.comfyui.data.model.ConnectionLink
import com.example.comfyui.data.model.Node
import com.example.comfyui.data.model.SocketType
import com.example.comfyui.ui.theme.BorderDark

@Composable
fun CanvasWireRenderer(
    nodes: List<Node>,
    links: List<ConnectionLink>,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    isExecuting: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wire_glow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val nodeMap = nodes.associateBy { it.id }

        // Standard node visual dimensions for link attachment points
        val nodeWidth = 260f

        for (link in links) {
            val fromNode = nodeMap[link.fromNodeId] ?: continue
            val toNode = nodeMap[link.toNodeId] ?: continue

            // Compute socket positions in canvas coordinate space
            val startX = (fromNode.positionX + nodeWidth) * scale + offsetX
            val startY = (fromNode.positionY + 48f + (link.fromSocketIndex * 26f)) * scale + offsetY

            val endX = toNode.positionX * scale + offsetX
            val endY = (toNode.positionY + 48f + (link.toSocketIndex * 26f)) * scale + offsetY

            val wireColor = link.type.color

            // Draw cubic Bezier spline
            val dx = (endX - startX).coerceAtLeast(60f) * 0.5f
            val path = Path().apply {
                moveTo(startX, startY)
                cubicTo(
                    startX + dx, startY,
                    endX - dx, endY,
                    endX, endY
                )
            }

            // Glow line if executing
            if (isExecuting) {
                drawPath(
                    path = path,
                    color = wireColor.copy(alpha = 0.35f * pulseAlpha),
                    style = Stroke(
                        width = 7f * scale,
                        cap = StrokeCap.Round
                    )
                )
            }

            // Core connection wire
            drawPath(
                path = path,
                color = wireColor.copy(alpha = if (fromNode.isBypassed || toNode.isBypassed) 0.3f else 0.85f),
                style = Stroke(
                    width = 2.8f * scale,
                    cap = StrokeCap.Round
                )
            )

            // Start & End connection port dots
            drawCircle(
                color = wireColor,
                radius = 4.5f * scale,
                center = Offset(startX, startY)
            )
            drawCircle(
                color = wireColor,
                radius = 4.5f * scale,
                center = Offset(endX, endY)
            )
        }
    }
}
