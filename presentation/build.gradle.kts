import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
android {
    namespace = "org.evoionosp.abstrak.presentation"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
androidComponents {
    onVariants { variant ->
        val generateAppIcons =
            tasks.register<GenerateAppIconsTask>(
                "generate${variant.name.replaceFirstChar(Char::uppercaseChar)}AppIcons",
            ) {
                foregroundTemplate.set(layout.projectDirectory.file("src/main/app-icon/foreground_template.xml"))
                palettes.set(layout.projectDirectory.file("src/main/res/values/app_icon_palettes.xml"))
            }
        variant.sources.res?.addGeneratedSourceDirectory(generateAppIcons, GenerateAppIconsTask::outputDirectory)
    }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":playback"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    ksp(libs.hilt.compiler)
    testImplementation(libs.bundles.unitTest)
    testImplementation(libs.bundles.androidUnitTest)
    testImplementation(testFixtures(project(":domain")))
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

/**
 * Generates the launcher icons from a single vector. Every icon is the same Folded A in its own
 * colors: for each palette in res/values/app_icon_palettes.xml (the colors app_icon_<icon>_*),
 * it writes @drawable/app_icon_<icon>_foreground from src/main/app-icon/foreground_template.xml,
 * and the adaptive icon @mipmap/app_icon_<icon>: that foreground on the palette's background,
 * with the shared monochrome layer for themed icons.
 */
@CacheableTask
abstract class GenerateAppIconsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val foregroundTemplate: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val palettes: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val colors = colorNames(palettes.get().asFile)
        val icons = colors.mapNotNull { BACKGROUND.matchEntire(it)?.groupValues?.get(1) }.sorted()
        check(icons.isNotEmpty()) { "No app icon palettes (app_icon_<icon>_background) in ${palettes.get().asFile}" }
        val template =
            foregroundTemplate
                .get()
                .asFile
                .readText()
                .replace(COMMENT, "")
                .trim()
        val facets = FACET_COLOR.findAll(template).map { it.groupValues[1] }.toSortedSet()
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val drawables = output.resolve("drawable").apply { mkdirs() }
        val mipmaps = output.resolve("mipmap-anydpi-v26").apply { mkdirs() }
        icons.forEach { icon ->
            val missing = facets.map { "app_icon_${icon}_$it" }.filterNot { it in colors }
            check(missing.isEmpty()) { "App icon palette '$icon' is missing ${missing.joinToString()}" }
            val foreground = template.replace("{icon}", icon)
            drawables.resolve("app_icon_${icon}_foreground.xml").writeText("$HEADER$foreground\n")
            mipmaps.resolve("app_icon_$icon.xml").writeText(HEADER + adaptiveIcon(icon))
        }
    }

    private fun colorNames(file: File): Set<String> {
        val nodes =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("color")
        return (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("name") }.toSet()
    }

    private fun adaptiveIcon(icon: String): String =
        """
        |<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
        |    <background android:drawable="@color/app_icon_${icon}_background" />
        |    <foreground android:drawable="@drawable/app_icon_${icon}_foreground" />
        |    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
        |</adaptive-icon>
        |
        """.trimMargin()

    private companion object {
        val BACKGROUND = Regex("app_icon_(\\w+)_background")
        val FACET_COLOR = Regex("@color/app_icon_\\{icon\\}_(\\w+)")
        val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
        const val HEADER =
            "<!-- Generated by GenerateAppIconsTask from src/main/app-icon/foreground_template.xml " +
                "and res/values/app_icon_palettes.xml. Edit those instead. -->\n"
    }
}
