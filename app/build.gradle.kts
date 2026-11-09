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
        versionCode = 4
        versionName = "0.3.0"
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

    testOptions {
        // JUnit 5 runs on the JUnit Platform; without this Gradle looks for JUnit 4 tests and finds none.
        unitTests.all { it.useJUnitPlatform() }
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
    implementation("androidx.datastore:datastore:1.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")

    // Unit tests run on the computer's JVM, not on a device. kotlin-test gives assertEquals and friends in Kotlin;
    // JUnit 5 (Jupiter) runs the tests, and the launcher is how Gradle starts it. kotlin-test's version is Kotlin's.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.4.21")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}
