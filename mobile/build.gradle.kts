import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("cc.chenhe.weargallery.copy-outs")
}

android {
    namespace = "cc.chenhe.weargallery"
    compileSdk = 34
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 23
        targetSdk = 33
        versionCode = 220603020 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.3.2"

        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            type = "String",
            name = "APPCENTER_SECRET",
            value = "\"${gradleLocalProperties(rootProject.projectDir)["mobile.appcenter.secret"] ?: ""}\""
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
        buildConfig = true
        viewBinding = true
        compose = true
    }
    lint {
        textReport = false
        xmlReport = false
        disable += "MissingTranslation"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeKotlinCompilerExtension
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
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
    val accompanist = "0.32.0"
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
    implementation("androidx.datastore:datastore-preferences:1.0.0")

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
