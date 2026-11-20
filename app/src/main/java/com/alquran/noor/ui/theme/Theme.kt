package com.alquran.noor.ui.theme

import android.app.Activity
import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7F1D6),
    onPrimaryContainer = Color(0xFF002115),
    secondary = Color(0xFF7A5C00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE089),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF3F6374),
    tertiaryContainer = Color(0xFFC2E8FC),
    onTertiaryContainer = Color(0xFF001F2A),
    background = Color(0xFFFBFDF9),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFFBFDF9),
    onSurface = Color(0xFF191C1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7ED9B0),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFFB7F1D6),
    secondary = Color(0xFFE9C349),
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF5B4300),
    onSecondaryContainer = Color(0xFFFFE089),
    tertiary = Color(0xFFA7CCE0),
    tertiaryContainer = Color(0xFF254B5C),
    onTertiaryContainer = Color(0xFFC2E8FC),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFE0E3DE),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFE0E3DE),
)

@Composable
fun NoorTheme(
    themeMode: Int,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colors = when {
        dynamicColor && dark -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    // أيقونات شريط الحالة/التنقل تتبع المظهر المختار لا مظهر النظام فقط
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
    }

    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}

/**
 * خط المصحف. ضع ملف خط (مثل Amiri Quran) في app/src/main/assets/fonts/quran.ttf
 * وسيُستخدم تلقائيًا، وإلا يُستعمل خط النظام العربي.
 */
@Composable
fun rememberQuranFont(): FontFamily {
    val context = LocalContext.current
    return remember {
        runCatching { FontFamily(Typeface.createFromAsset(context.assets, "fonts/quran.ttf")) }
            .getOrDefault(FontFamily.Default)
    }
}
