package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class DimensionFilter {
    private static WildcardedRLMatcher rtpDimensionMatcherB = null;
    private static WildcardedRLMatcher rtpDimensionMatcherW = null;

    private static WildcardedRLMatcher allDimensionMatcherBTo = null;
    private static WildcardedRLMatcher allDimensionMatcherBFrom = null;

    public static boolean isRtpDimensionOK(ResourceKey<Level> levelKey) {
        ResourceLocation name = levelKey.location();
        return !getRtpDimensionBlacklist().test(name) && (getRtpDimensionWhitelist().isEmpty() || getRtpDimensionWhitelist().test(name));
    }
    
    public static boolean isDimensionOKFrom(ResourceKey<Level> levelKey) {
        ResourceLocation name = levelKey.location();
        return !getAllCommandDimensionBlacklistFrom().test(name);
    }

    public static boolean isDimensionOKTo(ResourceKey<Level> levelKey) {
        ResourceLocation name = levelKey.location();
        return !getAllCommandDimensionBlacklistTo().test(name);
    }

    private static WildcardedRLMatcher getRtpDimensionWhitelist() {
        if (rtpDimensionMatcherW == null) {
            rtpDimensionMatcherW = new WildcardedRLMatcher(FTBEConfig.RTP_DIMENSION_WHITELIST.get());
        }
        return rtpDimensionMatcherW;
    }

    private static WildcardedRLMatcher getRtpDimensionBlacklist() {
        if (rtpDimensionMatcherB == null) {
            rtpDimensionMatcherB = new WildcardedRLMatcher(FTBEConfig.RTP_DIMENSION_BLACKLIST.get());
        }
        return rtpDimensionMatcherB;
    }
    
    private static WildcardedRLMatcher getAllCommandDimensionBlacklistFrom() {
        if (allDimensionMatcherBFrom == null) {
            allDimensionMatcherBFrom = new WildcardedRLMatcher(FTBEConfig.TELEPORTATION_BLACKLIST_FROM.get());
        }
        return allDimensionMatcherBFrom;
    }
    
    private static WildcardedRLMatcher getAllCommandDimensionBlacklistTo() {
        if (allDimensionMatcherBTo == null) {
            allDimensionMatcherBTo = new WildcardedRLMatcher(FTBEConfig.TELEPORTATION_BLACKLIST_TO.get());
        }
        return allDimensionMatcherBTo;
    }
    
    public static void clearMatcherCaches() {
        rtpDimensionMatcherB = null;
        rtpDimensionMatcherW = null;
        
        allDimensionMatcherBFrom = null;
        allDimensionMatcherBTo = null;
    }

    private static class WildcardedRLMatcher implements Predicate<ResourceLocation> {
        private final Set<String> namespaces = new ObjectOpenHashSet<>();
        private final Set<ResourceLocation> reslocs = new ObjectOpenHashSet<>();

        public WildcardedRLMatcher(Collection<String> toMatch) {
            ResourceLocation location;

            for (String s : toMatch) {
                if (s.endsWith(":*")) {
                    namespaces.add(s.split(":")[0]);
                } else if ((location = ResourceLocation.tryParse(s)) != null) {
                    reslocs.add(location);
                }
            }
        }

        public boolean isEmpty() {
            return reslocs.isEmpty() && namespaces.isEmpty();
        }

        @Override
        public boolean test(ResourceLocation resourceLocation) {
            return reslocs.contains(resourceLocation) || namespaces.contains(resourceLocation.getNamespace());
        }
    }
}
