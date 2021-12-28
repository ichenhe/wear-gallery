plugins {
    id("com.github.ben-manes.versions") version ("0.39.0")
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

@Suppress("JcenterRepositoryObsolete") allprojects {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/ichenhe/Actions-Mars")
            // about credentials see https://github.community/t/download-from-github-package-registry-without-authentication/14407/111
            credentials {
                username = "chenhe-pub"
                password = "\u0067hp_iEietheghA8ocZN0vCEvb6qCCx0xsU4YMFBf"
            }
        }

        maven("https://jitpack.io")
        mavenCentral()
        jcenter()
        google()
    }

    tasks.withType(Test::class.java) {
        useJUnitPlatform()
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
