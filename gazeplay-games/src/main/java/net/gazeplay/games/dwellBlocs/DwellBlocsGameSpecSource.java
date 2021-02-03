package net.gazeplay.games.dwellBlocs;

import net.gazeplay.GameCategories;
import net.gazeplay.GameSpec;
import net.gazeplay.GameSpecSource;
import net.gazeplay.GameSummary;
import net.gazeplay.commons.gamevariants.generators.SquareDimensionVariantGenerator;

public class DwellBlocsGameSpecSource implements GameSpecSource {
    @Override
    public GameSpec getGameSpec() {
        return new GameSpec(
            GameSummary.builder().nameCode("DwellBlocks").gameThumbnail("data/Thumbnails/block.png").category(GameCategories.Category.ACTION_REACTION).build(),
            new SquareDimensionVariantGenerator(2, 9), new DwellBlocsGameLauncher());
    }
}
