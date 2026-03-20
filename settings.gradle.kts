pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
val localProperties = java.util.Properties().apply {
    val file = file("local.properties")
    if (file.exists()) load(file.inputStream())
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials.username = "mapbox"
            credentials.password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""
            authentication { create<BasicAuthentication>("basic") }
        }
    }
}

rootProject.name = "scratch-map-app"
include(":app")
