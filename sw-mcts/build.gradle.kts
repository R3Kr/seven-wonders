plugins {
    kotlin("jvm")
}


dependencies {
    implementation(projects.swEngine)
    implementation(projects.swCommonModel)
    testImplementation(kotlin("test"))
}

