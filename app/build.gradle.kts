plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.dagger.hilt.android)
  alias(libs.plugins.devtools.ksp)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.parcelize)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "link.danb.launcher"
  compileSdk = 36

  defaultConfig {
    applicationId = "link.danb.launcher"
    minSdk = 28
    targetSdk = 36
    versionCode = 36
    versionName = "1.36"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    vectorDrawables { useSupportLibrary = true }
  }

  ksp { arg("room.schemaLocation", "$projectDir/schemas") }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = "11" }
  buildFeatures { compose = true }
  sourceSets { getByName("androidTest").assets.srcDirs("$projectDir/schemas") }

  packaging { resources.excludes.add("META-INF/*") }
}

dependencies {
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.ui.test.junit4)
  androidTestImplementation(libs.ui.test.junit4)
  androidTestImplementation(platform(libs.androidx.compose.bom))

  annotationProcessor(libs.androidx.room.compiler)

  debugImplementation(libs.androidx.ui.test.manifest)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.ui.tooling)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.collection.ktx)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.fragment.ktx)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.reactivestreams.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.runtime.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.palette.ktx)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.sqlite.ktx)
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.dagger)
  implementation(libs.hilt.android)
  implementation(libs.kotlinx.collections.immutable)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.ktor.server.content.negotiation)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.material)
  implementation(libs.material3)
  implementation(libs.slf4j.android)
  implementation(libs.slf4j.api)
  implementation(libs.ui)
  implementation(libs.ui.graphics)
  implementation(libs.ui.tooling.preview)
  implementation(platform(libs.androidx.compose.bom))

  ksp(libs.androidx.room.compiler)
  ksp(libs.dagger.compiler)
  ksp(libs.hilt.compiler)

  testImplementation(libs.junit)
}
