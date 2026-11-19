package dev.jamescullimore.starjar.routinebuilder.shared

/** Native interfaces follow the device language; unsupported languages use English. */
fun appLanguage(languageTag: String): String =
    if (languageTag.substringBefore('-').substringBefore('_').lowercase() == "de") "de" else "en"
