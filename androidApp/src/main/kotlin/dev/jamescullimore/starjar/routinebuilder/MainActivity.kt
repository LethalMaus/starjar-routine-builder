package dev.jamescullimore.starjar.routinebuilder

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import dev.jamescullimore.starjar.routinebuilder.ai.ParentGenerator
import dev.jamescullimore.starjar.routinebuilder.shared.ParentApp
import dev.jamescullimore.starjar.routinebuilder.shared.PlatformBridge
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), PlatformBridge {
    private var incomingDocument by mutableStateOf<String?>(null)
    private var documentCallback: ((String?) -> Unit)? = null
    private val generator by lazy { ParentGenerator(this) }
    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val callback = documentCallback
            documentCallback = null
            if (uri == null) callback?.invoke(null)
            else lifecycleScope.launch { callback?.invoke(readDocument(uri) ?: "{}") }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receive(intent)
        setContent {
            ParentApp(this, incomingDocument, consumeDocument = { incomingDocument = null },
                initialLanguage = LocalConfiguration.current.locales[0].toLanguageTag(), judgingAccess = BuildConfig.JUDGING_ACCESS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receive(intent)
    }

    private fun receive(intent: Intent) {
        val uri =
            when (intent.action) {
                Intent.ACTION_SEND ->
                    if (android.os.Build.VERSION.SDK_INT >= 33)
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            } ?: return
        if (uri.scheme != "content") return
        lifecycleScope.launch { incomingDocument = readDocument(uri) ?: "{}" }
    }

    private suspend fun readDocument(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes =
                    requireNotNull(contentResolver.openInputStream(uri)).use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (output.size() <= 200000) {
                            val count =
                                input.read(buffer, 0, minOf(buffer.size, 200001 - output.size()))
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                require(bytes.size <= 200000)
                bytes.toString(Charsets.UTF_8)
            }
                .getOrNull()
        }

    override fun supportsReporting() = com.google.firebase.FirebaseApp.getApps(this).isNotEmpty()

    override fun reportSuggestion(id: String, name: String, description: String, comment: String, completion: (Boolean) -> Unit) {
        com.google.firebase.appcheck.FirebaseAppCheck.getInstance().getAppCheckToken(false)
            .addOnFailureListener { completion(false) }
            .addOnSuccessListener { token ->
                lifecycleScope.launch {
                    val received = withContext(Dispatchers.IO) {
                        runCatching {
                            val connection = java.net.URL("https://europe-west3-${BuildConfig.FIREBASE_PROJECT_ID}.cloudfunctions.net/reportSuggestion").openConnection() as javax.net.ssl.HttpsURLConnection
                            try {
                                connection.requestMethod = "POST"
                                connection.connectTimeout = 15000
                                connection.readTimeout = 20000
                                connection.doOutput = true
                                connection.setRequestProperty("Content-Type", "application/json")
                                connection.setRequestProperty("X-Firebase-AppCheck", token.token)
                                val body = org.json.JSONObject().put("id", id).put("name", name).put("description", description).put("comment", comment).toString()
                                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                                connection.responseCode == 200
                            } finally { connection.disconnect() }
                        }.getOrDefault(false)
                    }
                    completion(received)
                }
            }
    }

    override fun newId() = UUID.randomUUID().toString()

    private fun localFile(name: String) = File(noBackupFilesDir, name)

    override fun loadDraft(): String? {
        val current = localFile("parent-plan.json")
        if (current.exists()) return current.readText()
        val legacy = localFile("editor.json")
        if (!legacy.exists()) return null
        return dev.jamescullimore.starjar.routinebuilder.shared.PlanTransfer.editable(
                LegacyPlans.draft(legacy.readText())
            )
            .also { saveDraft(it) }
    }

    override fun saveDraft(document: String) = writeLocal("parent-plan.json", document)

    override fun loadLibrary(): String {
        val current = localFile("parent-library.json")
        if (current.exists()) return current.readText()
        val legacy =
            dev.jamescullimore.starjar.routinebuilder.core
                .FileRoutineRepository(localFile("routines"))
                .load()
        val entries = legacy.map {
            kotlinx.serialization.json.Json.parseToJsonElement(
                dev.jamescullimore.starjar.routinebuilder.shared.PlanTransfer.editable(
                    LegacyPlans.saved(it)
                )
            )
        }
        return kotlinx.serialization.json.JsonArray(entries).toString().also { saveLibrary(it) }
    }

    override fun saveLibrary(document: String) = writeLocal("parent-library.json", document)

    private fun writeLocal(name: String, text: String) {
        val atomic = androidx.core.util.AtomicFile(localFile(name))
        val stream = atomic.startWrite()
        try {
            stream.write(text.toByteArray())
            atomic.finishWrite(stream)
        } catch (error: Exception) {
            atomic.failWrite(stream)
            throw error
        }
    }

    override fun openDocument(completion: (String?) -> Unit) {
        documentCallback = completion
        picker.launch(arrayOf("application/json", "text/plain"))
    }

    override fun share(document: String, filename: String) {
        require(filename in listOf("starjar-templates.json", "starjar-ideas-plan.json"))
        val directory = File(cacheDir, "shared-plans").apply { mkdirs() }
        // Two fixed filenames bound cache usage; only explicitly chosen documents are shared.
        val file = File(directory, filename).apply { writeText(document) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("application/json")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .apply { clipData = ClipData.newRawUri("Plan", uri) }
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, null))
    }

    override fun localAvailability(completion: (String) -> Unit) {
        lifecycleScope.launch {
            completion(
                runCatching { if (generator.localAvailable()) "available" else "unavailable" }
                    .getOrDefault("unavailable")
            )
        }
    }

    override fun generate(
        prompt: String,
        cloudAllowed: Boolean,
        advanced: Boolean,
        completion: (String?, String?, String?) -> Unit,
    ) {
        lifecycleScope.launch {
            runCatching { generator.generate(prompt, cloudAllowed, advanced) }
                .onSuccess { completion(it.first, it.second, null) }
                .onFailure { completion(null, null, it.javaClass.simpleName) }
        }
    }
}
