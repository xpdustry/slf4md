/*
 * This file is part of SLF4MD. A basic SLF4J implementation for Mindustry.
 *
 * MIT License
 *
 * Copyright (c) 2024-2026 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xpdustry.slf4md;

import arc.Core;
import arc.struct.ObjectMap;
import arc.struct.StringMap;
import arc.util.CommandHandler;
import arc.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mindustry.Vars;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.net.Administration;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.event.Level;

public final class MindustryLoggerMod extends Mod {

    private static final Supplier<Boolean> showClassName = MindustryUtils.registerSafeSettingEntry(
            "logShowClassName",
            "Whether the class name of a logger should be added to the log statement.",
            false,
            Boolean::parseBoolean);

    private static final Supplier<Boolean> showModName = MindustryUtils.registerSafeSettingEntry(
            "logShowModName",
            "Whether the mod name of a logger should be added to the log statement.",
            true,
            Boolean::parseBoolean);

    private static final Supplier<Boolean> traceEnabled = MindustryUtils.registerSafeSettingEntry(
            "trace",
            "Whether trace logging is enabled. Trace shows every single detail occurring in this server. Enabling tracing enables debug too.",
            true,
            Boolean::parseBoolean,
            () -> {
                if (isTraceEnabled()) {
                    Administration.Config.debug.set(true);
                }
            });

    private static final Map<String, Level> logLevels = new ConcurrentHashMap<>();

    static {
        // Do the thing!
        MindustryLoggerMod.loadLogLevels();

        // Class loader trickery to use the ModClassLoader instead of the root
        final ClassLoader rootClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(MindustryLoggerMod.class.getClassLoader());
        try {
            final ILoggerFactory factory = LoggerFactory.getILoggerFactory();
            if (!(factory instanceof MindustryLoggerFactory)) {
                Log.err("The slf4j Logger factory isn't provided by SLF4MD.");
                Log.err(
                        "Got @ instead of @.",
                        factory.getClass().getName(),
                        MindustryLoggerFactory.class.getSimpleName());
                Log.err("Make sure that other mods/plugins do not make their own slf4j implementation global.");
            } else {
                final Logger logger = factory.getLogger(MindustryLoggerMod.class.getName());
                logger.info("Initialized SLF4MD");
                try {
                    // Redirect JUL to SLF4J
                    SLF4JBridgeHandler.removeHandlersForRootLogger();
                    SLF4JBridgeHandler.install();
                    logger.debug("Successfully redirected java logging to SLF4MD");
                } catch (final NoClassDefFoundError e) {
                    logger.warn("Java logging classes are missing, skipping redirection", e);
                } catch (final Throwable e) {
                    logger.error("Failed to redirect java logging to SLF4MD", e);
                }
            }
        } catch (final Throwable e) {
            Log.err("Failed to initialize SLF4MD logger", e);
        } finally {
            // Restore the class loader
            Thread.currentThread().setContextClassLoader(rootClassLoader);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    public static boolean isShowClassName() {
        return MindustryLoggerMod.showClassName.get();
    }

    public static boolean isShowModName() {
        return MindustryLoggerMod.showModName.get();
    }

    public static boolean isTraceEnabled() {
        return MindustryLoggerMod.traceEnabled.get();
    }

    private static @Nullable Level getRootLoggerLevel() {
        switch (Log.level) {
            case debug:
                return MindustryLoggerMod.isTraceEnabled() ? Level.TRACE : Level.DEBUG;
            case warn:
                return Level.WARN;
            case err:
                return Level.ERROR;
            case none:
                return null;
            case info:
            default:
                return Level.INFO;
        }
    }

    public static @Nullable Level getLoggerLevel(String name, final Mods.@Nullable ModMeta meta) {
        name = name.toLowerCase(Locale.ROOT);
        if (name.equals(Logger.ROOT_LOGGER_NAME.toLowerCase(Locale.ROOT))) {
            return MindustryLoggerMod.getRootLoggerLevel();
        }
        Level level = MindustryLoggerMod.logLevels.get(name);
        if (level == null && meta != null) {
            level = MindustryLoggerMod.logLevels.get(meta.displayName.toLowerCase(Locale.ROOT));
            if (level == null) {
                level = MindustryLoggerMod.logLevels.get(meta.name.toLowerCase(Locale.ROOT));
            }
        }
        return level;
    }

    public static void setLoggerLevel(String name, final @Nullable Level level) {
        name = name.toLowerCase(Locale.ROOT);
        if (level == null) {
            MindustryLoggerMod.logLevels.remove(name);
        } else {
            MindustryLoggerMod.logLevels.put(name, level);
        }
        MindustryLoggerMod.saveLogLevels();
    }

    public static boolean hasAtLeastLevel(final String name, final Mods.@Nullable ModMeta meta, final Level level) {
        Level configuredLevel = MindustryLoggerMod.getLoggerLevel(name, meta);
        if (configuredLevel == null) {
            configuredLevel = MindustryLoggerMod.getRootLoggerLevel();
        }
        if (configuredLevel == null) {
            return false;
        }
        return level.toInt() >= configuredLevel.toInt();
    }

    private static void loadLogLevels() {
        final StringMap rawLogLevels =
                Core.settings.getJson("slf4md-log-levels", StringMap.class, String.class, StringMap::new);
        for (final ObjectMap.Entry<String, String> entry : rawLogLevels.entries()) {
            final Level level;
            try {
                level = Level.valueOf(entry.value);
            } catch (final IllegalArgumentException ignored) {
                continue;
            }
            MindustryLoggerMod.logLevels.put(entry.key, level);
        }
    }

    private static void saveLogLevels() {
        final StringMap rawLogLevels = new StringMap();
        for (final Map.Entry<String, Level> entry : MindustryLoggerMod.logLevels.entrySet()) {
            rawLogLevels.put(entry.getKey(), entry.getValue().toString());
        }
        Core.settings.putJson("slf4md-log-levels", String.class, rawLogLevels);
    }

    @Override
    public void registerServerCommands(final CommandHandler handler) {
        handler.register("log-level-set", "<name> <level|default>", "Set the log level of a SLF4MD logger.", args -> {
            final String name = args[0];
            final String levelRaw = args[1];
            final Level level;
            if (levelRaw.equalsIgnoreCase("default")) {
                level = null;
            } else {
                try {
                    level = Level.valueOf(levelRaw.toUpperCase(Locale.ROOT));
                } catch (final IllegalArgumentException e) {
                    log.atError()
                            .setMessage("This is not a valid level, expected ({}), got {}")
                            .addArgument(() -> Stream.concat(
                                            Stream.of("default"),
                                            Stream.of(Level.values())
                                                    .map(l -> l.name().toLowerCase(Locale.ROOT)))
                                    .collect(Collectors.joining("|")))
                            .addArgument(levelRaw)
                            .log();
                    return;
                }
            }
            MindustryLoggerMod.setLoggerLevel(args[0], level);
            log.info(
                    "Set log level of {} to {}",
                    name,
                    level == null ? "default" : level.name().toLowerCase(Locale.ROOT));
        });

        handler.register("log-level-list", "List the loggers with an explicit log level.", ignored -> {
            if (MindustryLoggerMod.logLevels.isEmpty()) {
                log.info("No explicit log levels have been set.");
                return;
            }

            final List<Map.Entry<String, Level>> entries = new ArrayList<>(MindustryLoggerMod.logLevels.entrySet());
            entries.sort(Map.Entry.comparingByKey());

            final StringBuilder builder = new StringBuilder();
            for (final Map.Entry<String, Level> entry : entries) {
                builder.append("\n");

                final String name = entry.getKey();
                final Level level = entry.getValue();

                builder.append("> ").append(name);
                Mods.LoadedMod mod = Vars.mods.list().find(m -> m.meta.name.equalsIgnoreCase(name));
                if (mod == null) {
                    mod = Vars.mods.list().find(m -> m.meta.displayName.equalsIgnoreCase(name));
                }
                if (mod != null) {
                    builder.append(" (mod)");
                }
                builder.append(": ").append(level.name().toLowerCase(Locale.ROOT));
            }

            log.info(builder.toString());
        });
    }
}
