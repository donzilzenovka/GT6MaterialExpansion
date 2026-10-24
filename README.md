# GT6MaterialExpansion

Experimental GregTech 6 material-data add-on for Minecraft Forge 1.7.10, targeting GT6 6.17.06.

## Initial source commit

The prototype registers four named materials in GT6's custom material ID range (32010–32013): Silt, Loam, Humus, and Soil. Silt uses GT6 Siltstone as its mineral proxy. Loam provisionally uses Sand:Silt:Clay in a 4:4:2 material-unit ratio. Humus and Soil are uncalibrated placeholders.

A limited set of vanilla and Biomes O' Plenty item stacks is assigned material data if no existing GT6 data is found. No blocks, items, crafting recipes, or ore-dictionary aliases are intentionally added.

## IMPORTANT: experimental and not build-tested

There is not yet a reproducible Gradle build or compiled JAR. GT6's OreDictManager.addItemData may also register recycling data and alter game behavior; we must verify this before describing the add-on as metadata-only. Uncalibrated Humus and Soil have no reliable mass yet. Registry names, load timing, material ID collisions, and the exact GT6 6.17.06 API must be checked in a test instance.

Next: establish build toolchain, compile against GT6 6.17.06, verify loading, then refine soil compositions and pack-specific associations.
