package dev.ftb.mods.ftbessentials.util;

import dev.ftb.mods.ftbessentials.config.FTBEConfig;
import dev.ftb.mods.ftblibrary.util.Lazy;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class DimensionFilter {
    private static final Lazy<WildcardedRLMatcher> RTP_DIMENSION_MATCHER_B
            = Lazy.of(() -> new WildcardedRLMatcher(FTBEConfig.RTP_DIMENSION_BLACKLIST.get()));
    private static final Lazy<WildcardedRLMatcher> RTP_DIMENSION_MATCHER_W
            = Lazy.of(() -> new WildcardedRLMatcher(FTBEConfig.RTP_DIMENSION_WHITELIST.get()));

    private static final Lazy<WildcardedRLMatcher> ALL_DIMENSION_MATCHER_B_TO
            = Lazy.of(() -> new WildcardedRLMatcher(FTBEConfig.TELEPORTATION_BLACKLIST_TO.get()));
    private static final Lazy<WildcardedRLMatcher> ALL_DIMENSION_MATCHER_B_FROM
            = Lazy.of(() -> new WildcardedRLMatcher(FTBEConfig.TELEPORTATION_BLACKLIST_FROM.get()));

    public static boolean isRtpDimensionOK(ResourceKey<Level> levelKey) {
        Identifier name = levelKey.identifier();
        return !getRtpDimensionBlacklist().test(name) && (getRtpDimensionWhitelist().isEmpty() || getRtpDimensionWhitelist().test(name));
    }
    
    public static boolean isDimensionOKFrom(ResourceKey<Level> levelKey) {
        Identifier name = levelKey.identifier();
        return !getAllCommandDimensionBlacklistFrom().test(name);
    }

    public static boolean isDimensionOKTo(ResourceKey<Level> levelKey) {
        Identifier name = levelKey.identifier();
        return !getAllCommandDimensionBlacklistTo().test(name);
    }

    private static WildcardedRLMatcher getRtpDimensionWhitelist() {
        return RTP_DIMENSION_MATCHER_W.get();
    }

    private static WildcardedRLMatcher getRtpDimensionBlacklist() {
        return RTP_DIMENSION_MATCHER_B.get();
    }
    
    private static WildcardedRLMatcher getAllCommandDimensionBlacklistFrom() {
        return ALL_DIMENSION_MATCHER_B_FROM.get();
    }
    
    private static WildcardedRLMatcher getAllCommandDimensionBlacklistTo() {
        return ALL_DIMENSION_MATCHER_B_TO.get();
    }
    
    public static void clearMatcherCaches() {
        RTP_DIMENSION_MATCHER_B.invalidate();
        RTP_DIMENSION_MATCHER_W.invalidate();
        
        ALL_DIMENSION_MATCHER_B_FROM.invalidate();
        ALL_DIMENSION_MATCHER_B_TO.invalidate();
    }

    private static class WildcardedRLMatcher implements Predicate<Identifier> {
        private final Set<String> namespaces = new ObjectOpenHashSet<>();
        private final Set<Identifier> reslocs = new ObjectOpenHashSet<>();

        public WildcardedRLMatcher(Collection<String> toMatch) {
            Identifier location;

            for (String s : toMatch) {
                if (s.endsWith(":*")) {
                    namespaces.add(s.split(":")[0]);
                } else if ((location = Identifier.tryParse(s)) != null) {
                    reslocs.add(location);
                }
            }
        }

        public boolean isEmpty() {
            return reslocs.isEmpty() && namespaces.isEmpty();
        }

        @Override
        public boolean test(Identifier Identifier) {
            return reslocs.contains(Identifier) || namespaces.contains(Identifier.getNamespace());
        }
    }
}
