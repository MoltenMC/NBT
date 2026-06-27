plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

group   = "io.github.agent0876"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(25)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId    = "io.github.agent0876",
        artifactId = "nbt",
        version    = "1.0.0"
    )

    pom {
        name.set("nbt")
        description.set("Kotlin NBT library for Minecraft Java and Bedrock Edition")
        url.set("https://github.com/MoltenMC/NBT")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("agent0876")
                name.set("Agent0876")
                email.set("shinseungmin070920@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/MoltenMC/NBT.git")
            developerConnection.set("scm:git:ssh://github.com/MoltenMC/NBT.git")
            url.set("https://github.com/MoltenMC/NBT")
        }
    }
}
