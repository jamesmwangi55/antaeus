plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC")
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
}