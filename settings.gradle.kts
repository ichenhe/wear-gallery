enableFeaturePreview("VERSION_CATALOGS")

include(":common", ":mobile", ":wear")
rootProject.name = "WearGallery"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    versionCatalogs {
        create("kotlinx") {
            version("coroutine", "1.5.1")
            alias("coroutines-android").to(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-android"
            ).versionRef("coroutine")
            bundle("coroutine", listOf("coroutines-android"))

            alias("coroutines-playservices").to(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-play-services"
            ).versionRef("coroutine")
        }

        create("androidx") {
            alias("annotation").to("androidx.annotation:annotation:1.2.0")
            alias("core-ktx").to("androidx.core:core-ktx:1.6.0")
            alias("constraintlayout").to("androidx.constraintlayout:constraintlayout:2.0.4")
            alias("recyclerview").to("androidx.recyclerview:recyclerview:1.2.1")
            alias("appcompat").to("androidx.appcompat:appcompat:1.3.1")
            alias("viewpager2").to("androidx.viewpager2:viewpager2:1.0.0")
            alias("fragment-ktx").to("androidx.fragment:fragment-ktx:1.3.6")
            bundle(
                "base",
                listOf(
                    "annotation",
                    "core-ktx",
                    "constraintlayout",
                    "recyclerview",
                    "appcompat",
                    "viewpager2",
                    "fragment-ktx",
                )
            )

            version("lifecycle", "2.3.1")
            alias("lifecycle-runtime-ktx").to("androidx.lifecycle", "lifecycle-runtime-ktx")
                .versionRef("lifecycle")
            alias("lifecycle-livedata-ktx").to("androidx.lifecycle", "lifecycle-livedata-ktx")
                .versionRef("lifecycle")
            alias("lifecycle-process").to("androidx.lifecycle", "lifecycle-process")
                .versionRef("lifecycle")
            alias("lifecycle-service").to("androidx.lifecycle", "lifecycle-service")
                .versionRef("lifecycle")
            alias("lifecycle-common-java8").to("androidx.lifecycle", "lifecycle-common-java8")
                .versionRef("lifecycle")
            bundle(
                "lifecycle",
                listOf(
                    "lifecycle-runtime-ktx",
                    "lifecycle-livedata-ktx",
                    "lifecycle-process",
                    "lifecycle-service",
                    "lifecycle-common-java8",
                )
            )

            version("nav", "2.3.5") // 注意同步更改根项目下的差距版本号
            alias("navigation-fragment-ktx").to("androidx.navigation", "navigation-fragment-ktx")
                .versionRef("nav")
            alias("navigation-ui-ktx").to("androidx.navigation", "navigation-ui-ktx")
                .versionRef("nav")
            bundle("nav", listOf("navigation-fragment-ktx", "navigation-ui-ktx"))

            version("room", "2.3.0")
            alias("room-ktx").to("androidx.room", "room-ktx").versionRef("room")
            alias("exifinterface").to("androidx.exifinterface:exifinterface:1.3.2")
            alias("cardview").to("androidx.cardview:cardview:1.0.0")
            alias("work-runtime-ktx").to("androidx.work:work-runtime-ktx:2.5.0")
            alias("preference-ktx").to("androidx.preference:preference-ktx:1.1.1")
            alias("percentlayout").to("androidx.percentlayout:percentlayout:1.0.0")
            alias("palette").to("androidx.palette:palette:1.0.0")
            alias("paging-runtime-ktx").to("androidx.paging:paging-runtime-ktx:3.0.1")

        }

        create("lib") {
            version("moshi", "1.12.0")
            alias("moshi").to("com.squareup.moshi", "moshi").versionRef("moshi")
            version("glide", "4.12.0")
            alias("glide").to("com.github.bumptech.glide", "glide").versionRef("glide")
            version("koin", "3.1.2")
            alias("koin-android").to("io.insert-koin", "koin-android").versionRef("koin")
            alias("koin-test").to("io.insert-koin", "koin-test").versionRef("koin")

            alias("subsampling").to("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
            alias("timber").to("com.jakewharton.timber:timber:5.0.0")
            alias("wearmsger").to("me.chenhe:wearmsger:2.0.2")
            alias("md").to("com.google.android.material:material:1.4.0")
            alias("compressor").to("id.zelory:compressor:3.0.1")
            alias("intro").to("com.heinrichreimersoftware:material-intro:2.0.0")
            alias("play-services-wearable").to("com.google.android.gms:play-services-wearable:17.1.0")
            alias("viewpagerindicator").to("com.github.zhpanvip:viewpagerindicator:1.2.1")

            version("appcenter", "4.1.0")
            alias("appcenter-analytics").to("com.microsoft.appcenter", "appcenter-analytics")
                .versionRef("appcenter")
            alias("appcenter-crashes").to("com.microsoft.appcenter", "appcenter-crashes")
                .versionRef("appcenter")
            bundle("appcenter", listOf("appcenter-analytics", "appcenter-crashes"))

            version("coil", "1.3.1")
            alias("coil-core").to("io.coil-kt", "coil").versionRef("coil")
            alias("coil-gif").to("io.coil-kt", "coil-gif").versionRef("coil")
            bundle("coil", listOf("coil-core", "coil-gif"))

            // --------
            // test
            // --------
            alias("junit-jupiter").to("org.junit.jupiter:junit-jupiter:5.7.2")
            alias("strikt-core").to("io.strikt:strikt-core:0.31.0")
            bundle("test", listOf("junit-jupiter", "strikt-core"))
        }
    }
}