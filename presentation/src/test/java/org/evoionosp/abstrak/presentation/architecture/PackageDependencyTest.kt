package org.evoionosp.abstrak.presentation.architecture
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageDependencyTest {
    @Test
    fun `design system depends on nothing app specific`() {
        val violations =
            importsIn("designsystem").filter { (_, import) ->
                import.startsWith(APP_PREFIX) &&
                    !import.startsWith("$PRESENTATION_PREFIX.designsystem.") &&
                    import != RESOURCES
            }
        assertNoViolations(violations)
    }

    @Test
    fun `shared ui depends only on the design system`() {
        val violations =
            importsIn("ui").filter { (_, import) ->
                presentationPackageOf(import) !in setOf(null, "ui", "designsystem")
            }
        assertNoViolations(violations)
    }

    @Test
    fun `features depend only on the shared layers`() {
        val features =
            sourceRoot
                .listFiles()
                .orEmpty()
                .filter { it.isDirectory && it.name !in SHELL_AND_SHARED }
                .map { it.name }
        assertTrue("No feature packages found under $sourceRoot", features.isNotEmpty())
        val violations =
            features.flatMap { feature ->
                importsIn(feature).filter { (_, import) ->
                    presentationPackageOf(import) !in setOf(null, feature, "designsystem", "ui") &&
                        import !in ALLOWED_EXCEPTIONS[feature].orEmpty()
                }
            }
        assertNoViolations(violations)
    }

    private fun importsIn(topLevelPackage: String): List<Pair<File, String>> {
        val directory = File(sourceRoot, topLevelPackage)
        check(directory.isDirectory) { "Missing $directory; run from the :presentation module directory" }
        return directory
            .walkTopDown()
            .filter { it.extension == "kt" }
            .flatMap { file ->
                file
                    .readLines()
                    .filter { it.startsWith("import ") }
                    .map { file to it.removePrefix("import ").substringBefore(" as ").trim() }
            }.toList()
    }

    private fun presentationPackageOf(import: String): String? =
        import
            .takeIf { it.startsWith("$PRESENTATION_PREFIX.") && it != RESOURCES }
            ?.removePrefix("$PRESENTATION_PREFIX.")
            ?.substringBefore('.')

    private fun assertNoViolations(violations: List<Pair<File, String>>) {
        assertTrue(
            "Disallowed imports:\n" +
                violations.joinToString("\n") { (file, import) -> "  ${file.relativeTo(sourceRoot)}: $import" },
            violations.isEmpty(),
        )
    }

    private companion object {
        const val APP_PREFIX = "org.evoionosp.abstrak."
        const val PRESENTATION_PREFIX = "org.evoionosp.abstrak.presentation"
        const val RESOURCES = "$PRESENTATION_PREFIX.R"
        val sourceRoot = File("src/main/java/org/evoionosp/abstrak/presentation")
        val SHELL_AND_SHARED = setOf("designsystem", "ui", "navigation")
        val ALLOWED_EXCEPTIONS =
            mapOf(
                "library" to
                    setOf(
                        "$PRESENTATION_PREFIX.home.CatalogEffects",
                        "$PRESENTATION_PREFIX.home.HomeUiState",
                        "$PRESENTATION_PREFIX.home.HomeViewModel",
                    ),
                "bookdetails" to
                    setOf(
                        "$PRESENTATION_PREFIX.navigation.BOOK_DETAILS_ARG_AUDIOBOOK_ID",
                        "$PRESENTATION_PREFIX.navigation.BOOK_DETAILS_ARG_LIBRARY_ID",
                    ),
            )
    }
}
