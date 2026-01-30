package com.example.xcelrent.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight // This was merged into "Fontimport"
import androidx.compose.ui.unit.sp
import com.example.xcelrent.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// Default text style to avoid repetition
val defaultInterTextStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Normal
)

val Typography = Typography(
    // Display - Large headers
    displayLarge = defaultInterTextStyle.copy(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = defaultInterTextStyle.copy(fontSize = 45.sp),
    displaySmall = defaultInterTextStyle.copy(fontSize = 36.sp),

    // Headline - Screen titles
    headlineLarge = defaultInterTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = defaultInterTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineSmall = defaultInterTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp),

    // Title - Smaller headings (App bars, etc.)
    titleLarge = defaultInterTextStyle.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = defaultInterTextStyle.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = defaultInterTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),

    // Body - Standard text (Inter shines here)
    bodyLarge = defaultInterTextStyle.copy(
        fontSize = 18.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = defaultInterTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = defaultInterTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp),

    // Label - Buttons, captions, and small hints
    labelLarge = defaultInterTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = defaultInterTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = defaultInterTextStyle.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)