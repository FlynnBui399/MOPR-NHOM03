import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    load(secretsFile.inputStream())
}

fun secretString(name: String): String {
    val value = secrets[name]?.toString().orEmpty().trim().trim('"')
    return "\"$value\""
}

fun secretString(name: String, defaultValue: String): String {
    val value = secrets[name]?.toString().orEmpty().trim().trim('"')
    return "\"${value.ifBlank { defaultValue }}\""
}

android {
    namespace = "com.example.pocket"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.pocket"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Inject secrets into BuildConfig
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", secretString("CLOUDINARY_CLOUD_NAME"))
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", secretString("CLOUDINARY_UPLOAD_PRESET"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", secretString("GOOGLE_WEB_CLIENT_ID"))
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.camera:camera-core:1.5.0")
    implementation(libs.touchimageview)
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-video:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    implementation(platform("com.google.firebase:firebase-bom:34.14.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-functions") {
        exclude(group = "com.google.firebase", module = "firebase-appcheck-interop")
    }
    implementation("com.google.firebase:firebase-messaging")

    // Room local database cache
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
}
