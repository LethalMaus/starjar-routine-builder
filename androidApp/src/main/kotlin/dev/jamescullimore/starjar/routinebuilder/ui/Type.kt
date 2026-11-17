package dev.jamescullimore.starjar.routinebuilder.ui

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.font.Font
import dev.jamescullimore.starjar.routinebuilder.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
@Composable
fun nunitoFontFamily(): FontFamily {
    if (Locale.current.language in setOf("ar", "he", "iw", "hi", "th")) return FontFamily.Default
    return FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).flatMap { weight ->
        listOf(
            Font(R.font.nunito_variablefont_wght, weight = weight, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(weight.weight))),
            Font(R.font.nunito_italic_variablefont_wght, weight = weight, style = FontStyle.Italic, variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(weight.weight)))
        )
    })
}

private val BaseTypography = Typography()

@Composable
fun appTypography() = Typography(
    displayLarge = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.015).sp
    ),
    displayMedium = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.015).sp
    ),
    displaySmall = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.015).sp
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = nunitoFontFamily()),
    headlineMedium = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = nunitoFontFamily()),
    titleLarge = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = nunitoFontFamily()),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = nunitoFontFamily()),
    bodyLarge = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = nunitoFontFamily()),
    labelLarge = TextStyle(
        fontFamily = nunitoFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = nunitoFontFamily()),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = nunitoFontFamily())
)
