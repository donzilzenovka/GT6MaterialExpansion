package zenovka.gt6materialexpansion.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"zenovka.gt6materialexpansion.core."})
public final class MixerCorePlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() {
        return new String[]{"zenovka.gt6materialexpansion.core.MixerTransformer"};
    }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public void injectData(Map<String, Object> data) {}
    @Override public String getAccessTransformerClass() { return null; }
}
