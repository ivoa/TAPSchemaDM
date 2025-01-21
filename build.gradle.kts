plugins {
    id("net.ivoa.vo-dml.vodmltools") version "0.5.14"
    id("com.diffplug.spotless") version "6.25.0"
    `maven-publish`
//    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
//    signing
}

group = "org.javastro.ivoa.dm"
version = "0.1-SNAPSHOT"


dependencies {
    api("org.javastro.ivoa.vo-dml:ivoa-base:1.0-SNAPSHOT") // IMPL using API so that it appears in transitive compile

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.h2database:h2:2.2.220") // try out h2
    implementation("org.slf4j:slf4j-api:1.7.32")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.12")

}

tasks.test {
    useJUnitPlatform()
}
//TODO integrate this into the main vodml plugin https://github.com/ivoa/vo-dml/issues/53
// use Spotless to reformat the generated code nicely.
spotless {
    java {
        target(vodml.outputJavaDir.asFileTree.matching(
            PatternSet().include("**/*.java")
        ))
        googleJavaFormat("1.12.0")
    }
}

tasks.named("spotlessJava") {
    dependsOn("vodmlJavaGenerate")
}

tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME) {
    dependsOn("spotlessApply")
}
// end of spotless config
