plugins {
    id("com.android.application")
}

android {
    namespace = "pl.szynolandia.szybkaklawiatura"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.szynolandia.szybkaklawiatura"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
