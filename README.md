# [Black Cat Game](https://mattemade.itch.io/pelican-valley) for the [Black Cat Jam #1](https://itch.io/jam/black-cat-jam-1/rate/2801669)

Made in 5 days (roughly 30 hours of dev time).

Assets are made by:
* Mark Myskov ([itch](https://m-for-mediocre.itch.io/), [github](https://mformarker.github.io/markmyskov/index.html)): sounds and music
* itsjustwoody ([itch](https://itsjustwoody.itch.io/)): all the graphics

Tech used by me:
* [littleKt](https://littlekt.com/) as the main game framework
* an [old Box2D port](https://central.sonatype.com/namespace/com.soywiz.korlibs.kbox2d) from [KorGE](https://korge.org/)
* [Tiled](https://www.mapeditor.org/) as the map editor
* [GIMP](https://www.gimp.org/) to sketch, crop and re-export stuff

## Project is based on a Starter Game Template for the LittleKt Game Framework

This template repository contains a base project for creating games with [LittleKt](https://littlekt.com). It contains
the bare necessities to get a LittleKt project up and running. This includes the necessary plugins, dependencies and
source set structure.

This project is set up to use all the available platforms that LittleKt currently supports: **JVM**, **Web**, and **Android**. 
If a certain platform isn't needed, simply deleting the source directory and the source sets in
the `build.gradle.kts` file will get rid of it.

## Usage

Clone this repo and open up in IntelliJ to get started. Each platform target contains a class to execute for their
respective platform.

### JVM

**Running:**

Run `LwjglApp` to execute on the desktop.

**Deploying:**

A custom deploy task is created specifically for JVM. Run the `package/packageFatJar` gradle task to create a fat
executable JAR. This task can be tinkered with in the `build.gradlek.kts` file.

If and when the packages are renamed from `com.game.template.LwjglApp` to whatever, ensure to update the `jvm.mainClass`
property in the `gradle.properties` file to ensure that the `packageFatJar` task will work properly.

### JS

**Running:**

Run the `kotlin browser/jsBrowserRun` gradle task like any other **Kotlin/JS** project to run in development mode.

**Deploying:**

Run the `kotlin browser/jsBrowserDistribution` gradle task to create a distribution build. This build will require a
webserver in order to run.
