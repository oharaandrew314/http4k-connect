dependencies {
    api(project(":http4k-connect-google-analytics-core"))
    api(Libs.http4k_format_moshi) {
        exclude("org.jetbrains.kotlin", "kotlin-reflect")
    }
    implementation(Libs.api)

    kapt(libs.symbol.processing.api)
    kapt(Libs.se_ansman_kotshi_compiler)

    implementation(Libs.http4k_format_core)
}