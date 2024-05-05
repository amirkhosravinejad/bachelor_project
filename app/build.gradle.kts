plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
}

android {
    namespace = "com.api"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.api"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // lottie animations
    implementation("com.airbnb.android:lottie:6.4.0")
    // 2 libraries in order to have web connection
    implementation("org.json:json:20211205")
    implementation("com.android.volley:volley:1.2.1")
    //implementation(files("C:\\Users\\max\\Downloads\\jmdns\\jmdns.jar"))
    implementation("javax.jmdns:jmdns:3.2.2")
//    implementation("org.dhcpcd:dhcpcd-ui:1.0.5")

    // library in java for supporting zeroconf protocol
//    implementation(libs.jmdns)
//    implementation("com.github.xiaogegexiao:rxbonjour:1.0.8")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}