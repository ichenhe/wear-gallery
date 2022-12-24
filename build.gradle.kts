plugins {
    id("com.android.application") version "7.0.4" apply false
    id("com.android.library") version "7.0.4" apply false
    id("org.jetbrains.kotlin.android") version "1.7.0" apply false
    id("com.google.devtools.ksp").version("1.7.0-1.0.6") apply false // sync with kotlin version

    id("com.github.ben-manes.versions") version ("0.39.0")
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    extra.apply {
        set("moshi", "1.13.0")
        set("nav", "2.3.5")
    }
    dependencies {
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

subprojects {
    // copy lint and test reports to /build/reports
    val projName = name
    tasks.register<Copy>("copyReports") {
        val reportDir = File(project.buildDir, "reports")
        onlyIf {
            reportDir.isDirectory && !reportDir.list().isNullOrEmpty()
        }
        from(reportDir)
        into(File(rootProject.buildDir, "reports" + File.separator + projName))
    }
    afterEvaluate {
        (extensions.findByType(com.android.build.gradle.LibraryExtension::class)
            ?: extensions.findByType(com.android.build.gradle.AppExtension::class))?.apply {
            afterEvaluate {
                tasks.findByName("lint")?.finalizedBy("copyReports")
                tasks.findByName("test")?.finalizedBy("copyReports")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
