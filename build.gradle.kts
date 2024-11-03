// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    id("dev.rikka.tools.materialthemebuilder") version "1.5.1" apply false
    id("dev.rikka.tools.autoresconfig") version "1.2.2" apply false

}