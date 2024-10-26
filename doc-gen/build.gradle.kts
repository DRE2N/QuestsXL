plugins {
    `java-library`
}

dependencies {
    implementation(project(":plugin"))
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.annotationProcessorPath = configurations["annotationProcessor"]
}