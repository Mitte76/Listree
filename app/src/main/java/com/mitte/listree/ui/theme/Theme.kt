package com.mitte.listree.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.mitte.listree.data.ThemePersistence

private val LocalLisTreeColors = staticCompositionLocalOf { LightLisTreeColors }

@Composable
fun LisTreeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    customColors: LisTreeColors? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themePersistence = ThemePersistence(context)
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    val colorsToProvide = customColors ?: if (darkTheme) {
        themePersistence.loadTheme("dark", DarkLisTreeColors)
    } else {
        themePersistence.loadTheme("light", LightLisTreeColors)
    }

    CompositionLocalProvider(LocalLisTreeColors provides colorsToProvide) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object LisTreeTheme {
    val colors: LisTreeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLisTreeColors.current
}
