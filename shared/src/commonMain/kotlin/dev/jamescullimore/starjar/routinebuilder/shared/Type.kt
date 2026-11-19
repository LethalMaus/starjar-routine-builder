package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import dev.jamescullimore.starjar.routinebuilder.shared.resources.Res
import dev.jamescullimore.starjar.routinebuilder.shared.resources.nunito
import org.jetbrains.compose.resources.Font

@Composable
fun appTypography(): Typography {
    val family =
        FontFamily(Font(Res.font.nunito, FontWeight.Normal), Font(Res.font.nunito, FontWeight.Bold))
    val t = Typography()
    return t.copy(
        headlineLarge =
            t.headlineLarge.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        headlineSmall =
            t.headlineSmall.copy(fontFamily = family, fontWeight = FontWeight.ExtraBold),
        titleLarge = t.titleLarge.copy(fontFamily = family),
        bodyLarge = t.bodyLarge.copy(fontFamily = family),
        bodyMedium = t.bodyMedium.copy(fontFamily = family),
        labelLarge = t.labelLarge.copy(fontFamily = family, fontWeight = FontWeight.Bold),
    )
}
