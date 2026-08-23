plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.simats.skillora"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.simats.skillora"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        externalNativeBuild {
            cmake {
                arguments("-DCMAKE_BUILD_TYPE=Release")
                cppFlags("-O3 -flto")
            }
        }
        ndk {
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    
    aaptOptions {
        noCompress += "gguf"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

tasks.register("checkLocalAIModel") {
    doLast {
        if (System.getenv("CI") == "true") {
            println("CI environment detected, skipping local AI model check.")
            return@doLast
        }
        val modelDir = file("src/main/assets/models")
        val modelFile = file("src/main/assets/models/qwen3-1.7b-q4_k_m.gguf")
        if (!modelDir.exists()) {
            modelDir.mkdirs()
        }
        if (!modelFile.exists()) {
            throw GradleException(
                "CRITICAL ERROR: Local AI model not found!\n" +
                "The application requires Qwen3 1.7B GGUF to be packaged.\n" +
                "Please download 'qwen3-1.7b-q4_k_m.gguf' and place it in:\n" +
                "${modelFile.absolutePath}"
            )
        }
        val minSize = 10L * 1024L * 1024L // 10MB minimum
        if (modelFile.length() < minSize) {
            throw GradleException(
                "CRITICAL ERROR: Local AI model is too small (${modelFile.length()} bytes).\n" +
                "This is likely a placeholder or corrupted download.\n" +
                "Please download the real 'qwen3-1.7b-q4_k_m.gguf' (expected ~1GB)."
            )
        }
    }
}

tasks.named("preBuild") {
    dependsOn("checkLocalAIModel")
}
