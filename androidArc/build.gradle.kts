plugins {
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.jreleaser)
}

group = "io.github.eugenprog"
if (version == Project.DEFAULT_VERSION) {
    version = "0.1.0-SNAPSHOT"
}

android {
    namespace = "com.kogen.androidarc"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.coroutines)

    // Compose: only what ScreenContainerWrapper needs to bridge a ViewModel's state/effects to a
    // screen Composable.
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.github.eugenprog"
                artifactId = "androidarc"
                version = project.version.toString()

                pom {
                    name.set("androidArc")
                    description.set("Minimal MVI ViewModel base class and Compose screen container for Android")
                    url.set("https://github.com/KoGen-libs/androidArc")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("EugenProg")
                            name.set("Eugen Kopp")
                            email.set("Eugen.kopp.kz@gmail.com")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/KoGen-libs/androidArc.git")
                        developerConnection.set("scm:git:ssh://github.com:KoGen-libs/androidArc.git")
                        url.set("https://github.com/KoGen-libs/androidArc/tree/master")
                    }
                }
            }
        }
        repositories {
            maven {
                setUrl(layout.buildDirectory.dir("staging-deploy"))
            }
        }
    }

    val signingKey = System.getenv("JRELEASER_GPG_SECRET_KEY")
    val signingPassword = System.getenv("JRELEASER_GPG_PASSPHRASE")
    if (!signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        signing {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications["release"])
        }
    }
}
