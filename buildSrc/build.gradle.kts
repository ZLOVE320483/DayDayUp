repositories {
    jcenter()
    google()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    // --- gradle ---
    implementation("com.android.tools.build:gradle:4.0.2")
    implementation("org.javassist:javassist:3.22.0-GA")
    implementation("commons-io:commons-io:2.6")
}