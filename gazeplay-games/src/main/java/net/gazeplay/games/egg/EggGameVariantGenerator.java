package net.gazeplay.games.egg;

import net.gazeplay.commons.gamevariants.generators.IntRangeAndPositionVariantGenerator;

public class EggGameVariantGenerator extends IntRangeAndPositionVariantGenerator {

    public EggGameVariantGenerator() {
        super("stepsToHatch", 2, 5);
    }

}
