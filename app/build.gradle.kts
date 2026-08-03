import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Aplica el plugin de Google Services solo cuando exista el google-services.json.
// Descárgalo desde Firebase Console (Project settings > Your apps) y colócalo en app/google-services.json.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Usar versionCode fijo - ignorar local.properties para evitar conflictos
val appVersionCode = 46
val appVersionName = providers.gradleProperty("APP_VERSION_NAME").orElse("1.0.0").get()
val appExpirationDays = providers.gradleProperty("APP_EXPIRATION_DAYS").orElse("3650").get().toLong()

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.carlos.controlmedicamentos"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.carlos.controlmedicamentos"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BUILD_TIMESTAMP para caducidad configurable (por defecto 10 años = 3650 días)
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        buildConfigField("long", "APP_EXPIRATION_DAYS", "${appExpirationDays}L")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProps.getProperty("KEYSTORE_PATH", "controlmedicamentos.jks"))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = localProps.getProperty("KEY_ALIAS", "controlmed")
            keyPassword = localProps.getProperty("KEY_PASSWORD", "")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(project(":sync-core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // OSMDroid (mapa OpenStreetMap, sin API key)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    // FusedLocationProvider para GPS bicicleta
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Room
    implementation("androidx.room:room-runtime:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")

    // Gson para serialización
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp para validación de licencias Lemon Squeezy
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coil para cargar imágenes
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Almacenamiento cifrado
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Firebase BOM y Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}