package net.gazeplay.commons.gamevariants.generators;

import lombok.Data;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.gamevariants.IntGameVariant;
import net.gazeplay.commons.gamevariants.IntStringGameVariant;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public class IntRangeAndPositionVariantGenerator extends IntRangeVariantGenerator {

    public IntRangeAndPositionVariantGenerator(String variantChooseText, int min, int max){
        super(variantChooseText,min,max);
    }


    @Override
    public Set<IGameVariant> getVariants() {

        final LinkedHashSet<IGameVariant> result = new LinkedHashSet<>();
        for (int i = this.getMin(); i <= this.getMax(); i++) {
            for ( String position : IntStringGameVariant.positions)
            result.add(new IntStringGameVariant(i, position));
        }
        return result;
    }
}
