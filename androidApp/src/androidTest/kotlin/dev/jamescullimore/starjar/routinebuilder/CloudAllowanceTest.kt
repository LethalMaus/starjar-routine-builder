package dev.jamescullimore.starjar.routinebuilder
import androidx.test.core.app.ApplicationProvider
import android.app.Application
import com.google.firebase.ai.DownloadStatus
import com.google.firebase.ai.OnDeviceModelStatus
import com.google.firebase.ai.type.PublicPreviewAPI
import dev.jamescullimore.starjar.routinebuilder.ai.*
import dev.jamescullimore.starjar.routinebuilder.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

@OptIn(PublicPreviewAPI::class)
class CloudAllowanceTest {
 private class Engine : RoutineGenerator {
  var calls=0;var source=GenerationSource.IN_CLOUD;var error:Exception?=null
  override suspend fun readiness()=OnDeviceModelStatus.UNAVAILABLE
  override fun download():Flow<DownloadStatus> = emptyFlow()
  override suspend fun generate(request:RoutineRequest,policy:GenerationPolicy,cloudDiagnostic:Boolean):GenerationOutput {
   calls++;error?.let { throw it };return GenerationOutput(GeneratedRoutineDraft(listOf("Brush teeth"),null),GenerationReceipt(source,"fake-test-engine",1))
  }
 }
 private fun scenario(block:suspend(CloudAllowance,Engine,RoutineGenerator)->Unit)=runBlocking {
  val app=ApplicationProvider.getApplicationContext<Application>();val dir=File(app.cacheDir,"allowance-"+UUID.randomUUID()).apply { mkdirs() }
  try {val q=CloudAllowance(File(dir,"usage.json"));val engine=Engine();block(q,engine,AllowanceRoutineGenerator(engine,q))}finally{dir.deleteRecursively()}
 }
 private val request=RoutineRequest("Brush teeth",taskCount=1,includeReward=false)
 @Test fun elevenCloudCallsOnlyReachEngineTenTimes()=scenario { q,e,g ->
  repeat(10){g.generate(request,GenerationPolicy.CLOUD_ALLOWED)}
  try {g.generate(request,GenerationPolicy.CLOUD_ALLOWED);fail("Must block")}catch(_:CloudAllowanceExceeded){}
  assertEquals(10,e.calls);assertEquals(0,q.status().remaining)
  e.source=GenerationSource.ON_DEVICE;g.generate(request,GenerationPolicy.LOCAL_ONLY);assertEquals(11,e.calls)
 }
 @Test fun provenLocalResultsRefundAllowance()=scenario { q,e,g ->
  e.source=GenerationSource.ON_DEVICE;repeat(12){g.generate(request,GenerationPolicy.CLOUD_ALLOWED)};assertEquals(10,q.status().remaining)
 }
 @Test fun failuresAndCancellationRetainReservations()=scenario {q,e,g ->
  e.error=java.io.IOException("synthetic offline")
  try{g.generate(request,GenerationPolicy.CLOUD_ALLOWED);fail()}catch(_:java.io.IOException){}
  e.error=CancellationException("synthetic cancellation")
  try{g.generate(request,GenerationPolicy.CLOUD_ALLOWED);fail()}catch(_:CancellationException){}
  assertEquals(8,q.status().remaining)
 }
 @Test fun invalidInputConsumesNothing()=scenario {q,e,g ->
  try{g.generate(request.copy(goal=""),GenerationPolicy.CLOUD_ALLOWED);fail()}catch(_:ValidationException){}
  assertEquals(0,e.calls);assertEquals(10,q.status().remaining)
 }
}
