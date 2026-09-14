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
        versionCode = 7
        versionName = "0.7.0"
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

dependencies {
    implementation("org.carrot2:morfologik-polish:2.1.9")
}
