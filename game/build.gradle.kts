import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

repositories {
    maven(url = "https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        compilations {
            val main by getting

            val mainClassName = (findProperty("jvm.mainClass") as? String)?.plus("Kt")
            if (mainClassName == null) {
                project.logger.log(
                    LogLevel.ERROR,
                    "Property 'jvm.mainClass' has either changed or has not been set. Check 'gradle.properties' and ensure it is properly set!"
                )
            }
            tasks {
                register<Copy>("copyResources") {
                    group = "package"
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmProcessResources"))
                    from(main.output.resourcesDir)
                    destinationDir = File("${layout.buildDirectory.asFile.get()}/publish")
                }
                register<Jar>("packageFatJar") {
                    group = "package"
                    archiveClassifier.set("all")
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    dependsOn(named("jvmJar"))
                    dependsOn(named("copyResources"))
                    manifest {
                        attributes["Main-Class"] = mainClassName
                    }
                    destinationDirectory.set(File("${layout.buildDirectory.asFile.get()}/publish/"))
                    from(
                        main.runtimeDependencyFiles.map { if (it.isDirectory) it else zipTree(it) },
                        main.output.classesDirs
                    )
                    doLast {
                        project.logger.lifecycle("[LittleKt] The packaged jar is available at: ${outputs.files.first().parent}")
                    }
                }
                // workaround for https://youtrack.jetbrains.com/issue/KT-62214
                if (Os.isFamily(Os.FAMILY_MAC) && mainClassName != null) {
                    register<JavaExec>("jvmRun") {
                        jvmArgs("-XstartOnFirstThread")
                        mainClass.set(mainClassName)
                        kotlin {
                            val mainCompile = targets["jvm"].compilations["main"]
                            dependsOn(mainCompile.compileAllTaskName)
                            classpath(
                                { mainCompile.output.allOutputs.files },
                                (configurations["jvmRuntimeClasspath"])
                            )
                        }
                    }
                }
            }
        }
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    js(KotlinJsCompilerType.IR) {
        binaries.executable()
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }

        this.attributes.attribute(
            KotlinPlatformType.attribute,
            KotlinPlatformType.js
        )

        compilations.all {
            kotlinOptions.sourceMap = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.littlekt.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation("com.soywiz.korlibs.kbox2d:kbox2d:3.3.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation(libs.kotlinx.html.js)
            }
        }
        val jsTest by getting
    }
}
