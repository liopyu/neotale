package net.liopyu.neotale.util;

import net.liopyu.neotale.NeoTale;

import java.util.Arrays;

public class NeoTaleHelperClass {
    public static void logInfo(Object... message) {
        NeoTale.LOGGER.atInfo().log(Arrays.toString(message));
    }

    public static void logWarning(Object... message) {
        NeoTale.LOGGER.atWarning().log(Arrays.toString(message));
    }

    public static void logError(Object... message) {
        NeoTale.LOGGER.atSevere().log(Arrays.toString(message));
    }
}
