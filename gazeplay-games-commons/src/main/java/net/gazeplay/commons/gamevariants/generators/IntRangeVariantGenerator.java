package net.gazeplay.commons.gamevariants.generators;

import lombok.Data;
import lombok.Getter;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.gamevariants.IntGameVariant;

import java.util.LinkedHashSet;
import java.util.Set;

public class IntRangeVariantGenerator implements IGameVariantGenerator {

    public IntRangeVariantGenerator(String variantChooseText, int min, int max){
        this.variantChooseText = variantChooseText;
        this.min = min;
        this.max = max;
    }

    @Getter
    private final String variantChooseText;

    @Getter
    private final int min;

    @Getter
    private final int max;

    @Override
    public Set<IGameVariant> getVariants() {
        final LinkedHashSet<IGameVariant> result = new LinkedHashSet<>();
        for (int i = min; i <= max; i++) {
            result.add(new IntGameVariant(i));
        }
        return result;
    }
}
