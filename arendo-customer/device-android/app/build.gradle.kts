plugins {
    id("com.android.application")
}

android {
    namespace = "kz.arendo.device"
    compileSdk = 35

    defaultConfig {
        applicationId = "kz.arendo.device"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    implementation("com.github.mik3y:usb-serial-for-android:3.10.0")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("org.slf4j:slf4j-api:1.7.36")
    testImplementation("junit:junit:4.13.2")
}
