plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")

    id("kotlin-parcelize")
}

android {
    namespace = "cc.chenhe.weargallery.common"
    compileSdk = 31

    resourcePrefix = "lib_"

    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(fileTree(baseDir = "libs") { include("*.jar") })
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    val lifecycle = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-process:$lifecycle")

    val moshi = rootProject.extra["moshi"] as String
    implementation("com.squareup.moshi:moshi:$moshi")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshi")

    implementation("androidx.exifinterface:exifinterface:1.3.5")

    api("com.jakewharton.timber:timber:5.0.1")
    api("me.chenhe:wearmsger:2.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-core:0.33.0")
}
