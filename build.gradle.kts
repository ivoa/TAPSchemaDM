plugins {
    id("net.ivoa.vo-dml.vodmltools") version "0.5.20"
    id("com.diffplug.spotless") version "6.25.0"
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    signing
}

group = "org.javastro.ivoa.dm"
version = "0.1-SNAPSHOT"

vodml {
    outputSiteDir.set(layout.projectDirectory.dir("doc/site/generated")) // N.B the last part of this path must be "generated"

}
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
//exclude the persistence.xml - this jar will more than likely be used with another model
tasks.jar.configure {
    exclude("META-INF/persistence.xml")
}
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("TAP Schema Data Model")
                description.set("A VO-DML defined model for the TAP Schema")
                url.set("https://www.ivoa.net/documents/TAP/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("pahjbo")
                        name.set("Paul Harrison")
                        email.set("paul.harrison@manchester.ac.uk")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ivoa/TAPSchemaDM.git")
                    developerConnection.set("scm:git:ssh://github.com/ivoa/TAPSchemaDM.git")
                    url.set("https://github.com/ivoa/TAPSchemaDM")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype()
    }
}

// site tasks
tasks.register<Copy>("copyJavaDocForSite") {
    from(layout.buildDirectory.dir("docs/javadoc"))
    into(vodml.outputSiteDir.dir("javadoc"))
    dependsOn(tasks.javadoc)

}


tasks.register<Copy>("copySchemaForSite") {
    from(layout.buildDirectory.dir("generated/sources/vodml/schema"))
    into(vodml.outputSiteDir.dir("schema"))
    dependsOn("vodmlSchema")

}


tasks.register<Exec>("makeSiteNav")
{
    commandLine("yq","eval",  "(.nav | .. | select(has(\"AutoGenerated Documentation\"))|.[\"AutoGenerated Documentation\"]) += (load(\"doc/site/generated/allnav.yml\"))", "mkdocs_template.yml")
    standardOutput= file("mkdocs.yml").outputStream()
    dependsOn("vodmlSite")
    dependsOn("copyJavaDocForSite")
    dependsOn("copySchemaForSite")

}
tasks.register<Exec>("testSite"){
    commandLine("mkdocs", "serve")
    dependsOn("makeSiteNav")
}
tasks.register<Exec>("doSite"){
    commandLine("mkdocs", "gh-deploy", "--force")
    dependsOn("makeSiteNav")
}

