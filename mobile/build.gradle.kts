plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdk = 31
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 23
        targetSdk = 31
        versionCode = 220601070 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.1.7"

        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        Signing(rootDir).readConfig()?.also { config ->
            create("release") {
                storeFile = config.storeFile
                storePassword = config.storePassword
                keyAlias = config.keyAlias
                keyPassword = config.keyPassword
            }
        }
    }
    buildTypes {
        release {
            signingConfigs.findByName("release")?.also { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions.add("channel")
    productFlavors {
        create("normal") {
            dimension = "channel"
            buildConfigField("Boolean", "IS_GP", "false")
        }
        create("gp") {
            dimension = "channel"
            buildConfigField("Boolean", "IS_GP", "true")
        }
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    afterEvaluate {
        tasks.register<Copy>("copyApk") {
            val apkOutDir = android.applicationVariants.first {
                !it.buildType.isDebuggable
            }.outputs.first().outputFile.parentFile
            from(apkOutDir)
            into(File(rootProject.buildDir, "outs"))
            include("*.apk")
        }
        tasks.getByName("assembleRelease").finalizedBy("copyApk")
        tasks.getByName("copyApk").dependsOn("assembleRelease")
    }
}

dependencies {
    wearApp(project(":wear"))
    implementation(fileTree("libs") { include("*.jar") })
    implementation(project(":common"))

    implementation(androidx.bundles.base)
    implementation(androidx.bundles.nav)
    implementation(androidx.cardview)
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.work.runtime.ktx)
    implementation(androidx.preference.ktx)
    implementation(androidx.exifinterface)
    implementation(kotlinx.bundles.coroutine)
    implementation(kotlinx.coroutines.playservices)

    implementation(lib.koin.android)
    implementation(lib.moshi)
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${lib.versions.moshi.get()}")
    implementation(lib.md)
    implementation(lib.compressor)
    implementation(lib.intro)
    implementation(lib.play.services.wearable)
    implementation(lib.bundles.appcenter)

    // pictures
    implementation(lib.glide)
    kapt("com.github.bumptech.glide:compiler:${lib.versions.glide.get()}")
    implementation(lib.subsampling)

    testImplementation(lib.bundles.test)
}
