import java.util.Properties
plugins {
 id("com.android.application")
 kotlin("android")
 kotlin("plugin.serialization")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
 id("app.cash.paparazzi")
}
val firebaseConfig = Properties().apply { rootProject.file("firebase.local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
fun firebaseValue(name: String) = "\"" + (firebaseConfig.getProperty(name) ?: "").replace("\\", "\\\\").replace("\"", "\\\"") + "\""
val releaseSigning = Properties().apply { rootProject.file("signing.local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) } }
android {
 namespace = "dev.jamescullimore.starjar.routinebuilder"
 compileSdk = 36
 defaultConfig {
  applicationId = "dev.jamescullimore.starjar.routinebuilder"
  minSdk = 26; targetSdk = 36; versionCode = 3; versionName = "0.3-hackathon"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  buildConfigField("String", "FIREBASE_APP_ID", firebaseValue("appId"))
  buildConfigField("String", "FIREBASE_PROJECT_ID", firebaseValue("projectId"))
  buildConfigField("String", "FIREBASE_API_KEY", firebaseValue("apiKey"))
  buildConfigField("String", "RECAPTCHA_SITE_KEY", firebaseValue("recaptchaSiteKey"))
  buildConfigField("boolean", "USE_RECAPTCHA", providers.gradleProperty("recaptchaAppCheck").map { (it == "true").toString() }.getOrElse("false"))
  buildConfigField("String", "CLOUD_MODEL", "\"gemini-3.5-flash-lite\"")
 }
 testBuildType = providers.gradleProperty("testBuildType").getOrElse("debug")
 if (releaseSigning.isNotEmpty()) {
  signingConfigs.create("companionRelease") {
   storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
   storePassword = releaseSigning.getProperty("storePassword")
   keyAlias = releaseSigning.getProperty("keyAlias")
   keyPassword = releaseSigning.getProperty("keyPassword")
  }
  buildTypes.getByName("release").signingConfig = signingConfigs.getByName("companionRelease")
 }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 testOptions { unitTests.isIncludeAndroidResources = true }
 packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }
dependencies {
 implementation(project(":core"))
 implementation("androidx.activity:activity-compose:1.12.3")
 implementation("androidx.compose.ui:ui:1.10.2")
 implementation("androidx.compose.ui:ui-tooling-preview:1.10.2")
 implementation("androidx.compose.material3:material3:1.4.0")
 implementation("androidx.compose.material:material-icons-extended:1.7.3")
 implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
 implementation("com.google.firebase:firebase-ai:18.2.0")
 implementation("com.google.firebase:firebase-ai-ondevice:16.0.0-beta08")
 ksp("com.google.firebase:firebase-ai-ksp-processor:17.0.0")
 debugImplementation("com.google.firebase:firebase-appcheck-debug:20.1.0")
 implementation("com.google.firebase:firebase-appcheck-recaptcha:20.0.0")
 debugImplementation("androidx.compose.ui:ui-tooling:1.10.2")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
 testImplementation("io.github.sergio-sastre.ComposablePreviewScanner:android:0.8.1")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.10.2")
 debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.2")
}
tasks.withType<Test>().configureEach { maxHeapSize = "2g"; javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }) }
