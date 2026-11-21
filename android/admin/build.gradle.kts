plugins { id("com.android.application") }
val releaseSigning = listOf("PULSE_KEYSTORE", "PULSE_STORE_PASSWORD", "PULSE_KEY_ALIAS", "PULSE_KEY_PASSWORD")
    .map { System.getenv(it) }
if (releaseSigning.any { !it.isNullOrBlank() } && releaseSigning.any { it.isNullOrBlank() }) {
    error("All PULSE signing variables must be provided together")
}
android {
    namespace = "dev.dozyfj.chatpuls.admin"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.dozyfj.chatpuls.admin"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    if (releaseSigning.all { !it.isNullOrBlank() }) {
        signingConfigs.create("pulseRelease") {
            storeFile = file(releaseSigning[0]!!)
            storePassword = releaseSigning[1]
            keyAlias = releaseSigning[2]
            keyPassword = releaseSigning[3]
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("pulseRelease")
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
}
