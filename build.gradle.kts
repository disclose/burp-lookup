plugins {
    java
    // Fat-jar plugin: shades dependencies (Gson) into the jar because Burp
    // does NOT provide them at runtime. Burp DOES provide montoya-api at runtime.
    id("com.gradleup.shadow") version "8.3.5"
}

group = "io.disclose"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    // Burp ships with a modern JRE; target 17 for broad compatibility.
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Provided by Burp at runtime -> compileOnly (must NOT be shaded in).
    compileOnly("net.portswigger.burp.extensions:montoya-api:2026.4")

    // NOT provided by Burp -> real dependency, shaded into the fat jar.
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.shadowJar {
    // Stable output name so the README/CI instructions are predictable.
    archiveBaseName.set("burp-lookup")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())

    // Relocate Gson so it can never clash with another extension's bundled
    // copy inside the shared Burp JVM.
    relocate("com.google.gson", "io.disclose.burplookup.shaded.gson")
}

// Make `build` produce the shaded jar.
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
