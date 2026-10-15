package com.mvrk.vrka

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.view.WindowCompat

// Colors from user palette
val COLOR_ONYX = Color(0xFF17181C)
val COLOR_MINT = Color(0xFF97E7BA)
val COLOR_CYBER_LIME = Color(0xFFFBFB78)
val COLOR_WHITE = Color(0xFFFFFFFF)

// Legacy brand aliases mapped to new palette
val VRKA_PURPLE = COLOR_MINT
val VrkaPurple = COLOR_MINT
val VrkaPurpleLight = Color(0xFFB4F2CE)
val VrkaPurpleDark = Color(0xFF6ECFA0)
val VrkaSuccess = COLOR_MINT
val VrkaWarning = COLOR_CYBER_LIME
val VrkaError = Color(0xFFFF6B6B)

data class VrkaColors(
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceInset: Color,
    val surfaceFloatingNav: Color,
    val borderSubtle: Color,
    val borderNav: Color,
    val borderActive: Color,
    val accent: Color,
    val accentLight: Color,
    val accentContainer: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val isLight: Boolean,
)

val AmoledVrkaColors = VrkaColors(
    background = Color(0xFF000000),
    surfaceCard = Color(0xFF141519),
    surfaceElevated = Color(0xFF1C1E24),
    surfaceInset = Color(0xFF0D0E11),
    surfaceFloatingNav = Color(0xF217181C),
    borderSubtle = Color(0x0EFFFFFF),
    borderNav = Color(0x3597E7BA),
    borderActive = COLOR_MINT,
    accent = COLOR_MINT,
    accentLight = COLOR_MINT,
    accentContainer = Color(0xFF162B1E),
    success = COLOR_MINT,
    warning = Color(0xFFE5DE62),
    error = VrkaError,
    textPrimary = COLOR_WHITE,
    textSecondary = Color(0xFF9EA2AE),
    textTertiary = Color(0xFF656976),
    isLight = false,
)

val StandardDarkVrkaColors = VrkaColors(
    background = Color(0xFF0E0F12),
    surfaceCard = Color(0xFF181A20),
    surfaceElevated = Color(0xFF22242C),
    surfaceInset = Color(0xFF121317),
    surfaceFloatingNav = Color(0xEB17181C),
    borderSubtle = Color(0x0CFFFFFF),
    borderNav = Color(0x28FFFFFF),
    borderActive = COLOR_MINT,
    accent = COLOR_MINT,
    accentLight = COLOR_MINT,
    accentContainer = Color(0xFF162B1E),
    success = COLOR_MINT,
    warning = Color(0xFFE5DE62),
    error = VrkaError,
    textPrimary = COLOR_WHITE,
    textSecondary = Color(0xFF9EA2AE),
    textTertiary = Color(0xFF656976),
    isLight = false,
)

val LightVrkaColors = VrkaColors(
    background = Color(0xFFF6F7F9),
    surfaceCard = COLOR_WHITE,
    surfaceElevated = COLOR_WHITE,
    surfaceInset = Color(0xFFEEF0F4),
    surfaceFloatingNav = Color(0xF5FFFFFF),
    borderSubtle = Color(0xFFE5E7EB),
    borderNav = Color(0x15000000),
    borderActive = Color(0xFF16804A),
    accent = Color(0xFF15803D),
    accentLight = Color(0xFF0F5A32),
    accentContainer = Color(0xFFE6F8EF),
    success = Color(0xFF15803D),
    warning = Color(0xFFB45309),
    error = Color(0xFFDC2626),
    textPrimary = Color(0xFF111827),
    textSecondary = Color(0xFF4B5563),
    textTertiary = Color(0xFF9CA3AF),
    isLight = true,
)

val LocalVrkaColors = staticCompositionLocalOf { AmoledVrkaColors }

val VrkaGlassSurface = Color(0xEB17181C)
val VrkaGlassBorder = Color(0x14FFFFFF)
val VrkaSurfaceCard = Color(0xFF141519)
val VrkaCardBorder = Color(0x0EFFFFFF)

private val AmoledColors = darkColorScheme(
    primary = COLOR_MINT,
    onPrimary = COLOR_ONYX,
    primaryContainer = Color(0xFF162B1E),
    onPrimaryContainer = COLOR_MINT,
    background = Color.Black,
    onBackground = COLOR_WHITE,
    surface = Color(0xFF141519),
    onSurface = COLOR_WHITE,
    surfaceVariant = Color(0xFF1C1E24),
    onSurfaceVariant = Color(0xFF9EA2AE),
    outline = Color(0x3097E7BA),
    outlineVariant = Color(0x0EFFFFFF),
    error = VrkaError,
)

private val StandardDarkColors = darkColorScheme(
    primary = COLOR_MINT,
    onPrimary = COLOR_ONYX,
    primaryContainer = Color(0xFF162B1E),
    onPrimaryContainer = COLOR_MINT,
    background = Color(0xFF0E0F12),
    onBackground = COLOR_WHITE,
    surface = Color(0xFF181A20),
    onSurface = COLOR_WHITE,
    surfaceVariant = Color(0xFF22242C),
    onSurfaceVariant = Color(0xFF9EA2AE),
    outline = Color(0x2897E7BA),
    outlineVariant = Color(0x0CFFFFFF),
    error = VrkaError,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = COLOR_WHITE,
    primaryContainer = Color(0xFFE6F8EF),
    onPrimaryContainer = Color(0xFF0F5A32),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF111827),
    surface = COLOR_WHITE,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF0F4),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB),
    error = Color(0xFFDC2626),
)

// PP Mori font family
val PPMori = FontFamily(
    Font(R.font.pp_mori_regular, FontWeight.Normal),
    Font(R.font.pp_mori_regular, FontWeight.Medium),
    Font(R.font.pp_mori_semibold, FontWeight.SemiBold),
    Font(R.font.pp_mori_semibold, FontWeight.Bold),
)

val SpaceMono = FontFamily(
    Font(R.font.space_mono_regular, FontWeight.Normal),
    Font(R.font.space_mono_bold, FontWeight.Bold),
)

val LocalVrkaFontFamily = staticCompositionLocalOf { PPMori }

val VrkaMonoFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = LocalVrkaFontFamily.current

val VrkaSansFamily: FontFamily
    @Composable
    @ReadOnlyComposable
    get() = LocalVrkaFontFamily.current

private val defaultTypography = Typography()

fun vrkaTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily),
)

val VrkaTypography = vrkaTypography(PPMori)

@Composable
fun VrkaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    amoled: Boolean = true,
    fontPreference: FontPreference = FontPreference.VRKA_FONT,
    content: @Composable () -> Unit,
) {
    val isLight = themeMode == ThemeMode.LIGHT
    val amoledEffective = amoled && !isLight
    val vrkaColors = when {
        isLight -> LightVrkaColors
        amoledEffective -> AmoledVrkaColors
        else -> StandardDarkVrkaColors
    }
    val colors = when {
        isLight -> LightColors
        amoledEffective -> AmoledColors
        else -> StandardDarkColors
    }
    val selectedFont = when (fontPreference) {
        FontPreference.VRKA_FONT -> PPMori
        FontPreference.SYSTEM_FONT -> FontFamily.Default
    }
    val typography = vrkaTypography(selectedFont)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    CompositionLocalProvider(
        LocalVrkaColors provides vrkaColors,
        LocalVrkaFontFamily provides selectedFont,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = typography,
            content = content,
        )
    }
}
