import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("config.properties")
if (localPropertiesFile.isFile) {
    localProperties.load(FileInputStream(localPropertiesFile))
} else {
    logger.log(
        LogLevel.ERROR,
        "Can't find file <config.properties>. Please see <config-tmpl.properties>."
    )
    localProperties["ks.file"] = "n/a"
}

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "cc.chenhe.weargallery"
        minSdk = 21
        targetSdk = 30
        versionCode = 220601001 // header(22)+xx.xx.xx+device(0-phone; 1-wear)
        versionName = "v6.1.0-preview"

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
        }
    }
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("ks.file"))
            storePassword = localProperties.getProperty("ks.pwd")
            keyAlias = localProperties.getProperty("ks.alias")
            keyPassword = localProperties.getProperty("ks.aliaspwd")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
    implementation(fileTree("libs") { include("*.jar") })
    implementation(project(":common"))

    implementation("androidx.core:core-ktx:${Ver.ktx}")
    implementation("androidx.appcompat:appcompat:${Ver.appcompat}")
    implementation("androidx.fragment:fragment-ktx:${Ver.fragment}")
    implementation("androidx.constraintlayout:constraintlayout:${Ver.constraintlayout}")
    implementation("androidx.recyclerview:recyclerview:${Ver.recyclerview}")
    implementation("androidx.navigation:navigation-fragment-ktx:${Ver.nav}")
    implementation("androidx.navigation:navigation-ui-ktx:${Ver.nav}")
    implementation("androidx.viewpager2:viewpager2:${Ver.viewpager2}")
    implementation("androidx.percentlayout:percentlayout:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-common-java8:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-service:${Ver.lifecycle}")
    implementation("androidx.lifecycle:lifecycle-process:${Ver.lifecycle}")
    implementation("androidx.preference:preference-ktx:${Ver.preference}")
    implementation("androidx.annotation:annotation:${Ver.annotation}")
    implementation("androidx.palette:palette:${Ver.palette}")
    implementation("androidx.exifinterface:exifinterface:${Ver.exifinterface}")
    implementation("androidx.room:room-ktx:${Ver.room}")
    kapt("androidx.room:room-compiler:${Ver.room}")
    implementation("androidx.paging:paging-runtime-ktx:3.0.1")

    implementation("io.insert-koin:koin-android:${Ver.koin}")
    implementation("com.google.android.gms:play-services-wearable:17.1.0")
    implementation("com.google.android.support:wearable:2.8.1")
    compileOnly("com.google.android.wearable:wearable:2.8.1")
    implementation("cc.chenhe:watch-face-helper:2.0.3")
    implementation("com.heinrichreimersoftware:material-intro:${Ver.materialIntro}")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("com.squareup.moshi:moshi:${Ver.moshi}")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:${Ver.moshi}")
    implementation("com.microsoft.appcenter:appcenter-analytics:${Ver.appCenter}")
    implementation("com.microsoft.appcenter:appcenter-crashes:${Ver.appCenter}")
    val sketch = "2.7.1"
    implementation("me.panpf:sketch:$sketch")
    implementation("me.panpf:sketch-gif:$sketch")
    implementation("me.chenhe:wearvision:0.1.1")
    implementation("io.coil-kt:coil:1.3.1")
    implementation("io.coil-kt:coil-gif:1.3.1")
    implementation("com.github.zhpanvip:viewpagerindicator:1.2.1")

    testImplementation("junit:junit:${Ver.junit}")
    testImplementation("org.amshove.kluent:kluent-android:${Ver.kluent}")
    testImplementation("io.insert-koin:koin-test:${Ver.koin}")
}
