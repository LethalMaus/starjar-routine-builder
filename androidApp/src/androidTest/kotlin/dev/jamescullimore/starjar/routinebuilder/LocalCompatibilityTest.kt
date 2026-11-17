package dev.jamescullimore.starjar.routinebuilder

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.ai.*
import com.google.firebase.ai.type.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@OptIn(PublicPreviewAPI::class)
@RunWith(AndroidJUnit4::class)
class LocalCompatibilityTest {
 @Test fun inspectCategoriesWithoutCloudOrModelDownloads() {
  assumeTrue(InstrumentationRegistry.getArguments().getString("localProbe") == "true")
  val context=InstrumentationRegistry.getInstrumentation().targetContext
  ActivityScenario.launch(MainActivity::class.java).use {
   val results=runBlocking {
    listOf("STABLE" to OnDeviceModelOption.STABLE,"PREVIEW" to OnDeviceModelOption.PREVIEW,"PREVIEW_FAST" to OnDeviceModelOption.PREVIEW_FAST).map { (name,option) ->
     buildJsonObject {
      put("category",name)
      try {
       val model=Firebase.ai(backend=GenerativeBackend.googleAI()).generativeModel(modelName=BuildConfig.CLOUD_MODEL,onDeviceConfig=OnDeviceConfig(mode=InferenceMode.ONLY_ON_DEVICE,modelOption=option,maxOutputTokens=256))
       val ext=requireNotNull(model.onDeviceExtension)
       val status=withTimeout(20000) { ext.checkStatus() };put("status",status.toString())
       if(status==OnDeviceModelStatus.AVAILABLE) put("model",withTimeout(10000) { ext.getModelName() })
      } catch(e:Exception) { put("errorType",e.javaClass.simpleName);put("error",e.message?.take(300)) }
     }
    }
   }
   val output=File(context.getExternalFilesDir(null),"acceptance").apply { mkdirs() }
   File(output,"local-compatibility.json").writeText(Json { prettyPrint=true }.encodeToString(buildJsonObject { put("device",android.os.Build.MODEL);put("sdk",android.os.Build.VERSION.SDK_INT);put("categories",JsonArray(results));put("cloudRequests",0);put("modelDownloads",0) }))
  }
 }
}
