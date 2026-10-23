pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hermes-watch-android"

include(":android-shared")
include(":wear-app")
include(":android-companion")

project(":android-shared").projectDir = file("../android-shared")
project(":wear-app").projectDir = file("../wear-app")
project(":android-companion").projectDir = file("../android-companion")
