package com.example.taskmanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Curated macOS Cupertino Color Swatches
val MacAestheticPalette = listOf(
    "#007AFF" to "Classic Blue",
    "#5856D6" to "Deep Indigo",
    "#AF52DE" to "Royal Purple",
    "#FF2D55" to "Vibrant Pink",
    "#FF3B30" to "System Red",
    "#FF9500" to "Sunset Orange",
    "#FFCC00" to "Warm Gold",
    "#34C759" to "Mint Green",
    "#00C7BE" to "Emerald Teal",
    "#30B0C7" to "Sky Cyan",
    "#64D2FF" to "Ice Blue",
    "#BF5AF2" to "Neon Lavender",
    "#FF6482" to "Coral Rose",
    "#A2845E" to "Latte Mocha",
    "#8E8E93" to "Slate Grey"
)

fun hsvToColor(hue: Float, saturation: Float = 0.85f, value: Float = 0.95f): Color {
    val hsv = floatArrayOf(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

@Composable
fun MacColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Color Selection"
) {
    var currentHue by remember { mutableFloatStateOf(210f) }
    var currentBrightness by remember { mutableFloatStateOf(0.95f) }
    var isUsingRainbow by remember { mutableStateOf(false) }

    val rainbowBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFFF3B30),
                Color(0xFFFF9500),
                Color(0xFFFFCC00),
                Color(0xFF34C759),
                Color(0xFF00C7BE),
                Color(0xFF007AFF),
                Color(0xFF5856D6),
                Color(0xFFAF52DE),
                Color(0xFFFF2D55),
                Color(0xFFFF3B30)
            )
        )
    }

    val activeColor = if (isUsingRainbow) {
        hsvToColor(currentHue, value = currentBrightness)
    } else {
        runCatching { Color(android.graphics.Color.parseColor(selectedColorHex)) }.getOrDefault(Color(0xFF007AFF))
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with Live Color Meter Swatch & Hex Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )

                Spacer(Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = colorToHex(activeColor),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 1. macOS Swatches Palette (Horizontal Scrollable Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MacAestheticPalette.forEach { (hex, _) ->
                val swatchColor = Color(android.graphics.Color.parseColor(hex))
                val isSelected = !isUsingRainbow && selectedColorHex.equals(hex, ignoreCase = true)

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .clickable {
                            isUsingRainbow = false
                            onColorSelected(hex)
                        }
                        .then(
                            if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            else Modifier.border(0.5.dp, Color.Black.copy(alpha = 0.1f), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 2. Rainbow Hue Spectrum Bar (Continuous Color Wheel in 1D)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🌈 Rainbow Spectrum Hue",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${currentHue.toInt()}°",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(rainbowBrush)
            ) {
                Slider(
                    value = currentHue,
                    onValueChange = {
                        currentHue = it
                        isUsingRainbow = true
                        val newColor = hsvToColor(it, value = currentBrightness)
                        onColorSelected(colorToHex(newColor))
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // 3. Brightness / Shade Slider
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "💡 Shade & Tone",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(currentBrightness * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Slider(
                value = currentBrightness,
                onValueChange = {
                    currentBrightness = it
                    if (isUsingRainbow) {
                        val newColor = hsvToColor(currentHue, value = it)
                        onColorSelected(colorToHex(newColor))
                    }
                },
                valueRange = 0.4f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = activeColor,
                    activeTrackColor = activeColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
