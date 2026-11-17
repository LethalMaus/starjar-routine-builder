package dev.jamescullimore.starjar.routinebuilder

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.ai.DownloadStatus
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.type.PublicPreviewAPI
import dev.jamescullimore.starjar.routinebuilder.ai.*
import dev.jamescullimore.starjar.routinebuilder.core.*
import dev.jamescullimore.starjar.routinebuilder.ui.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/** Deterministic lifecycle tests; fake inference is never reported as real model evidence. */
@OptIn(PublicPreviewAPI::class)
@RunWith(AndroidJUnit4::class)
class GenerationRecoveryTest {
 private class Engine:RoutineGenerator {
  val answer=CompletableDeferred<GenerationOutput>();var calls=0;var policy:GenerationPolicy?=null
  override suspend fun readiness()=OnDeviceModelStatus.UNAVAILABLE
  override fun download():Flow<DownloadStatus> = emptyFlow()
  override suspend fun generate(request:RoutineRequest,policy:GenerationPolicy,cloudDiagnostic:Boolean):GenerationOutput {
   calls++;this.policy=policy
   // Deliberately behave like a transport which cannot cancel its already-sent response.
   return withContext(NonCancellable) { answer.await() }
  }
 }
 private fun main(block:()->Unit)=InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
 private fun until(predicate:()->Boolean) { val end=System.currentTimeMillis()+5000;while(!predicate()&&System.currentTimeMillis()<end)Thread.sleep(20);assertTrue(predicate()) }
 private fun withModel(block:(BuilderViewModel,Engine)->Unit) {
  val context=ApplicationProvider.getApplicationContext<Application>()
  val directory=File(context.cacheDir,"recovery-"+UUID.randomUUID()).apply { mkdirs() }
  val app=object:Application(){init { attachBaseContext(context) };override fun getNoBackupFilesDir()=directory}
  val engine=Engine();val store=ViewModelStore();lateinit var model:BuilderViewModel
  main { model=BuilderViewModel(app,engine);store.put("test",model) }
  try { until { model.state.value.readiness==ModelReadiness.UNAVAILABLE };block(model,engine) }
  finally { engine.answer.cancel();main { store.clear() };directory.deleteRecursively() }
 }
 private fun setup(model:BuilderViewModel) { main { model.action(BuilderAction.ManualRoutine);model.action(BuilderAction.SetTitle("Keep my routine"));model.action(BuilderAction.SetTaskName(0,"Keep my edited task"));model.action(BuilderAction.SetGoal("Put books away")) } }
 private fun result()=GenerationOutput(GeneratedRoutineDraft(listOf("Late generated task"),null),GenerationReceipt(GenerationSource.ON_DEVICE,"fake-test-engine",1))
 @Test fun localDefaultDoesNotAllowForcedCloudWithoutConsent()=withModel { model,engine ->
  setup(model);assertEquals(GenerationPolicy.LOCAL_ONLY,model.state.value.policy)
  val before=model.state.value
  main { model.action(BuilderAction.CloudDiagnostic) };assertEquals(0,engine.calls)
  if(BuildConfig.DEBUG) assertTrue(model.state.value.isError) else assertEquals(before,model.state.value)
 }
 @Test fun editDuringGenerationWinsOverUncancellableLateResponse()=withModel { model,engine ->
  setup(model);main { model.action(BuilderAction.Generate) };until { engine.calls==1 }
  main { model.action(BuilderAction.SetTaskName(0,"Newest edit")) };engine.answer.complete(result());Thread.sleep(150)
  assertEquals("Newest edit",model.state.value.tasks.first().name);assertFalse(model.state.value.busy);assertNull(model.state.value.source)
  assertEquals(GenerationPolicy.LOCAL_ONLY,engine.policy)
 }
 @Test fun backgroundCancelsWithoutReplacingReviewedEdits()=withModel { model,engine ->
  setup(model);main { model.action(BuilderAction.Generate) };until { engine.calls==1 };main { model.backgrounded() }
  engine.answer.complete(result());Thread.sleep(150)
  assertEquals("Keep my edited task",model.state.value.tasks.first().name);assertFalse(model.state.value.busy)
 }
 @Test fun offlineFailureKeepsEditsAndAllowsRetry()=withModel { model,engine ->
  setup(model);main { model.action(BuilderAction.Generate) };until { engine.calls==1 };engine.answer.completeExceptionally(java.io.IOException("synthetic offline"))
  until { !model.state.value.busy };assertEquals("Keep my edited task",model.state.value.tasks.first().name);assertTrue(model.state.value.isError)
  main { model.action(BuilderAction.Generate) };until { engine.calls==2 };until { !model.state.value.busy };assertEquals("Keep my routine",model.state.value.title)
 }
}
