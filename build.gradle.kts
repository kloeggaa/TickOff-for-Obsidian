buildscript {
    dependencies {
        // Build-time only (nothing here ships in the APK): AGP 9.4.1 pulls in versions of these with
        // known advisories. Raise them to the first fixed releases; drop each line once AGP ships
        // that version or newer itself (check with `./gradlew buildEnvironment`).
        constraints {
            classpath("org.bouncycastle:bcprov-jdk18on:1.85") { because("GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26, GHSA-c3fc-8qff-9hwx") }
            classpath("org.bouncycastle:bcpkix-jdk18on:1.85") { because("GHSA-wg6q-6289-32hp") }
            classpath("org.bouncycastle:bcutil-jdk18on:1.85") { because("must match bcprov") }
            classpath("org.jdom:jdom2:2.0.6.1") { because("GHSA-2363-cqg2-863c") }
            classpath("org.bitbucket.b_c:jose4j:0.9.6") { because("GHSA-3677-xxcr-wjqv") }
            classpath("org.apache.commons:commons-lang3:3.18.0") { because("GHSA-j288-q9x7-2f5v") }
        }
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    // AGP 9 compiles Kotlin itself (built-in Kotlin); this declaration only pins the Kotlin Gradle
    // plugin version it uses, which must match the Compose compiler plugin below.
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
