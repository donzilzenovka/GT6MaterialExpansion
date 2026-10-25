# GT6MaterialExpansion

Experimental GregTech 6 material-data add-on for Minecraft Forge **1.7.10-10.13.4.1614**, targeting **GT6 6.17.06**.

## Material model

The aim is to register foundational material compositions for vanilla and modded items, using GregTech's native material quantities. Mass and recycling should both derive from those compositions.

The initial source registers four named materials in GT6's custom material ID range (32010–32013): Silt, Loam, Humus, and Soil. Silt provisionally uses GT6 Siltstone as its mineral proxy. Loam provisionally uses Sand:Silt:Clay in a 4:4:2 material-unit ratio. Humus and Soil are currently uncalibrated placeholders.

A limited set of vanilla and Biomes O' Plenty item stacks is assigned material data if no existing GT6 data is found. No new blocks, items, crafting recipes, or ore-dictionary aliases are intentionally added.

## Recycling is intentional

GT6's item material data may register recycling information. This is desirable: objects should be recoverable as their constituent materials through appropriate GregTech machinery. Composition and recoverable output must be validated separately, however, to account for moisture, processing losses, and machine-specific behavior. Never promise exact recovery solely from a composition tooltip.

## Build toolchain target

Target **Gradle 8.x** with a modern legacy-Minecraft build plugin compatible with Forge 1.7.10. Do **not** use original ForgeGradle 1.x directly with Gradle 8; verify the chosen plugin and its Java 8 bytecode output before committing a wrapper or claiming a successful build. The Gradle runtime JDK and Minecraft target JDK may differ.

## Build and validation status

Build verified against Forge **1.7.10-10.13.4.1614** and **GT6 6.17.06**, producing Java 8 bytecode. In-game startup, material IDs, mod registry names, registration timing, tooltip quantities/weight, and recycling behavior still require validation in a disposable world.

## Building

The included Gradle wrapper pins **Gradle 8.14.3**. RetroFuturaGradle **1.4.9** provides the legacy Forge build environment, matching the build-plugin generation used by IguanaTweaks. RetroFuturaGradle 2.0.2 contains Java 25 bytecode that this Gradle version cannot process.

Place the matching **GT6 6.17.06** mod jar at `libs/gregtech_1.7.10-6.17.06.jar` (deliberately ignored by Git). A Java 8 JDK is required for compilation; the build was verified with Java 8 also running Gradle.

Run `gradlew.bat build` on Windows or `./gradlew build` on other platforms. The distributable is `build/libs/GT6MaterialExpansion-0.1.0.jar`. GT6 is compile-only and must be installed separately in the game.
