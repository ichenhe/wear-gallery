pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        // has some dependencies in legacy jcenter, will remove them as soon as possible.
        // jcenter has 301 to maven central which doesn't include required packages,
        // use a mirror as temporary workaround.
        maven("https://maven.aliyun.com/repository/jcenter")
    }
}

include(":common", ":mobile", ":wear")
rootProject.name = "WearGallery"