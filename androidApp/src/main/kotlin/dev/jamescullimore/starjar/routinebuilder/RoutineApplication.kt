package dev.jamescullimore.starjar.routinebuilder
import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
class RoutineApplication : Application() {
 override fun onCreate() {
  super.onCreate()
  if (BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() && BuildConfig.FIREBASE_APP_ID.isNotBlank() && BuildConfig.FIREBASE_API_KEY.isNotBlank()) {
   FirebaseApp.initializeApp(this, FirebaseOptions.Builder().setProjectId(BuildConfig.FIREBASE_PROJECT_ID).setApplicationId(BuildConfig.FIREBASE_APP_ID).setApiKey(BuildConfig.FIREBASE_API_KEY).build())
   installAppCheck()
  }
 }
}
