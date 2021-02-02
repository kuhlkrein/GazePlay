package net.gazeplay.commons.gamevariants.generators;

import lombok.Data;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.gamevariants.IntGameVariant;
import net.gazeplay.commons.gamevariants.IntStringGameVariant;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

@Data
public class IntRangeAndPositionVariantGenerator implements IGameVariantGenerator {

    private final String variantChooseText;

    private final int min;

    private final int max;

    @Override
    public Set<IGameVariant> getVariants() {
        String[] positions = new String[]{
            "TOP_LEFT",
            "TOP_CENTER",
            "TOP_RIGHT",
            "MIDDLE_LEFT",
            "MIDDLE_CENTER",
            "MIDDLE_RIGHT",
            "BOTTOM_LEFT",
            "BOTTOM_CENTER",
            "BOTTOM_RIGHT"};

        final LinkedHashSet<IGameVariant> result = new LinkedHashSet<>();
        for (int i = min; i <= max; i++) {
            for ( String position : positions)
            result.add(new IntStringGameVariant(i, position));
        }
        return result;
    }
}
