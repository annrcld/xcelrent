package com.example.xcelrent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RedBlackColorScheme = darkColorScheme(
    primary = SportRed,
    onPrimary = PureWhite,
    secondary = DarkRed,
    onSecondary = PureWhite,
    background = PureBlack,
    onBackground = PureWhite,
    surface = DeepGrey,
    onSurface = PureWhite,
    error = Color(0xFFCF6679),
    onError = PureBlack
)


@Composable
fun XcelrentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RedBlackColorScheme,
        typography = Typography,
        content = content
    )
}