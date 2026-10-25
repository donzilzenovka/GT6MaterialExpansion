# GT6MaterialExpansion

Experimental GregTech 6 material-data add-on for Minecraft Forge **1.7.10-10.13.4.1614**, targeting **GT6 6.17.06**.

## Material model

**Minimal, non-invasive material enrichment.** Preserve GT6's existing material identity and item data. Only annotate missing material associations; do not introduce extra materials when GT6 already provides one. Existing named materials (e.g. Wheat) retain intrinsic water and biological content rather than being recursively broken down.

- Dirt: 9U of **Soil** (new terminal material, ore dictionary name `Soil`).
- Silt: **GT6 Siltstone** (no custom Silt material).
- Loam: **40% GT6 Siltstone, 20% GT6 Clay, 40% Soil** by component material units (new material `Loam`).
- Biomass: reuse **GT6 Biomass** (`MT.Biomass`); do not register a duplicate. Note GT6 declares it using a liquid/fluid material helper, so test solid item-data/recycling suitability before applying it to compost, mulch or food.
- Humus: proposed Soil + Biomass, but **not yet assigned**; Forestry recipe amounts and processing need review.
- Water: intrinsic to materials, not an extra breakdown component in this model.

Custom IDs 32011 and 32013 remain provisional and may collide with other custom materials. No new blocks, items, recipes or ore dictionary aliases are registered. Existing item data is never overwritten.

## Recycling is intentional

GT6's item material data may register recycling information. This is desirable: objects should be recoverable as their constituent materials through appropriate GregTech machinery. Composition and recoverable output must be validated separately, however, to account for moisture, processing losses, and machine-specific behavior. Never promise exact recovery solely from a composition tooltip.

## Build toolchain target

Target **Gradle 8.x** with a modern legacy-Minecraft build plugin compatible with Forge 1.7.10. Do **not** use original ForgeGradle 1.x directly with Gradle 8; verify the chosen plugin and its Java 8 bytecode output before committing a wrapper or claiming a successful build. The Gradle runtime JDK and Minecraft target JDK may differ.

## Build and validation status

Build verified against Forge **1.7.10-10.13.4.1614** and **GT6 6.17.06**, producing Java 8 bytecode. The user has confirmed an in-game test showing dirt as 9.00 units of Soil with the previous implementation. The revised Loam definition and plain material names are not yet build- or runtime-verified; recycling still needs testing.

## Building

The included Gradle wrapper pins **Gradle 8.14.3**. RetroFuturaGradle **1.4.9** provides the legacy Forge build environment, matching the build-plugin generation used by IguanaTweaks. RetroFuturaGradle 2.0.2 contains Java 25 bytecode that this Gradle version cannot process.

Place the matching **GT6 6.17.06** mod jar at `libs/gregtech_1.7.10-6.17.06.jar` (deliberately ignored by Git). A Java 8 JDK is required for compilation; the build was verified with Java 8 also running Gradle.

Run `gradlew.bat build` on Windows or `./gradlew build` on other platforms. The distributable is `build/libs/GT6MaterialExpansion-0.1.0.jar`. GT6 is compile-only and must be installed separately in the game.
