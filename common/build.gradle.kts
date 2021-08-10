plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

android {
    compileSdk = 30

    resourcePrefix = "lib_"

    defaultConfig {
        minSdk = 21
        targetSdk = 30
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Ver.coroutinesAndroid}")
    implementation("androidx.annotation:annotation:${Ver.annotation}")
    implementation("androidx.core:core-ktx:${Ver.ktx}")
    implementation("androidx.constraintlayout:constraintlayout:${Ver.constraintlayout}")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-process:${Ver.lifecycle}")
    implementation("androidx.recyclerview:recyclerview:${Ver.recyclerview}")
    implementation("androidx.room:room-ktx:${Ver.room}")
    kapt("androidx.room:room-compiler:${Ver.room}")

    implementation("com.squareup.moshi:moshi:${Ver.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Ver.moshi}")
    compileOnly("me.chenhe:wearmsger:${Ver.wearMsger}")
    api("com.jakewharton.timber:timber:${Ver.timber}")
    api("me.chenhe:mars-xlog:0.1.0")
    api("me.chenhe:wearmsger:${Ver.wearMsger}")


    testImplementation("junit:junit:${Ver.junit}")
    testImplementation("androidx.test:core:${Ver.test}") // Robolectric environment
    testImplementation("org.mockito:mockito-core:${Ver.mockito}") // Mockito framework
    testImplementation("org.amshove.kluent:kluent-android:${Ver.kluent}")
}
