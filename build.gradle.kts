plugins {
    java
    application
}

group = "ru.berrytrip"
version = "3.11.0"

// Java 8 target: jar запускается и на Java 8, и на Java 21, которая уже есть у Prism.
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("ru.berrytrip.berryup.Main")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

/**
 * Один автономный jar: классы лаунчера плюс FlatLaf и Gson внутри.
 * Плагин shadow не нужен — зависимостей всего две и сервис-файлы не требуются.
 */
val fatJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Собирает автономный BerryUp.jar со всеми зависимостями"
    archiveFileName.set("BerryUp.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "ru.berrytrip.berryup.Main",
            "Implementation-Title" to "BerryUp",
            "Implementation-Version" to version
        )
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")
    // Нативные библиотеки FlatLaf не используются: окно обычное, без native-декораций.
    // Без них в jar нет ни одного .dll/.so, и антивирусам нечего распаковывать в temp.
    exclude("com/formdev/flatlaf/natives/*.dll")
    exclude("com/formdev/flatlaf/natives/*.so")
    exclude("com/formdev/flatlaf/natives/*.dylib")
}

tasks.named("build") {
    dependsOn(fatJar)
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
}
