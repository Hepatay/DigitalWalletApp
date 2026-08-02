import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.androidx.room)

    // Room kod üretimi
    id("kotlin-kapt")
}

val keystorePropertiesFile =
    rootProject.file("keystore.properties")

val keystoreProperties =
    Properties()

val localProperties =
    Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use(::load)
        }
    }

val apiNoktamApiKey =
    localProperties.getProperty("APINOKTAM_API_KEY", "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

if (keystorePropertiesFile.exists()) {

    keystorePropertiesFile
        .inputStream()
        .use(
            keystoreProperties::load
        )
}

android {

    namespace =
        "com.epatay.digitalwallet"

    compileSdk =
        36

    defaultConfig {

        applicationId =
            "com.epatay.digitalwallet"

        minSdk =
            24

        targetSdk =
            36

        versionCode =
            2

        versionName =
            "1.1.0"

        buildConfigField(
            "String",
            "APINOKTAM_API_KEY",
            "\"$apiNoktamApiKey\""
        )

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {

        if (keystorePropertiesFile.exists()) {

            create("release") {

                storeFile =
                    file(
                        requireNotNull(
                            keystoreProperties.getProperty(
                                "storeFile"
                            )
                        )
                    )

                storePassword =
                    requireNotNull(
                        keystoreProperties.getProperty(
                            "storePassword"
                        )
                    )

                keyAlias =
                    requireNotNull(
                        keystoreProperties.getProperty(
                            "keyAlias"
                        )
                    )

                keyPassword =
                    requireNotNull(
                        keystoreProperties.getProperty(
                            "keyPassword"
                        )
                    )
            }
        }
    }

    buildTypes {

        release {

            signingConfig =
                signingConfigs.findByName(
                    "release"
                )

            isDebuggable =
                false

            isMinifyEnabled =
                true

            isShrinkResources =
                true

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {

        jvmTarget =
            "17"
    }

    buildFeatures {

        viewBinding =
            true

        buildConfig =
            true
    }
}

room {

    schemaDirectory(
        "$projectDir/schemas"
    )
}

dependencies {

    // Retrofit ve Gson
    implementation(
        "com.squareup.retrofit2:retrofit:3.0.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:3.0.0"
    )

    implementation(
        "com.google.code.gson:gson:2.13.2"
    )

    // Splash ve Lottie
    implementation(
        "androidx.core:core-splashscreen:1.2.0"
    )

    implementation(
        "com.airbnb.android:lottie:6.7.1"
    )

    // Google AdMob banner reklam SDK
    implementation(
        "com.google.android.gms:play-services-ads:24.9.0"
    )

    // AndroidX Core
    implementation(
        "androidx.core:core-ktx:1.12.0"
    )

    // Lifecycle
    implementation(
        "androidx.lifecycle:lifecycle-livedata-ktx:2.6.2"
    )

    implementation(
        "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"
    )

    // AdMob kullanıcı gizliliği ve onay yönetimi
    implementation(
        "com.google.android.ump:user-messaging-platform:4.0.0"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
    )

    // Coroutines
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    )

    // Grafik
    implementation(
        "com.github.PhilJay:MPAndroidChart:v3.1.0"
    )

    // Fragment
    implementation(
        "androidx.fragment:fragment-ktx:1.8.1"
    )

    // WorkManager
    implementation(
        "androidx.work:work-runtime-ktx:2.11.2"
    )

    // Temel Android kütüphaneleri
    implementation(
        libs.androidx.appcompat
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    // GerÃ§ek .xlsx workbook Ã¼retimi
    implementation(
        "org.dhatim:fastexcel:0.20.2"
    )

    implementation(
        libs.androidx.activity
    )

    implementation(
        libs.androidx.constraintlayout
    )

    // Room
    val roomVersion =
        "2.7.1"

    implementation(
        "androidx.room:room-runtime:$roomVersion"
    )

    implementation(
        "androidx.room:room-ktx:$roomVersion"
    )

    kapt(
        "androidx.room:room-compiler:$roomVersion"
    )

    // Testler
    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        "androidx.room:room-testing:$roomVersion"
    )
}

configurations.all {

    resolutionStrategy {

        force(
            "androidx.core:core-ktx:1.13.1"
        )

        force(
            "androidx.core:core:1.13.1"
        )
    }
}
