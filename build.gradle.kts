plugins {
    id("com.android.application") version Versions.agp apply false
    id("com.android.library") version Versions.agp apply false
    id("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id("org.jetbrains.kotlin.plugin.compose") version Versions.kotlin apply false
    id("com.google.devtools.ksp") version Versions.ksp apply false
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val navVersion = "2.5.3"
    extra.apply {
        set("moshi", "1.14.0")
        set("nav", navVersion)
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
