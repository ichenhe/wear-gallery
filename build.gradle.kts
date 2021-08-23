// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        // mirrors for china
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")

        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.21")
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:${Ver.nav}")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

allprojects {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/ichenhe/Actions-Mars")
            // about credentials see https://github.community/t/download-from-github-package-registry-without-authentication/14407/111
            credentials {
                username = "chenhe-pub"
                password = "\u0067hp_iEietheghA8ocZN0vCEvb6qCCx0xsU4YMFBf"
            }
        }

        // mirrors for china
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")

        maven("https://jitpack.io")
        mavenCentral()
        google()
    }

    tasks.withType(Test::class.java) {
        useJUnitPlatform()
    }
}

childProjects.forEach { (s, project) ->
    // copy lint and test reports to /build/reports
    project.tasks.register<Copy>("copyReports") {
        val reportDir = File(project.buildDir, "reports")
        onlyIf {
            reportDir.isDirectory && !reportDir.list().isNullOrEmpty()
        }
        from(reportDir)
        into(File(rootProject.buildDir, "reports" + File.separator + s))
    }
    project.afterEvaluate {
        project.tasks.findByName("lint")?.finalizedBy("copyReports")
        project.tasks.findByName("test")?.finalizedBy("copyReports")
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}
