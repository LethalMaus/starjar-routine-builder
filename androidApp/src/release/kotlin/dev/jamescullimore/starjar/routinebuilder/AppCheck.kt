package dev.jamescullimore.starjar.routinebuilder
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
internal fun installAppCheck() {
 require(BuildConfig.RECAPTCHA_SITE_KEY.isNotBlank()) { "reCAPTCHA site key is required for distribution" }
 FirebaseAppCheck.getInstance().installAppCheckProviderFactory(RecaptchaAppCheckProviderFactory.getInstance(BuildConfig.RECAPTCHA_SITE_KEY))
}
