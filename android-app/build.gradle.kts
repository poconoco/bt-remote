// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val kotlin_version = "2.3.0"

    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    subprojects {
        afterEvaluate {
            if (hasProperty("android")) {

                val androidExt = extensions.getByName("android")
                val getNamespaceMethod = androidExt.javaClass.methods.find { it.name == "getNamespace" }
                val setNamespaceMethod = androidExt.javaClass.methods.find { it.name == "setNamespace" }

                if (getNamespaceMethod != null && setNamespaceMethod != null) {
                    val currentNamespace = getNamespaceMethod.invoke(androidExt)
                    if (currentNamespace == null) {
                        setNamespaceMethod.invoke(androidExt, project.group.toString())
                    }
                }

            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
