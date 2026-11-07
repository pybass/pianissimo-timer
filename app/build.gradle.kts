plugins {
    id("com.android.application") version "9.5.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
    // Generates the JSON code for classes marked @Serializable; its version must match Kotlin's.
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.21"
}

android {
    namespace = "io.github.pybass.pianissimotimer"
    compileSdk = 37

    defaultConfig {
        minSdk = 36
        targetSdk = 37
        // Android installs an update only when this number is higher than the installed one. Raise it every release.
        versionCode = 2
        versionName = "0.1.1"
        // Fills `${appName}` in the manifest: the name under the launcher icon.
        manifestPlaceholders["appName"] = "Pianissimo Timer"
    }

    buildTypes {
        debug {
            // Its own id and name, so the debug build installs next to the released app instead of replacing it.
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "Pianissimo Dev"
        }
        release {
            // R8 drops the code and resources the app never uses: without it the APK is 25 MB, nearly all libraries.
            // It cannot see code called by name at run time, so check the release build by hand after a new library.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        warningsAsErrors = true
    }
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    // Not used directly. The BOM's lifecycle 2.9.4 ships lint checks that the current lint rejects as obsolete.
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
}
