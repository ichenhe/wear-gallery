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
        minSdk = 23
        targetSdk = 33
        versionCode = 220603010 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.3.1"

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

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeKotlinCompilerExtension
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

    // compose
    implementation(platform("androidx.compose:compose-bom:2022.12.00"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2022.12.00"))
    // sync to compose version: https://github.com/google/accompanist#compose-versions
    val accompanist = "0.28.0"
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist")
    implementation("com.google.accompanist:accompanist-placeholder-material:$accompanist")
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("androidx.compose.material3:material3-window-size-class")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")
    implementation("io.insert-koin:koin-android:3.2.0")
    implementation("io.insert-koin:koin-androidx-compose:3.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("com.google.android.material:material:1.7.0")
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.google.android.gms:play-services-wearable:18.0.0")

    val nav = rootProject.ext["nav"] as String
    implementation("androidx.navigation:navigation-fragment-ktx:$nav")
    implementation("androidx.navigation:navigation-compose:$nav")
    implementation("androidx.navigation:navigation-ui-ktx:$nav")

    val lifecycle = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")

    val moshi = rootProject.extra["moshi"] as String
    implementation("com.squareup.moshi:moshi:$moshi")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:$moshi")

    val appcenter = "5.0.0"
    implementation("com.microsoft.appcenter:appcenter-analytics:$appcenter")
    implementation("com.microsoft.appcenter:appcenter-crashes:$appcenter")

    // pictures
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    val coil = "2.2.2"
    implementation("io.coil-kt:coil:$coil")
    implementation("io.coil-kt:coil-gif:$coil")
    implementation("io.coil-kt:coil-compose:$coil")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-core:0.33.0")
}
