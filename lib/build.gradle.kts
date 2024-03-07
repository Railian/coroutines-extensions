import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {

    jvm()

    targets.withType(KotlinNativeTarget::class)
        .configureEach {
            binaries.framework {
                baseName = "lib"
                isStatic = true
            }
        }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    explicitApi = ExplicitApiMode.Strict
}
