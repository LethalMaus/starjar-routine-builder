package dev.jamescullimore.starjar.routinebuilder.shared
actual fun normalizedName(value: String) =
    java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
        .trim()
        .uppercase(java.util.Locale.ROOT)
