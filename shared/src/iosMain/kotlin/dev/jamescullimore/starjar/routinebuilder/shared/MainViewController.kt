package dev.jamescullimore.starjar.routinebuilder.shared

import androidx.compose.runtime.*
import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import platform.Foundation.NSLocale
import platform.Foundation.preferredLanguages

class DocumentInbox {
    var document: String? by mutableStateOf(null)
}

fun MainViewController(bridge: PlatformBridge, inbox: DocumentInbox, judgingAccess: Boolean = false) = ComposeUIViewController(configure = {
    // The scrollable form handles IME insets. Native panning would adjust it a second time.
    onFocusBehavior = OnFocusBehavior.DoNothing
}) {
    ParentApp(bridge, inbox.document, consumeDocument = { inbox.document = null },
        initialLanguage = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en", judgingAccess = judgingAccess)
}
