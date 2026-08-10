plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")


}

android {
    namespace = "com.utng.compasos_movil"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.utng.compasos_movil"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Dependencias para la Navegacion
    implementation("androidx.navigation:navigation-compose:2.9.0")

    // Dependencias ROOM
    val room_version = "3.0.0"
    implementation("androidx.room3:room3-runtime:$room_version")
    ksp("androidx.room3:room3-compiler:$room_version")

    // material 3 iconos
    implementation("androidx.compose.material:material-icons-extended:1.7.6")

// OSMDroid - OpenStreetMap
    implementation("org.osmdroid:osmdroid-android:6.1.17")
// Retrofit para llamadas a API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
// Gson
    implementation("com.google.code.gson:gson:2.10.1")
// Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
// Permisos (Opcional - Simplifica la gestión de permisos)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
// Hilt (Inyección de dependencias - Opcional)
    implementation("com.google.dagger:hilt-android:2.48")

    implementation("com.mapbox.extension:maps-compose:11.28.0")
    implementation("com.mapbox.maps:android:11.28.0")


    implementation("androidx.security:security-crypto:1.1.0-alpha06")




}
