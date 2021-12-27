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
    implementation(kotlinx.bundles.coroutine)
    implementation(androidx.bundles.base)
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.room.ktx)
    kapt("androidx.room:room-compiler:${androidx.versions.room.get()}")
    implementation(androidx.exifinterface)

    implementation(lib.moshi)
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${lib.versions.moshi.get()}")

    api(lib.timber)
    api(lib.wearmsger)

    testImplementation(lib.bundles.test)
}


