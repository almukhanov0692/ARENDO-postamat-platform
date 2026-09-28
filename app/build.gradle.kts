plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.postamatmodbus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.postamatmodbus"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "0.2.1"
    }
}

repositories {
    google()
    mavenCentral()
    flatDir {
        dirs("libs")
    }
}

dependencies {
    implementation(files("libs/usb-serial-for-android-3.10.0.aar"))
    implementation(files("libs/Java-WebSocket-1.5.7.jar"))
    implementation("org.slf4j:slf4j-api:1.7.36")
}
