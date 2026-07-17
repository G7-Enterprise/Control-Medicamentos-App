package com.carlos.controlmedicamentos

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MedicamentoFormGradientWrapper(
    modifier: Modifier = Modifier,
    mostrarEscritorio: Boolean,
    panelUsaScrollInterno: Boolean,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val baseGradient = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF030D1F),
                            Color(0xFF0A2A4B),
                            Color(0xFF1768A3),
                            Color(0xFF0C3451),
                            Color(0xFF030D1F)
                        )
                    )
                    val topShade = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.22f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.30f)
                        )
                    )
                    val strongLineColor = Color.White.copy(alpha = 0.11f)
                    val softLineColor = Color(0xFFA7D0FF).copy(alpha = 0.08f)
                    val thinStroke = 1.dp.toPx()
                    val softStroke = 0.6.dp.toPx()
                    val spacing = 6.dp.toPx()
                    val secondarySpacing = 3.dp.toPx()

                    onDrawBehind {
                        drawRect(brush = baseGradient)
                        var y = 0f
                        var index = 0
                        while (y < size.height) {
                            drawLine(
                                color = if (index % 3 == 0) strongLineColor else softLineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = if (index % 3 == 0) thinStroke else softStroke
                            )
                            if (index % 5 == 0) {
                                drawLine(
                                    color = Color.Black.copy(alpha = 0.06f),
                                    start = Offset(size.width * 0.08f, y + secondarySpacing),
                                    end = Offset(size.width * 0.92f, y + secondarySpacing),
                                    strokeWidth = softStroke
                                )
                            }
                            y += spacing
                            index += 1
                        }
                        drawRect(brush = topShade)
                    }
                }
                .let {
                    if (!panelUsaScrollInterno) it.verticalScroll(scrollState)
                    else it
                }
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (mostrarEscritorio) 24.dp else 0.dp,
                    bottom = if (mostrarEscritorio) 24.dp else 0.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
