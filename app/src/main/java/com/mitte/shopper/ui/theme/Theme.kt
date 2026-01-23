package com.mitte.shopper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 1. Create a data class for your custom semantic colors
data class ShopperColors(
    val topAppBarContainer: Color,
    val topAppBarTitle: Color,
    val listMetaCount: Color
)

// 2. Define the specific colors for light theme
private val LightCustomColors = ShopperColors(
    topAppBarContainer = TopAppBarContainerLight,
    topAppBarTitle = TopAppBarTitleLight,
    listMetaCount = ListMetaCountLight
)

// 3. Define the specific colors for dark theme
private val DarkCustomColors = ShopperColors(
    topAppBarContainer = TopAppBarContainerDark,
    topAppBarTitle = TopAppBarTitleDark,
    listMetaCount = ListMetaCountDark
)

// 4. Create the CompositionLocal that will hold the custom colors
private val LocalShopperColors = staticCompositionLocalOf {
    LightCustomColors // Default to light
}

@Composable
fun ShopperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    // 5. Select the right set of custom colors
    val customColors = if (darkTheme) DarkCustomColors else LightCustomColors

    // 6. Provide both the standard and custom colors to your app
    CompositionLocalProvider(LocalShopperColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// 7. Create a public object to easily access your custom colors
object ShopperTheme {
    val colors: ShopperColors
        @Composable
        get() = LocalShopperColors.current
}