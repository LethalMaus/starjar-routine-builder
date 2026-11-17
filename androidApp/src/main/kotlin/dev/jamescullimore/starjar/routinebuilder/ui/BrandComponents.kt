package dev.jamescullimore.starjar.routinebuilder.ui
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jamescullimore.starjar.routinebuilder.R
@Composable fun StarryMark(modifier: Modifier = Modifier) {
 Image(painterResource(R.drawable.starry), contentDescription = null, modifier = modifier)
}
@Composable fun BrandButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
 Button(onClick = onClick, modifier = modifier.heightIn(min = 64.dp), enabled = enabled,
  shape = RoundedCornerShape(32.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)) {
  ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)) { content() }
 }
}

@Composable fun BrandTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
 TextButton(onClick = onClick, modifier = modifier, enabled = enabled,
  colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer), content = content)
}
