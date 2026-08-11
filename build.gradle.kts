plugins {
    java
    // Fat-jar plugin: shades dependencies (Gson) into the jar because Burp
    // does NOT provide them at runtime. Burp DOES provide montoya-api at runtime.
    id("com.gradleup.shadow") version "9.6.0"
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
    compileOnly("net.portswigger.burp.extensions:montoya-api:2026.7")

    // NOT provided by Burp -> real dependency, shaded into the fat jar.
    implementation("com.google.code.gson:gson:2.14.0")

    // Test-only: JUnit 5. junit-platform-launcher is added explicitly so the
    // `test` task works on Gradle 9 (which no longer auto-provides it).
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")
}

tasks.shadowJar {
    // Stable output name so the README/CI instructions are predictable.
    archiveBaseName.set("burp-lookup")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())

    // NOTE: Gson is bundled but NOT relocated. Burp loads each extension in its
    // own isolated classloader, so a bundled-dependency clash across extensions
    // isn't a concern — and Shadow's relocation remapper has an ASM bug that
    // fails on classes carrying `long` constants (our cache TTL). Bundling
    // without relocation is the correct, working choice here.
}

// Make `build` produce the shaded jar.
tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
