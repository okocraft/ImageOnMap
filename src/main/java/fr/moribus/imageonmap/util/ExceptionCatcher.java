package fr.moribus.imageonmap.util;

import fr.moribus.imageonmap.ImageOnMap;

import java.util.logging.Level;

public class ExceptionCatcher {

    public static void catchException(Thread thread, Throwable throwable) {
        ImageOnMap.getPlugin().getLogger().log(
                Level.SEVERE,
                "An exception occurred in the thread " + thread.getName(),
                throwable
        );
    }
}
