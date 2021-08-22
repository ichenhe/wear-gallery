plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 23
        targetSdk = 30
        versionCode = 220601020 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.1.2-preview"

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
}

dependencies {
    wearApp(project(":wear"))
    implementation(fileTree("libs") { include("*.jar") })
    implementation(project(":common"))

    implementation("androidx.appcompat:appcompat:${Ver.appcompat}")
    implementation("androidx.recyclerview:recyclerview:${Ver.recyclerview}")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:${Ver.nav}")
    implementation("androidx.navigation:navigation-ui-ktx:${Ver.nav}")
    implementation("androidx.constraintlayout:constraintlayout:${Ver.constraintlayout}")
    implementation("androidx.viewpager2:viewpager2:${Ver.viewpager2}")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-common-java8:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-service:${Ver.lifecycle}")
    implementation("androidx.work:work-runtime-ktx:2.5.0")
    implementation("androidx.fragment:fragment-ktx:${Ver.fragment}")
    implementation("androidx.preference:preference-ktx:${Ver.preference}")
    implementation("androidx.core:core-ktx:${Ver.ktx}")
    implementation("androidx.exifinterface:exifinterface:${Ver.exifinterface}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Ver.coroutinesAndroid}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.5.1")

    implementation("io.insert-koin:koin-android:${Ver.koin}")
    implementation("com.squareup.moshi:moshi:${Ver.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Ver.moshi}")
    implementation("com.google.android.material:material:${Ver.material}")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.heinrichreimersoftware:material-intro:${Ver.materialIntro}")
    implementation("com.microsoft.appcenter:appcenter-analytics:${Ver.appCenter}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${Ver.appCenter}")

    // pictures
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:${Ver.subsamplingImageView}")

    testImplementation("org.junit.jupiter:junit-jupiter:${Ver.junit}")
    testImplementation("io.strikt:strikt-core:${Ver.strikt}")
}
