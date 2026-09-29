plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
  .orElse("https://dm.zwtx.top/api/")
  .get()

android {
  namespace = "top.zwtx.daysmatter"
  compileSdk = 37

  defaultConfig {
    applicationId = "top.zwtx.daysmatter"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
    buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl.trimEnd('/')}\"")
  }

  buildTypes {
    debug {
      manifestPlaceholders["usesCleartextTraffic"] = "true"
    }
    release {
      isMinifyEnabled = false
      manifestPlaceholders["usesCleartextTraffic"] = "false"
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
  implementation(composeBom)
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.activity:activity-compose:1.13.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("cn.6tail:lunar:1.7.7")
  implementation("dev.chrisbanes.haze:haze:1.7.3")
  implementation("dev.chrisbanes.haze:haze-materials:1.7.3")
}
