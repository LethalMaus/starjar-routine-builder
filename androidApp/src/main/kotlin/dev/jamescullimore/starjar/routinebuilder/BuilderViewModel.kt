package dev.jamescullimore.starjar.routinebuilder

import android.app.Application
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.ai.DownloadStatus
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.type.PublicPreviewAPI
import dev.jamescullimore.starjar.routinebuilder.ai.*
import dev.jamescullimore.starjar.routinebuilder.core.*
import dev.jamescullimore.starjar.routinebuilder.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

sealed interface BuilderEffect { data class Export(val json: String): BuilderEffect; data class Copy(val json: String): BuilderEffect }
@Serializable private data class DraftItem(val name: String, val points: String)
@Serializable private data class EditorDraft(val id: String, val title: String, val language: String, val tasks: List<DraftItem>, val reward: DraftItem?, val taskCount: Int = 3, val includeReward: Boolean = true, val constraints: Set<String> = emptySet(), val unsaved: Boolean = true)

@OptIn(PublicPreviewAPI::class)
class BuilderViewModel @JvmOverloads constructor(app: Application, generator: RoutineGenerator = FirebaseRoutineGenerator(app.assets.open("routine-instructions.txt").bufferedReader().use { it.readText() })) : AndroidViewModel(app) {
 private val allowance = CloudAllowance(File(app.noBackupFilesDir, "cloud-usage.json"))
 private val generator = AllowanceRoutineGenerator(generator, allowance)
 private val language = if (app.resources.configuration.locales[0].language == "de") "de" else "en"
 private val mutable = MutableStateFlow(BuilderUiState(uiLanguage = language, outputLanguage = language))
 val state = mutable.asStateFlow()
 private val effectsChannel = Channel<BuilderEffect>(Channel.BUFFERED)
 val effects = effectsChannel.receiveAsFlow()
 private val repository = FileRoutineRepository(File(app.noBackupFilesDir, "routines"))
 private val draftFile = AtomicFile(File(app.noBackupFilesDir, "editor.json"))
 private val io = Dispatchers.IO.limitedParallelism(1)
 private var routineId = UUID.randomUUID().toString()
 private var epoch = 0L
 private var generation: Job? = null
 private var readinessJob: Job? = null
 private val writes = Channel<EditorDraft>(Channel.CONFLATED)
 init {
  viewModelScope.launch {
   try {
    val loaded = withContext(io) { repository.load() }
    mutable.update { it.copy(savedRoutines = loaded) }
    val draft = try { withContext(io) { if (draftFile.baseFile.exists()) Json.decodeFromString<EditorDraft>(draftFile.openRead().bufferedReader().use { it.readText() }) else null } }
    catch (_: Exception) { message("The unfinished draft could not be read. Saved routines are still available.","Der Entwurf konnte nicht gelesen werden. Gespeicherte Routinen sind weiterhin verfügbar.",true); null }
    mutable.update { s ->
     if (epoch == 0L && draft != null) {
      routineId = draft.id
      s.copy(title = draft.title, outputLanguage = draft.language, tasks = draft.tasks.map { EditableItem(it.name,it.points) }, reward = draft.reward?.let { EditableItem(it.name,it.points) }, taskCount = draft.taskCount, includeReward = draft.includeReward, constraints = draft.constraints, savedRoutines = loaded, hasUnsavedChanges = draft.unsaved)
     } else s.copy(savedRoutines = loaded)
    }
   } catch (_: Exception) { message("Saved routines could not be read. Existing files were kept.", "Gespeicherte Routinen konnten nicht gelesen werden. Die Dateien bleiben erhalten.", true) }
  }
  viewModelScope.launch(io) { for (draft in writes) {
   var stream: java.io.FileOutputStream? = null
   try { stream = draftFile.startWrite(); stream.write(Json.encodeToString(draft).toByteArray()); draftFile.finishWrite(stream) }
   catch (_: Exception) { draftFile.failWrite(stream); message("Draft could not be saved. Keep this screen open.", "Der Entwurf konnte nicht gespeichert werden. Lass diesen Bildschirm geöffnet.", true) }
  } }
  refreshAllowance()
  readiness()
 }
 private fun message(en: String, de: String, error: Boolean = false) { mutable.update { it.copy(message = if (language == "de") de else en, isError = error) } }
 private fun invalidate() { epoch++; generation?.cancel(); generation = null; mutable.update { it.copy(busy = false) } }
 fun backgrounded() { if (state.value.busy) { invalidate(); message("Generation stopped when the app left the foreground. Your edits are here.", "Die Erstellung wurde im Hintergrund angehalten. Deine Änderungen bleiben erhalten.") } }
 private fun changed(transform: (BuilderUiState)->BuilderUiState) { invalidate(); mutable.update { transform(it).copy(hasUnsavedChanges = true, message = null) }; persistEditor() }
 private fun persistEditor() { val s=state.value; writes.trySend(EditorDraft(routineId,s.title,s.outputLanguage,s.tasks.map { DraftItem(it.name,it.points) },s.reward?.let { DraftItem(it.name,it.points) },s.taskCount,s.includeReward,s.constraints,s.hasUnsavedChanges)) }
 fun action(action: BuilderAction) {
  when(action) {
   is BuilderAction.Navigate -> { invalidate(); mutable.update { it.copy(page = action.page) } }
   BuilderAction.NewRoutine -> { invalidate(); routineId=UUID.randomUUID().toString(); mutable.update { BuilderUiState(page=BuilderPage.CREATE, uiLanguage=language,outputLanguage=language,policy=it.policy,readiness=it.readiness,savedRoutines=it.savedRoutines,cloudRemaining=it.cloudRemaining) };persistEditor() }
   BuilderAction.ManualRoutine -> { invalidate(); mutable.update { s -> s.copy(page=BuilderPage.REVIEW,title=s.title.ifBlank { if(language=="de") "Meine Routine" else "My routine" },tasks=s.tasks.ifEmpty { List(s.taskCount){EditableItem()} },reward=s.reward ?: if(s.includeReward) EditableItem("","30") else null,hasUnsavedChanges=true) };persistEditor() }
   is BuilderAction.UsePreset -> {
    val goals = if (state.value.outputLanguage=="de") mapOf("morning" to "Eine kurze Schulmorgen-Routine mit Zähneputzen, Schultasche und Schuhen.","bedtime" to "Eine ruhige Abendroutine: Spielsachen wegräumen, Schlafanzug anziehen und ein Buch lesen.","home" to "Zu Hause mithelfen: Bücher wegräumen, den Tisch decken und Pflanzen gießen.") else mapOf("morning" to "A short school morning routine: brush teeth, prepare a schoolbag and put shoes on.","bedtime" to "A calm bedtime routine: tidy toys, put on pyjamas and read a book.","home" to "Help at home: put books away, set the table and water plants.")
    changed { it.copy(goal=goals[action.id] ?: goals.values.first(),page=BuilderPage.CREATE) }
   }
   is BuilderAction.SetGoal -> { invalidate(); mutable.update { it.copy(goal=action.value.take(1200)) } }
   is BuilderAction.SetOutputLanguage -> changed { it.copy(outputLanguage=action.value) }
   is BuilderAction.SetTaskCount -> { invalidate();mutable.update { it.copy(taskCount=action.value.coerceIn(1,3)) };persistEditor() }
   is BuilderAction.SetIncludeReward -> { invalidate();mutable.update { it.copy(includeReward=action.value) };persistEditor() }
   is BuilderAction.ToggleConstraint -> { invalidate();mutable.update { it.copy(constraints=if(action.id in it.constraints) it.constraints-action.id else it.constraints+action.id) };persistEditor() }
   is BuilderAction.SetPolicy -> {
    val wasBusy=state.value.busy;invalidate();mutable.update { it.copy(policy=action.value) }
    if(wasBusy) message("Policy changed and generation cancelled. Requests already sent cannot be recalled.","Die Einstellung wurde geändert und die Erstellung abgebrochen. Bereits gesendete Anfragen lassen sich nicht zurückholen.")
   }
   BuilderAction.Generate -> generate(false)
   BuilderAction.CloudDiagnostic -> if(BuildConfig.DEBUG) generate(true)
   BuilderAction.CancelGeneration -> { invalidate();message("Generation cancelled. Your edits are unchanged.","Erstellung abgebrochen. Deine Änderungen bleiben erhalten.") }
   is BuilderAction.SetTitle -> changed { it.copy(title=action.value.take(120)) }
   is BuilderAction.SetTaskName -> changed { it.copy(tasks=it.tasks.mapIndexed { i,t -> if(i==action.index)t.copy(name=action.value.take(120)) else t }) }
   is BuilderAction.SetTaskPoints -> changed { it.copy(tasks=it.tasks.mapIndexed { i,t -> if(i==action.index)t.copy(points=action.value.take(10)) else t }) }
   is BuilderAction.RemoveTask -> changed { it.copy(tasks=it.tasks.filterIndexed { i,_ -> i!=action.index }) }
   BuilderAction.AddTask -> changed { if(it.tasks.size<3) it.copy(tasks=it.tasks+EditableItem()) else it }
   is BuilderAction.SetRewardName -> changed { it.copy(reward=it.reward?.copy(name=action.value.take(120))) }
   is BuilderAction.SetRewardPoints -> changed { it.copy(reward=it.reward?.copy(points=action.value.take(10))) }
   BuilderAction.RemoveReward -> changed { it.copy(reward=null) }
   BuilderAction.AddReward -> changed { it.copy(reward=EditableItem("","30")) }
   BuilderAction.Save -> save()
   BuilderAction.Export -> export(false)
   BuilderAction.Copy -> export(true)
   is BuilderAction.OpenSaved -> {
    val r=state.value.savedRoutines.find { it.id==action.id } ?: return
    invalidate();routineId=r.id;mutable.update { it.copy(page=BuilderPage.REVIEW,title=r.title,outputLanguage=r.language,tasks=r.tasks.map { t->EditableItem(t.name,t.pointAmount.toString()) },reward=r.reward?.let { t->EditableItem(t.name,t.pointAmount.toString()) },source=null,hasUnsavedChanges=false,message=null) };persistEditor()
   }
   is BuilderAction.DeleteSaved -> viewModelScope.launch { try { val list=withContext(io) { repository.delete(action.id);repository.load() };if(routineId==action.id) { invalidate();routineId=UUID.randomUUID().toString();mutable.update { it.copy(title="",tasks=emptyList(),reward=null,source=null,hasUnsavedChanges=false) };persistEditor() };mutable.update { it.copy(savedRoutines=list) };message("Routine deleted.","Routine gelöscht.") } catch (_: Exception) { message("Could not delete this routine.","Diese Routine konnte nicht gelöscht werden.",true) } }
   BuilderAction.DismissMessage -> mutable.update { it.copy(message=null) }
   BuilderAction.RefreshReadiness -> readiness()
   BuilderAction.DownloadModel -> download()
  }
 }
 private fun reviewed(): ReviewedRoutine {
  val s=state.value
  return ReviewedRoutine(routineId,s.title.trim(),s.outputLanguage,s.tasks.map { RoutineItem(it.name.trim(),RoutineValidation.points(it.points)) },s.reward?.let { RoutineItem(it.name.trim(),RoutineValidation.points(it.points)) }).also(RoutineValidation::reviewed)
 }
 private fun validationMessage() = message("Check the title, task names and stars. Use whole numbers from 1 to 1,000,000 and at least one task or reward.","Prüfe Titel, Namen und Sterne. Verwende ganze Zahlen von 1 bis 1.000.000 und mindestens eine Aufgabe oder Belohnung.",true)
 private fun save() {
  val r=try { reviewed() } catch (_: Exception) { validationMessage();return }
  val revision=epoch;mutable.update { it.copy(saving=true) }
  viewModelScope.launch { try {
   val list=withContext(io) { repository.save(r);repository.load() }
   mutable.update { it.copy(savedRoutines=list,saving=false,hasUnsavedChanges=if(epoch==revision) false else it.hasUnsavedChanges) }
   persistEditor();message("Routine saved on this device.","Routine auf diesem Gerät gespeichert.")
  } catch (_: Exception) { mutable.update { it.copy(saving=false) };message("Could not save. Your edits are still here.","Speichern fehlgeschlagen. Deine Änderungen bleiben erhalten.",true) } }
 }
 private fun export(copy: Boolean) {
  val encoded=try { TemplateExporter.encode(reviewed()) } catch (_:Exception) { validationMessage();return }
  viewModelScope.launch { effectsChannel.send(if(copy)BuilderEffect.Copy(encoded) else BuilderEffect.Export(encoded)) }
 }
 fun exportFinished(success:Boolean, cancelled:Boolean=false) {
  if(cancelled) return
  if(success) message("JSON file saved.","JSON-Datei gespeichert.") else message("Could not write the file. Your routine is unchanged.","Die Datei konnte nicht geschrieben werden. Deine Routine bleibt erhalten.",true)
 }
 private fun refreshAllowance() { viewModelScope.launch {
  val remaining = try { withContext(io) { allowance.status().remaining } } catch (_: CloudAllowanceUnavailable) { null }
  mutable.update { it.copy(cloudRemaining = remaining) }
 } }
 private fun generate(cloudDiagnostic:Boolean) {
  val s=state.value;val request=RoutineRequest(s.goal,s.outputLanguage,s.taskCount,s.includeReward,s.constraints.toList())
  try { RoutineValidation.request(request) } catch (_: Exception) { message("Describe a routine first (up to 1,200 characters).","Beschreibe zuerst eine Routine (bis zu 1.200 Zeichen).",true);return }
  if(cloudDiagnostic && s.policy!=GenerationPolicy.CLOUD_ALLOWED) { message("Allow cloud processing before running this diagnostic.","Erlaube zuerst die Cloud-Verarbeitung, um diesen Test auszuführen.",true);return }
  invalidate();val id=epoch;mutable.update { it.copy(busy=true,message=null) }
  generation=viewModelScope.launch {
   try {
    val result=withTimeout(90_000) { generator.generate(request,s.policy,cloudDiagnostic) }
    if(epoch!=id) return@launch
    mutable.update { it.copy(page=BuilderPage.REVIEW,busy=false,title=it.title.ifBlank { if(language=="de") "Meine Routine" else "My routine" },tasks=result.draft.tasks.map { n->EditableItem(n,"5") },reward=result.draft.reward?.let { n->EditableItem(n,"30") },source=result.receipt.source,hasUnsavedChanges=true,diagnostics="${result.receipt.source} · ${result.receipt.model} · ${result.receipt.durationMs} ms") };persistEditor()
   } catch (e: TimeoutCancellationException) { if(epoch==id) message("Generation timed out. Your edits are still here.","Die Erstellung hat zu lange gedauert. Deine Änderungen bleiben erhalten.",true) }
   catch (e: CancellationException) { throw e }
   catch (e: Exception) { if(epoch==id) {
    mutable.update { it.copy(diagnostics=e.javaClass.simpleName) }
    when(e) {
     is CloudAllowanceExceeded -> message("Cloud allowance used up for now. Switch to on-device processing or continue manually. Attempts become available again after 24 hours.","Dein Cloud-Kontingent ist vorerst aufgebraucht. Wechsle zur Verarbeitung auf dem Gerät oder schreibe die Routine selbst. Nach 24 Stunden werden die Versuche wieder freigegeben.",true)
     is CloudAllowanceUnavailable -> message("Cloud usage could not be checked. On-device processing and manual routines are still available.","Die Cloud-Nutzung konnte nicht geprüft werden. Du kannst weiterhin Vorschläge auf dem Gerät erstellen oder Routinen selbst schreiben.",true)
     is ConfigurationRequired -> message("AI setup is not complete. You can create, save and export manually.","Die KI ist noch nicht eingerichtet. Du kannst Routinen selbst erstellen, speichern und exportieren.",true)
     is NeedsInput -> message("Add a little more detail about the routine you want.","Beschreibe etwas genauer, welche Routine du erstellen möchtest.",true)
     else -> message("Suggestions are unavailable. Your edits are still here; try again or continue manually.","Vorschläge sind gerade nicht verfügbar. Deine Änderungen bleiben erhalten. Versuche es erneut oder erstelle die Routine selbst.",true)
    }
   } } finally { refreshAllowance(); if(epoch==id) mutable.update { it.copy(busy=false) } }
  }
 }
 private fun readiness() {
  readinessJob?.cancel();readinessJob=viewModelScope.launch {
   if(!FirebaseRoutineGenerator.configured) { mutable.update { it.copy(readiness=ModelReadiness.CONFIG_REQUIRED) };return@launch }
   mutable.update { it.copy(readiness=ModelReadiness.CHECKING) }
   try { val ready=withTimeout(15_000) { generator.readiness() };mutable.update { it.copy(readiness=when(ready){ OnDeviceModelStatus.AVAILABLE->ModelReadiness.READY;OnDeviceModelStatus.DOWNLOADABLE->ModelReadiness.DOWNLOADABLE;OnDeviceModelStatus.DOWNLOADING->ModelReadiness.DOWNLOADING;else->ModelReadiness.UNAVAILABLE }) } }
   catch(_:TimeoutCancellationException){mutable.update { it.copy(readiness=ModelReadiness.UNAVAILABLE) }} catch(e:CancellationException){throw e} catch(_:Exception){mutable.update { it.copy(readiness=ModelReadiness.UNAVAILABLE) }}
  }
 }
 private fun download() {
  readinessJob?.cancel();readinessJob=viewModelScope.launch {
   var total=0L;mutable.update { it.copy(readiness=ModelReadiness.DOWNLOADING) }
   try { generator.download().collect { status -> when(status) {
    is DownloadStatus.DownloadStarted -> total=status.bytesToDownload
    is DownloadStatus.DownloadInProgress -> mutable.update { it.copy(downloadProgress=if(total>0)status.totalBytesDownloaded.toFloat()/total else null) }
    is DownloadStatus.DownloadFailed -> throw status.exception
    DownloadStatus.DownloadCompleted -> mutable.update { it.copy(readiness=ModelReadiness.READY,downloadProgress=null) }
   } } } catch(e:CancellationException){throw e} catch(_:Exception){ mutable.update { it.copy(readiness=ModelReadiness.UNAVAILABLE,downloadProgress=null) };message("Model download failed. Manual routines still work.","Das Modell konnte nicht geladen werden. Du kannst Routinen weiterhin selbst erstellen.",true) }
  }
 }
}
