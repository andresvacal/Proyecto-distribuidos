plugins {
    id("java")
}

group = "javeriana.edu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Añadir JeroMQ como dependencia
    implementation("org.zeromq:jeromq:0.5.2")
}

tasks.test {
    useJUnitPlatform()
}