/*
 * Copyright or © or Copr. QuartzLib contributors (2015 - 2020)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.quartzlib.tools;

import fr.zcraft.quartzlib.core.QuartzLib;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class PluginLogger {
    private static Thread mainThread;
    private static HashMap<Thread, PluginThreadLogger> loggers;

    private PluginLogger() {
    }

    public static void init() {
        mainThread = Thread.currentThread();
        loggers = new HashMap<>();
    }

    public static void log(Level level, String message, Throwable ex) {
        getLogger().log(level, message, ex);
    }

    public static void log(Level level, String message, Object... args) {
        getLogger().log(level, message, args);
    }

    public static void log(Level level, String message, Throwable ex, Object... args) {
        log(level, message, args);
        log(level, "Exception : ", ex);
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public static void warning(String message, Object... args) {
        log(Level.WARNING, message, args);
    }

    public static void warning(String message, Throwable ex) {
        log(Level.WARNING, message, ex);
    }

    public static void warning(String message, Throwable ex, Object... args) {
        log(Level.WARNING, message, ex, args);
    }

    public static void error(String message) {
        log(Level.SEVERE, message);
    }

    public static void error(String message, Throwable ex) {
        log(Level.SEVERE, message, ex);
    }

    public static void error(String message, Throwable ex, Object... args) {
        log(Level.SEVERE, message, ex, args);
    }

    public static void error(String message, Object... args) {
        log(Level.SEVERE, message, args);
    }

    private static Logger getLogger() {
        Thread currentThread = Thread.currentThread();
        if (currentThread.equals(mainThread)) {
            return QuartzLib.getPlugin().getLogger();
        }
        return getLogger(currentThread);
    }

    private static Logger getLogger(Thread thread) {
        PluginThreadLogger logger = loggers.get(thread);
        if (logger == null) {
            logger = new PluginThreadLogger(thread);
            loggers.put(thread, logger);
        }
        return logger;
    }

    private static class PluginThreadLogger extends Logger {
        private final String loggerName;

        public PluginThreadLogger(Thread thread) {
            super(QuartzLib.getPlugin().getClass().getCanonicalName(), null);
            setParent(QuartzLib.getPlugin().getLogger());
            setLevel(Level.ALL);
            loggerName = "[" + thread.getName() + "] ";
        }

        @Override
        public void log(LogRecord logRecord) {
            logRecord.setMessage(loggerName + logRecord.getMessage());
            super.log(logRecord);
        }
    }
}
