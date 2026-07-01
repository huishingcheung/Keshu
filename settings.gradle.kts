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

rootProject.name = "JNU Smart Edu"
include(":app")

gradle.beforeProject {
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir(".build/${project.path.removePrefix(":").replace(':', '/')}"))
}
