package dev.jamescullimore.starjar.routinebuilder

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Tasks
import com.google.firebase.appcheck.FirebaseAppCheck
import dev.jamescullimore.starjar.routinebuilder.ai.FirebaseRoutineGenerator
import dev.jamescullimore.starjar.routinebuilder.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.TimeUnit

/** Opt-in only. Three bounded synthetic requests; never run in ordinary CI. */
@RunWith(AndroidJUnit4::class)
class LiveInferenceTest {
 @Test fun protectedCloudAndFallbackRecordActualSource() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("liveAi") == "true")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  val output=File(context.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
  ActivityScenario.launch(MainActivity::class.java).use {
   val token=Tasks.await(FirebaseAppCheck.getInstance().getAppCheckToken(true),30,TimeUnit.SECONDS)
   assertTrue(token.token.isNotBlank()) // Never log or persist the attestation token.
   val generator=FirebaseRoutineGenerator(context.assets.open("routine-instructions.txt").bufferedReader().use { it.readText() })
   val cases=listOf(
    Triple("cloud-en",RoutineRequest("Help us remember teeth, the schoolbag and shoes in the morning.","en",3,true,listOf("no_purchases","short_tasks")),true),
    Triple("cloud-de",RoutineRequest("Morgens an Zähneputzen, Schultasche und Schuhe denken.","de",3,true,listOf("no_purchases","short_tasks")),true),
    Triple("fallback-en",RoutineRequest("Two short jobs: put toys away and put books on the shelf.","en",2,false,listOf("short_tasks")),false)
   )
   val selected=InstrumentationRegistry.getArguments().getString("liveCase")
   val selectedCases=cases.filter { selected==null || it.first==selected }
   require(selectedCases.isNotEmpty())
   for((id,request,forceCloud) in selectedCases) {
    val result=runBlocking { withTimeout(90000) { generator.generate(request,GenerationPolicy.CLOUD_ALLOWED,forceCloud) } }
    if(forceCloud) assertEquals(GenerationSource.IN_CLOUD,result.receipt.source)
    assertEquals(request.taskCount,result.draft.tasks.size)
    val evidence=buildJsonObject {
     put("case",id);put("timestampMs",System.currentTimeMillis());put("source",result.receipt.source.name)
     put("model",result.receipt.model);put("durationMs",result.receipt.durationMs);put("appCheckTokenObtained",true)
     put("attestationProvider",if(!BuildConfig.DEBUG || BuildConfig.USE_RECAPTCHA) "recaptcha-enterprise" else "debug");put("device",android.os.Build.MODEL)
     put("synthetic",true);put("tasks",JsonArray(result.draft.tasks.map(::JsonPrimitive)))
     put("reward",result.draft.reward?.let(::JsonPrimitive)?:JsonNull)
    }
    File(output,"$id.json").writeText(Json { prettyPrint=true }.encodeToString(evidence))
   }
  }
 }
}
