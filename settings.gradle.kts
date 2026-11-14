import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.kotlinx.kover.aggregation") version "0.9.9"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "abstrak-android"
include(":app")
include(":data")
include(":domain")
include(":presentation")
include(":playback")
val coverageFloors =
    mapOf(
        ":domain" to 0,
        ":data" to 0,
        ":playback" to 0,
        ":presentation" to 0,
        ":app" to 0,
    )
kover {
    enableCoverage()
    reports {
        excludedClasses.addAll(
            "*_Factory",
            "*_Factory\$*",
            "*_MembersInjector",
            "*_HiltModules*",
            "*_GeneratedInjector",
            "Hilt_*",
            "*.Hilt_*",
            "hilt_aggregated_deps.*",
            "dagger.hilt.*",
            "*_Impl",
            "*_Impl\$*",
            "*ComposableSingletons*",
            "*.BuildConfig",
            "*.R",
            "*.R\$*",
            "*.AbstrakApplication",
            "*Activity",
            "*Activity\$*",
            "*.PlaybackService",
            "*.di.*Module",
            "*.PlaybackModule",
        )
        excludesAnnotatedBy.addAll(
            "androidx.compose.runtime.Composable",
            "androidx.compose.ui.tooling.preview.Preview",
        )
        verify {
            warningInsteadOfFailure.set(false)
            eachProjectRule {
                val floor = coverageFloors[projectPath] ?: 0
                bound {
                    minValue.set(floor)
                    coverageUnits.set(CoverageUnit.LINE)
                    aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
                }
            }
            rule("Aggregate line coverage") {
                bound {
                    minValue.set(0)
                    coverageUnits.set(CoverageUnit.LINE)
                    aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
                }
            }
        }
    }
}
