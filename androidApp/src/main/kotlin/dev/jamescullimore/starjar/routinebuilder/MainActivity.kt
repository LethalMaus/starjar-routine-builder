package dev.jamescullimore.starjar.routinebuilder

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.jamescullimore.starjar.routinebuilder.ui.RoutineBuilderScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
 private val model: BuilderViewModel by viewModels()
 private var pendingExport: String? = null
 private val saveFile=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
  val json=pendingExport;pendingExport=null
  if(uri==null || json==null) { model.exportFinished(false,true) }
  else lifecycleScope.launch {
   val success=withContext(Dispatchers.IO) { runCatching { requireNotNull(contentResolver.openOutputStream(uri,"wt")).bufferedWriter(Charsets.UTF_8).use { it.write(json) } }.isSuccess }
   model.exportFinished(success)
  }
 }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  pendingExport=savedInstanceState?.getString("export")
  setContent {
   val state=model.state.collectAsStateWithLifecycle().value
   LaunchedEffect(Unit) { model.effects.collect { effect -> when(effect) {
    is BuilderEffect.Export -> { pendingExport=effect.json;saveFile.launch("starjar-routine.json") }
    is BuilderEffect.Copy -> { (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("StarJar template",effect.json)) }
   } } }
   val preferences=remember { getSharedPreferences("adult-entry",MODE_PRIVATE) }
   var adult by remember { mutableStateOf(preferences.getBoolean("confirmed18",false)) }
   if(adult) RoutineBuilderScreen(state,model::action)
   else AdultEntry(state.uiLanguage=="de",{ preferences.edit().putBoolean("confirmed18",true).apply();adult=true },{ finish() })
  }
 }
 override fun onSaveInstanceState(outState:Bundle) { pendingExport?.let { outState.putString("export",it) };super.onSaveInstanceState(outState) }
 override fun onStop() { super.onStop();model.backgrounded() }
}
