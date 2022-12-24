plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
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

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")
    implementation("io.insert-koin:koin-android:3.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("com.google.android.material:material:1.4.0")
    implementation("id.zelory:compressor:3.0.1")
    implementation("com.heinrichreimersoftware:material-intro:2.0.0")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")

    val nav = rootProject.ext["nav"] as String
    implementation("androidx.navigation:navigation-fragment-ktx:$nav")
    implementation("androidx.navigation:navigation-ui-ktx:$nav")

    val lifecycle = "2.4.0"
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")

    val moshi = rootProject.extra["moshi"] as String
    implementation("com.squareup.moshi:moshi:$moshi")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi")

    val appcenter = "4.4.2"
    implementation("com.microsoft.appcenter:appcenter-analytics:$appcenter")
    implementation("com.microsoft.appcenter:appcenter-crashes:$appcenter")

    // pictures
    val glide = "4.12.0"
    implementation("com.github.bumptech.glide:glide:$glide")
    kapt("com.github.bumptech.glide:compiler:$glide")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.strikt:strikt-core:0.33.0")
}
