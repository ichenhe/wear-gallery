plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    compileSdk = 33
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 21
        targetSdk = 33
        versionCode = 220603011 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.3.1"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
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
    lint {
        disable("MissingTranslation")
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

    implementation(androidx.bundles.base)
    implementation(androidx.bundles.nav)
    implementation(androidx.bundles.lifecycle)
    implementation(androidx.percentlayout)
    implementation(androidx.preference.ktx)
    implementation(androidx.palette)
    implementation(androidx.exifinterface)
    implementation(androidx.room.ktx)
    implementation(androidx.room.paging)
    kapt("androidx.room:room-compiler:${androidx.versions.room.get()}")
    implementation(androidx.paging.runtime.ktx)

    implementation(lib.koin.android)
    implementation(lib.play.services.wearable)
    implementation("com.google.android.support:wearable:2.9.0")
    compileOnly("com.google.android.wearable:wearable:2.9.0")
    implementation("cc.chenhe:watch-face-helper:2.0.3")
    implementation(lib.intro)
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation(lib.moshi)
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${lib.versions.moshi.get()}")
    implementation(lib.bundles.appcenter)
    val sketch = "2.7.1"
    implementation("me.panpf:sketch:$sketch")
    implementation("me.panpf:sketch-gif:$sketch")
    implementation("me.chenhe:wearvision:0.1.1")

    implementation(lib.bundles.coil)
    implementation(lib.viewpagerindicator)

    testImplementation(lib.bundles.test)
    testImplementation(lib.koin.test)
}
