import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

// the screens live in ../ui and get compiled into both the android app and this one
sourceSets["main"].kotlin.srcDir("../ui/src")

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation("junit:junit:4.13.2")
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.patternbox.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "patternbox"
            packageVersion = "0.4.0"
            description = "sandbox fighting game made of lock patterns"
            vendor = "jhu7600codes"
            // keep the bundled java small: only what compose + sockets need
            modules("java.desktop", "java.prefs", "jdk.unsupported", "java.naming")
            linux {
                shortcut = true
                menuGroup = "Game"
                appCategory = "Game"
                iconFile.set(project.file("icon.png"))
                debMaintainer = "patternbox@users.noreply.github.com"
            }
        }
    }
}
