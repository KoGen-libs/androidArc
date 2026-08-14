plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kogen.androidarc.demo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kogen.androidarc.demo"
        minSdk = 26
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("debug") {
            java.directories.add("build/generated/ksp/debug/kotlin")
        }
        getByName("release") {
            java.directories.add("build/generated/ksp/release/kotlin")
        }
    }
}

dependencies {
    implementation(project(":androidArc"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines)

    implementation(libs.kogen.di)
    ksp(libs.kogen.di.compiler)
    implementation(libs.androidx.navigation)
    implementation(libs.koGenNavigation)
    ksp(libs.koGenNavigationCompiler)

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)
    debugImplementation(libs.compose.ui.tooling)
}

ksp {
    arg("packageName", "com.kogen.androidarc.demo")
    arg("includeViewModelInjector", "true")
    arg("defaultAnimation", "slideLeft")
    arg("screenSuffix", "container")
}
