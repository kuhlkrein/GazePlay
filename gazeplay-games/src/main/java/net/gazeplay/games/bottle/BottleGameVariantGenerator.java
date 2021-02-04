package net.gazeplay.games.bottle;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.gamevariants.IntStringGameVariant;
import net.gazeplay.commons.gamevariants.generators.IGameVariantGenerator;

import java.util.Set;

public class BottleGameVariantGenerator implements IGameVariantGenerator {
    @Override
    public Set<IGameVariant> getVariants() {
        return Sets.newLinkedHashSet(Lists.newArrayList(
            new IntStringGameVariant(4, "NORMAL"),
            new IntStringGameVariant(8, "NORMAL"),
            new IntStringGameVariant(12, "NORMAL"),
            new IntStringGameVariant(16, "NORMAL"),
            new IntStringGameVariant(4, "HIGH"),
            new IntStringGameVariant(8, "HIGH"),
            new IntStringGameVariant(12, "HIGH"),
            new IntStringGameVariant(16, "HIGH")
        ));
    }
}
