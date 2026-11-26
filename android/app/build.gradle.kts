plugins {
    alias(libs.plugins.android.application)
}

// Reseller branding lives in one file shared with the iPhone project: ../../whitelabel/build.xcconfig
val whiteLabel: Map<String, String> = rootProject.file("../whitelabel/build.xcconfig").readLines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("//") && it.contains("=") }
    .associate { it.substringBefore("=").trim() to it.substringAfter("=").trim() }

fun label(key: String): String = whiteLabel[key] ?: error("whitelabel/build.xcconfig is missing $key")

android {
    namespace = "com.r4xn.whitelabelpos"
    compileSdk = 36

    defaultConfig {
        applicationId = label("APP_ID")
        minSdk = 26          // Android 8.0
        targetSdk = 36
        versionCode = label("APP_BUILD").toInt()
        versionName = label("APP_VERSION")
        manifestPlaceholders["appName"] = label("APP_NAME")
    }

    sourceSets {
        getByName("main") {
            // designs.json, catalogs.json and currencies.json are shared with the iPhone app.
            assets.directories.add("../../whitelabel/app")
        }
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    // Real org.json for JVM unit tests (Android's copy is stubbed there).
    testImplementation(libs.org.json)
}
