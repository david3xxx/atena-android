plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
  namespace = "pe.atena.study"
  compileSdk = 35
  defaultConfig {
    applicationId = "pe.atena.study.demo"
    minSdk = 29
    targetSdk = 35
    versionCode = 4
    versionName = "0.4-demo"
  }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
}

dependencies { testImplementation("junit:junit:4.13.2") }
