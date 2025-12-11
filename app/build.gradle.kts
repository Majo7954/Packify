plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ucb.deliveryapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ucb.deliveryapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Habilitar pruebas en modo debug
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    // Configuración para testing
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
        animationsDisabled = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // AGREGAR ESTAS EXCLUSIONES PARA RESOLVER EL ERROR
            excludes += listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/io.netty.versions.properties",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module",
                "META-INF/proguard/*",
                "META-INF/versions/*",
                "META-INF/services/*",
                "**/*.kotlin_builtins",
                "**/*.kotlin_metadata"
            )
        }
    }
}

dependencies {
    // ========== FIREBASE ==========
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-config-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    androidTestImplementation("com.google.firebase:firebase-firestore:24.10.0")
    androidTestImplementation("com.google.firebase:firebase-auth:22.3.0")

    // ========== JETPACK COMPOSE ==========
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ========== ROOM DATABASE ==========
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1") // Para pruebas

    // ========== LIFECYCLE / VIEWMODEL ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    // ========== COROUTINES ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // ========== NAVEGACIÓN Y MAPAS ==========
    implementation("androidx.navigation:navigation-compose:2.8.1")
    implementation("com.mapbox.maps:android:10.14.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation(libs.play.services.maps)
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // ========== UI CLÁSICA ==========
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.google.android.material:material:1.10.0")

    // ========== VIEWMODEL EN COMPOSE ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")

    // ========== DATASTORE ==========
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")

    // ========== ICONOS EXTENDIDOS ==========
    implementation("androidx.compose.material:material-icons-extended:1.5.4")

    // ========== DEPENDENCIAS DE INYECCIÓN (OPCIONAL) ==========
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ========== PRUEBAS UNITARIAS (test/) ==========
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test:rules:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("junit:junit:4.13.2")

    // Para pruebas con ViewModel
    testImplementation("androidx.lifecycle:lifecycle-viewmodel-testing:2.8.4")

    // ========== PRUEBAS DE INTEGRACIÓN/UI (androidTest/) ==========
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation("io.mockk:mockk-android:1.13.9")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.5")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // Para capturar screenshots en pruebas UI
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    // ========== DEPENDENCIAS PARA TESTS DE INTEGRACIÓN ==========
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")
    androidTestImplementation("com.squareup.okhttp3:okhttp-tls:4.11.0")

    // ========== DEBUG ==========
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}