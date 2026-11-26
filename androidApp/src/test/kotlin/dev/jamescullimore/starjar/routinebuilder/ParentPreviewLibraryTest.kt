package dev.jamescullimore.starjar.routinebuilder

import app.cash.paparazzi.*
import com.android.resources.Density
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(Parameterized::class)
@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
class ParentPreviewLibraryTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
    private val width: Int,
    private val height: Int,
    private val font: Float,
    private val dark: Boolean,
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}-{1}x{2}-{3}-{4}")
        fun cases(): List<Array<Any>> {
            val previews =
                AndroidComposablePreviewScanner()
                    .scanPackageTrees("dev.jamescullimore.starjar.routinebuilder")
                    .getPreviews()
                    .filter { it.methodName.startsWith("Parent") }
                    .distinctBy { it.methodName }
                    .sortedWith(compareBy<ComposablePreview<AndroidPreviewInfo>> { it.methodName.startsWith("ParentFeatures") }.thenBy { it.methodName })
            check(previews.size == 14)
            return previews.flatMap { p ->
                listOf(
                    arrayOf(p, 411, 891, 1f, false),
                    arrayOf(p, 320, 568, 2f, false),
                    arrayOf(p, 840, 1180, 1f, false),
                    arrayOf(p, 411, 891, 1f, true),
                )
            }
        }
    }

    @get:Rule
    val paparazzi =
        Paparazzi(
            deviceConfig =
                DeviceConfig.NEXUS_5.copy(
                    screenWidth = width,
                    screenHeight = height,
                    density = Density.MEDIUM,
                    xdpi = 160,
                    ydpi = 160,
                    fontScale = font,
                    softButtons = false,
                    nightMode =
                        if (dark) com.android.resources.NightMode.NIGHT
                        else com.android.resources.NightMode.NOTNIGHT,
                ),
            showSystemUi = false,
            useDeviceResolution = true,
        )

    @Test
    fun render() {
        paparazzi.snapshot(
            name =
                "${preview.methodName}-${width}x${height}-${font}-${if(dark) "dark" else "light"}"
        ) {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                org.jetbrains.compose.resources.PreviewContextConfigurationEffect()
                preview()
            }
        }
    }
}
