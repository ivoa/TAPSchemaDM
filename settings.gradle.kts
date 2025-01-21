
rootProject.name="tapschema"
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        /*
        add this repository to pick up the SNAPSHOT version of the IVOA base library - in the future when this
        will not be necessary when this library is released as a non-SNAPSHOT version.
         */
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

