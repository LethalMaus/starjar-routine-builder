package dev.jamescullimore.starjar.routinebuilder.shared
/** Implemented by each native host. No second Kotlin/Native framework is needed. */
interface PlatformBridge {
    fun supportsReporting(): Boolean = false

    fun reportSuggestion(id: String, name: String, description: String, comment: String, completion: (Boolean) -> Unit) { completion(false) }

    fun newId(): String

    fun loadDraft(): String?

    fun saveDraft(document: String)

    fun loadLibrary(): String

    fun saveLibrary(document: String)

    fun share(document: String, filename: String)

    fun openDocument(completion: (String?) -> Unit)

    fun localAvailability(completion: (String) -> Unit)

    fun generate(
        prompt: String,
        cloudAllowed: Boolean,
        advanced: Boolean,
        completion: (String?, String?, String?) -> Unit,
    )
}
