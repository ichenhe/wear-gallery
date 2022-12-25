plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("org.jetbrains.kotlin.android") version Versions.kotlin apply false
    id("com.google.devtools.ksp").version("${Versions.kotlin}-1.0.8") apply false

    id("com.github.ben-manes.versions") version ("0.39.0")
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
