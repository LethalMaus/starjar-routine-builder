package dev.jamescullimore.starjar.routinebuilder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jamescullimore.starjar.routinebuilder.ui.RoutineBuilderTheme
import dev.jamescullimore.starjar.routinebuilder.ui.BrandButton
import dev.jamescullimore.starjar.routinebuilder.ui.BrandTextButton
import dev.jamescullimore.starjar.routinebuilder.ui.StarryMark
@Composable
fun AdultEntry(german:Boolean,onContinue:()->Unit,onClose:()->Unit) {
 fun text(en:String,de:String)=if(german)de else en
 RoutineBuilderTheme { Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
  Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
   StarryMark(Modifier.height(96.dp).align(androidx.compose.ui.Alignment.CenterHorizontally))
   Text("StarJar Routine Builder",style=MaterialTheme.typography.headlineLarge)
   Text(text("A planning tool for adults","Ein Planungstool für Erwachsene"),style=MaterialTheme.typography.headlineSmall)
   Text(text("This separate app is for parents and caregivers aged 18 or over. Children should use the routines you review, not this AI tool.","Diese eigenständige App richtet sich an Eltern und Bezugspersonen ab 18 Jahren. Kinder nutzen die von dir geprüften Routinen, nicht dieses KI-Tool."))
   Text(text("Please leave names, schools and other personal details out of requests. Cloud suggestions stay off unless you explicitly allow them. You can also create routines without AI.","Bitte gib keine Namen, Schulen oder anderen persönlichen Angaben ein. Cloud-Vorschläge bleiben ausgeschaltet, bis du sie ausdrücklich erlaubst. Du kannst Routinen auch ohne KI erstellen."))
   BrandButton(onClick=onContinue,modifier=Modifier.fillMaxWidth().testTag("adultContinue")) { Text(text("I’m 18 or over · Continue","Ich bin mindestens 18 · Weiter")) }
   BrandTextButton(onClick=onClose,modifier=Modifier.fillMaxWidth()) { Text(text("Close","Schließen")) }
  }
 } }
}
@Preview @Composable fun AdultEntryEnPreview()=AdultEntry(false,{},{})
@Preview @Composable fun AdultEntryDePreview()=AdultEntry(true,{},{})
