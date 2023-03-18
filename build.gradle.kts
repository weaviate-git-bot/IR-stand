import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.0.4"
    id("io.spring.dependency-management") version "1.1.0"
    kotlin("jvm") version "1.8.10"
    kotlin("plugin.spring") version "1.8.10"
    kotlin("kapt") version "1.8.10"
    id("me.champeau.jmh") version "0.7.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.2.0"
    id("org.jlleitschuh.gradle.ktlint-idea") version "11.2.0"
    id("com.star-zero.gradle.githook") version "1.2.1"
}

group = "ru.itmo"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") } // djl nightly snapshots
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework:spring-web")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("com.github.haifengl:smile-core:${findProperty("smileVersion")}")
    implementation("com.github.haifengl:smile-kotlin:${findProperty("smileVersion")}")
    implementation("info.picocli:picocli-spring-boot-starter:4.7.0")

    implementation("org.springframework.boot:spring-boot-starter-data-elasticsearch")
    implementation("org.apache.lucene:lucene-queryparser:9.5.0")
    implementation("org.apache.lucene:lucene-grouping:9.5.0")

    implementation("edu.stanford.nlp:stanford-corenlp:${findProperty("stanfordCoreNlpVersion")}")
    implementation("edu.stanford.nlp:stanford-corenlp:${findProperty("stanfordCoreNlpVersion")}:models")
    implementation("org.tensorflow:tensorflow:1.4.0")
    implementation("com.h2database:h2-mvstore:2.1.214")

    // djl
    implementation(platform("ai.djl:bom:${findProperty("djlVersion")}"))
    implementation("ai.djl:api")
    implementation("ai.djl.pytorch:pytorch-engine")
    implementation("ai.djl.huggingface:tokenizers")

    kapt("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:${findProperty("mockkVersion")}")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jmh {
    includes.set(listOf(".*")) // include pattern (regular expression) for benchmarks to be executed
    warmupIterations.set(2) // Number of warmup iterations to do
    iterations.set(2) // Number of measurement iterations to do
    fork.set(2) // How many times to forks a single benchmark. Use 0 to disable forking altogether
    zip64.set(true) // is used for big archives (more than 65535 entries)
}

ktlint {
    version.set("0.48.2")
}

githook {
    hooks {
        register("pre-commit") {
            task = "--continue --parallel ktlintFormat"
        }
        register("pre-push") {
            task = "--continue --parallel ktlintCheck"
        }
    }
}
