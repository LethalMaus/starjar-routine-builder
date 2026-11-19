package dev.jamescullimore.starjar.routinebuilder.shared

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCompatibilityMapping

@OptIn(ExperimentalForeignApi::class)
actual fun normalizedName(value: String): String =
    (value as NSString).precomposedStringWithCompatibilityMapping.trim().uppercase()
