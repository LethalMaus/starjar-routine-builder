package dev.jamescullimore.starjar.routinebuilder
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
internal fun installAppCheck() {
 val factory = if (BuildConfig.USE_RECAPTCHA) {
  require(BuildConfig.RECAPTCHA_SITE_KEY.isNotBlank()) { "reCAPTCHA site key is required" }
  RecaptchaAppCheckProviderFactory.getInstance(BuildConfig.RECAPTCHA_SITE_KEY)
 } else DebugAppCheckProviderFactory.getInstance()
 FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
}
