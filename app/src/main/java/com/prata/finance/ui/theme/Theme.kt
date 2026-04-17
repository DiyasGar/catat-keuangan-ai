package com.prata.finance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = VibrantPurple,
    secondary = MetallicGold,
    tertiary = VibrantPurple,
    background = NavyDark,
    surface = SlateGrey,
    onPrimary = OffWhite,
    onSecondary = NavyDark,
    onBackground = OffWhite,
    onSurface = OffWhite
)

@Composable
fun CatatKeuanganMobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // We default dynamicColor to false to explicitly show off our custom Dark Theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}