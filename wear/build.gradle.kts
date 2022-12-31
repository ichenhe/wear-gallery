import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "cc.chenhe.weargallery"
    compileSdk = 33
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 21
        targetSdk = 30
        versionCode = 220603021 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.3.2"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }

        buildConfigField(
            type = "String",
            name = "APPCENTER_SECRET",
            value = "\"${gradleLocalProperties(rootProject.projectDir)["wear.appcenter.secret"] ?: ""}\""
        )
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
    lint {
        disable += "MissingTranslation"
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
    implementation(fileTree("libs") { include("*.jar") })
    implementation(project(":common"))

    implementation("androidx.exifinterface:exifinterface:1.3.5")
    implementation("androidx.palette:palette:1.0.0")
    implementation("io.insert-koin:koin-android:3.2.0")
    implementation("com.google.android.gms:play-services-wearable:18.0.0")
    implementation("com.google.android.support:wearable:2.9.0")
    compileOnly("com.google.android.wearable:wearable:2.9.0")
    implementation("cc.chenhe:watch-face-helper:2.0.3")
    implementation("com.heinrichreimersoftware:material-intro:2.0.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.github.zhpanvip:viewpagerindicator:1.2.1")


    val nav = rootProject.ext["nav"] as String
    implementation("androidx.navigation:navigation-fragment-ktx:$nav")
    implementation("androidx.navigation:navigation-ui-ktx:$nav")

    val lifecycle = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")

    val room = "2.4.3"
    implementation("androidx.room:room-ktx:$room")
    implementation("androidx.room:room-paging:$room")
    ksp("androidx.room:room-compiler:$room")
    implementation("androidx.paging:paging-runtime-ktx:3.1.1")

    val moshi = rootProject.extra["moshi"] as String
    implementation("com.squareup.moshi:moshi:$moshi")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi")

    val appcenter = "5.0.0"
    implementation("com.microsoft.appcenter:appcenter-analytics:$appcenter")
    implementation("com.microsoft.appcenter:appcenter-crashes:$appcenter")

    val sketch = "2.7.1"
    implementation("me.panpf:sketch:$sketch")
    implementation("me.panpf:sketch-gif:$sketch")
    implementation("me.chenhe:wearvision:0.1.1")

    val coil = "2.2.2"
    implementation("io.coil-kt:coil:$coil")
    implementation("io.coil-kt:coil-gif:$coil")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-core:0.33.0")
}
