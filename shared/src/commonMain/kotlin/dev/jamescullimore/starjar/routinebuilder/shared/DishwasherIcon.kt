package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

// Original vector: appliance controls above a rack of plates. Stable ID: Dishwasher.
internal val DishwasherIcon: ImageVector by lazy {
    ImageVector.Builder("Dishwasher", 24.dp, 24.dp, 24f, 24f)
        .apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 2f)
                lineTo(20f, 2f)
                lineTo(20f, 22f)
                lineTo(4f, 22f)
                close()
                moveTo(4f, 7f)
                lineTo(20f, 7f)
                moveTo(7f, 4.5f)
                lineTo(7.2f, 4.5f)
                moveTo(14f, 4.5f)
                lineTo(17f, 4.5f)
                moveTo(7f, 18f)
                lineTo(17f, 18f)
                moveTo(8f, 17f)
                curveTo(6f, 14f, 6f, 11f, 8f, 10f)
                moveTo(12f, 17f)
                curveTo(10f, 14f, 10f, 11f, 12f, 10f)
                moveTo(16f, 17f)
                curveTo(14f, 14f, 14f, 11f, 16f, 10f)
            }
        }
        .build()
}
