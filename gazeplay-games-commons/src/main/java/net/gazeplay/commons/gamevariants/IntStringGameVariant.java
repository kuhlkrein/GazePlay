package net.gazeplay.commons.gamevariants;

import lombok.Data;
import net.gazeplay.commons.gamevariants.IGameVariant;
import net.gazeplay.commons.ui.Translator;

@Data
public class IntStringGameVariant implements IGameVariant {

    private final int number;
    private final String stringValue;

    @Override
    public String getLabel(final Translator translator) {
        return number + " " + stringValue;
    }

    @Override
    public String toString() { return "IntStringGameVariant:" + number  + ":" + stringValue;
    }
}
