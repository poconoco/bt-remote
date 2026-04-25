plugins {
    id("com.android.application")
}

android {
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nocomake.serialremote"
        minSdk = 27
        targetSdk = 36
        versionCode = 6
        versionName = "1.6"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("com.google.guava:guava:33.6.0-android")
    implementation("io.reactivex.rxjava3:rxjava:3.1.12")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.media3:media3-exoplayer:1.10.0")
    implementation("androidx.media3:media3-ui:1.10.0")
    implementation("androidx.media3:media3-common:1.10.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.0")
    implementation("com.github.niqdev:ipcam-view:3.0.0")
}
