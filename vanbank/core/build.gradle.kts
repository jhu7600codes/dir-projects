plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Plain Kotlin/JVM module: no Android dependency, so this builds and its
// tests run with nothing but a JDK, e.g. in a sandbox without the Android
// SDK installed:
//
//   ./gradlew :core:test
//
// Everything in here is deterministic, pure Kotlin: password hashing, DIR
// account/card number generation (with a real Luhn checksum), loan interest
// amortization, statement totals, and budget category aggregation.

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.test {
    useJUnit()
}
