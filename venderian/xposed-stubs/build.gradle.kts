// stand-ins for the xposed api, compile only: lsposed provides the real classes at runtime,
// and refuses modules that ship their own copy
plugins {
    `java-library`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
