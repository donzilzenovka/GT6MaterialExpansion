# GT6MaterialExpansion

Experimental GregTech 6 material-data add-on for Minecraft Forge **1.7.10-10.13.4.1614**, targeting **GT6 6.17.06**.

## Material model

The aim is to register foundational material compositions for vanilla and modded items, using GregTech's native material quantities. Mass and recycling should both derive from those compositions.

The initial source registers four named materials in GT6's custom material ID range (32010–32013): Silt, Loam, Humus, and Soil. Silt provisionally uses GT6 Siltstone as its mineral proxy. Loam provisionally uses Sand:Silt:Clay in a 4:4:2 material-unit ratio. Humus and Soil are currently uncalibrated placeholders.

A limited set of vanilla and Biomes O' Plenty item stacks is assigned material data if no existing GT6 data is found. No new blocks, items, crafting recipes, or ore-dictionary aliases are intentionally added.

## Recycling is intentional

GT6's item material data may register recycling information. This is desirable: objects should be recoverable as their constituent materials through appropriate GregTech machinery. Composition and recoverable output must be validated separately, however, to account for moisture, processing losses, and machine-specific behavior. Never promise exact recovery solely from a composition tooltip.

## Build and validation status

**Experimental source; not compiled or tested.** A reproducible Forge 1.7.10-10.13.4.1614 Gradle environment and GT6 6.17.06 dependency still need to be established. Material IDs, actual GT6 API compatibility, mod registry names, and registration timing must be checked.

Next: build against the specified Forge/GT6 versions, confirm GT6 tooltip material quantities and weight, and test recycling recipes in a disposable world.
