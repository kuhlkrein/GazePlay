package net.gazeplay.games.memory;

import javafx.scene.image.Image;
import net.gazeplay.IGameContext;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.utils.stats.Stats;
import net.gazeplay.games.math101.RoundDetails;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.testfx.framework.junit5.ApplicationExtension;
import javafx.geometry.Dimension2D;

import java.util.HashMap;
import java.util.Random;

import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
@RunWith(MockitoJUnitRunner.class)
class MemoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    IGameContext mockGameContext;

    @Mock
    Stats mockStats;

    @Mock
    Configuration mockConfig;

    @EnumSource(value = Memory.MemoryGameType.class, mode = EnumSource.Mode.EXCLUDE, names = {"CUSTOMIZED"})
    void shouldPickAndBuildRandomPictures(Memory.MemoryGameType gameType) {
        Memory memory = new Memory(gameType, mockGameContext, 2, 2, mockStats, true);
        when(mockConfig.getLanguage()).thenReturn("eng");

        Dimension2D mockDimension = new Dimension2D(20, 20);
        when(mockGameContext.getGamePanelDimensionProvider().getDimension2D()).thenReturn(mockDimension);
        when(mockGameContext.getConfiguration()).thenReturn(mockConfig);

        Random random = new Random();
        HashMap<Integer, Image> randomPictures = memory.selectionAleaImages();
        assert randomPictures.size() == 4;
    }

}
