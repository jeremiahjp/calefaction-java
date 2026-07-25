plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.8.0"
    id("org.graalvm.buildtools.native") version "1.1.6"
}

group = "com.calefaction"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("net.dv8tion:JDA:6.5.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.4")
    implementation("com.google.maps:google-maps-services:2.2.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("org.springframework.boot:spring-boot-starter-test")
    runtimeOnly("io.netty.incubator:netty-incubator-codec-native-quic:0.0.75.Final:linux-x86_64")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

spotless {
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    java {
        target("src/main/java/**/*.java", "src/main/test/**/*.java")
        removeUnusedImports()
        importOrder("")
        trimTrailingWhitespace()
        indentWithSpaces(4)
        toggleOffOn()
        endWithNewline()
    }
}
