package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun getFontFamilyByName(name: String): FontFamily {
    return when (name.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "sans-serif", "roboto" -> FontFamily.SansSerif
        "raleway" -> FontFamily.SansSerif // Modern clean sans fallback
        "ruda" -> FontFamily.SansSerif
        "ubuntu" -> FontFamily.SansSerif
        "zilla slab" -> FontFamily.Serif
        else -> FontFamily.Default
    }
}

val AvailableFonts = listOf(
    "Raleway",
    "Roboto",
    "Ruda",
    "Ubuntu",
    "Zilla Slab",
    "Sans-serif",
    "Serif",
    "Monospace"
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

