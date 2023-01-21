package com.ravingarinc.manhunt.api.util;

import com.ravingarinc.manhunt.RavinPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Static logger class used for shortcut logging.
 */
public class I {
    private static RavinPlugin plugin;

    public I() {
    }

    public static void load(final RavinPlugin plugin) {
        I.plugin = plugin;
    }

    public static void log(final Level level, final String message, final Object... replacements) {
        plugin.log(level, message, null, replacements);
    }

    public static void log(final Level level, final String message, @Nullable final Throwable throwable, final Object... replacements) {
        plugin.log(level, message, throwable, replacements);
    }

    public static void log(final Level level, final String message, final Throwable throwable) {
        plugin.log(level, message, throwable);
    }

    public static void logIfDebug(final Supplier<String> message, final Object... replacements) {
        plugin.logIfDebug(message, replacements);
    }

    public static void runIfDebug(final Runnable runnable) {
        plugin.runIfDebug(runnable);
    }
}
