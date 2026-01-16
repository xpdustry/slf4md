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

import arc.files.Fi;
import arc.struct.ObjectMap;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.serialization.Jval;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.Vars;
import mindustry.mod.Mod;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.slf4j.event.Level;

public final class MindustryLoggerMod extends Mod {

    private static boolean showClassName = false;
    private static boolean showModName = true;
    private static boolean traceEnabled = false;
    private static final Map<String, Level> levels = new ConcurrentHashMap<>();

    static {
        // Do the thing!
        MindustryLoggerMod.load();

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

    public static boolean isShowClassName() {
        return MindustryLoggerMod.showClassName;
    }

    public static void setShowClassName(final boolean showClassName) {
        MindustryLoggerMod.showClassName = showClassName;
        MindustryLoggerMod.save();
    }

    public static boolean isShowModName() {
        return MindustryLoggerMod.showModName;
    }

    public static void setShowModName(boolean showModName) {
        MindustryLoggerMod.showModName = showModName;
        MindustryLoggerMod.save();
    }

    public static boolean isTraceEnabled() {
        return MindustryLoggerMod.traceEnabled;
    }

    public static void setTraceEnabled(final boolean traceEnabled) {
        MindustryLoggerMod.traceEnabled = traceEnabled;
        MindustryLoggerMod.save();
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

    public static @Nullable Level getLoggerLevel(final String logger) {
        final String name = logger.toLowerCase(Locale.ROOT);
        if (name.equals(Logger.ROOT_LOGGER_NAME.toLowerCase(Locale.ROOT))) {
            return MindustryLoggerMod.getRootLoggerLevel();
        } else {
            return MindustryLoggerMod.levels.get(name);
        }
    }

    public static void setLoggerLevel(final String logger, final @Nullable Level level) {
        final String name = logger.toLowerCase(Locale.ROOT);
        if (level == null) {
            MindustryLoggerMod.levels.remove(name);
        } else {
            MindustryLoggerMod.levels.put(name, level);
        }
        MindustryLoggerMod.save();
    }

    public static boolean hasAtLeastLevel(final String logger, final Level level) {
        Level configuredLevel = MindustryLoggerMod.getLoggerLevel(logger);
        if (configuredLevel == null) {
            configuredLevel = MindustryLoggerMod.getRootLoggerLevel();
        }
        if (configuredLevel == null) {
            return false;
        }
        return level.toInt() >= configuredLevel.toInt();
    }

    private static void save() {
        final Fi configFile = Vars.modDirectory.child("slf4md").child("config.json");
        final Jval object = Jval.newObject()
                .put("show-class-name", MindustryLoggerMod.showClassName)
                .put("show-mod-name", MindustryLoggerMod.showModName)
                .put("trace-enabled", MindustryLoggerMod.traceEnabled);
        final Jval levels = Jval.newObject();
        for (final Map.Entry<String, Level> entry : MindustryLoggerMod.levels.entrySet()) {
            levels.put(entry.getKey(), entry.getValue().toString());
        }
        try (final Writer writer = configFile.writer(false)) {
            object.put("log-levels", levels).writeTo(writer, Jval.Jformat.formatted);
        } catch (final Exception e) {
            Log.err("Failed to save settings to disk", e);
        }
    }

    private static void load() {
        final Fi configFile = Vars.modDirectory.child("slf4md").child("config.json");
        if (!configFile.exists()) {
            return;
        }
        final Jval object;
        try (final Reader reader = configFile.reader()) {
            object = Jval.read(reader);
        } catch (final Exception e) {
            Log.err("[SLF4MD] Failed to read SLF4MD settings", e);
            return;
        }
        final Jval showClassName = object.get("show-class-name");
        if (showClassName != null && showClassName.isBoolean()) {
            MindustryLoggerMod.showClassName = showClassName.asBool();
        }
        final Jval showModName = object.get("show-mod-name");
        if (showModName != null && showModName.isBoolean()) {
            MindustryLoggerMod.showModName = showModName.asBool();
        }
        final Jval traceEnabled = object.get("trace-enabled");
        if (traceEnabled != null && traceEnabled.isBoolean()) {
            MindustryLoggerMod.traceEnabled = traceEnabled.asBool();
        }
        final Jval levels = object.get("log-levels");
        if (levels != null && levels.isObject()) {
            for (final ObjectMap.Entry<String, Jval> entry : levels.asObject()) {
                if (!entry.value.isString()) {
                    continue;
                }
                final Level level;
                try {
                    level = Level.valueOf(entry.value.asString().toUpperCase(Locale.ROOT));
                } catch (final IllegalArgumentException e) {
                    Log.warn("[SLF4MD]: Invalid log level @ for @ in settings file.", entry.value, entry.key);
                    continue;
                }
                MindustryLoggerMod.levels.put(entry.key.toLowerCase(Locale.ROOT), level);
            }
        }
    }

    @Override
    public void registerServerCommands(final CommandHandler handler) {
        handler.register("slf4md", "[subcommand] [arg1] [arg2]", "SLF4MD management commands.", args -> {
            if (args.length == 0) {
                Log.info(">>> SLF4MD >>> Available SubCommands >>>");
                Log.info("> log-level <logger> [level|clear]");
                Log.info("Change or clear the log level of a specified logger.");
                Log.info("> log-level-list");
                Log.info("List the log levels you have explicitly set.");
                Log.info("> enable-trace [true|false]");
                Log.info("Toggle trace logging when debug is active.");
                Log.info("> show-mod-name [true|false]");
                Log.info("Toggle mod name display in log statements.");
                Log.info("> show-class-name [true|false]");
                Log.info("Toggle class name display in log statements.");
                return;
            }

            switch (args[0]) {
                case "log-level":
                    if (args.length == 1) {
                        Log.err("Usage: log-level <logger> [level|clear]");
                    } else if (args.length == 2) {
                        final Level level = MindustryLoggerMod.getLoggerLevel(args[1]);
                        if (level == null) {
                            Log.info("Logger @ has no explicit level set (inherits from root).", args[1]);
                        } else {
                            Log.info("Logger @ has level @.", args[1], level);
                        }
                    } else {
                        if (args[1].equalsIgnoreCase(Logger.ROOT_LOGGER_NAME)) {
                            Log.err("Cannot modify the root logger level. Use Mindustry's log level settings instead.");
                            return;
                        }
                        if (args[2].equalsIgnoreCase("clear")) {
                            MindustryLoggerMod.setLoggerLevel(args[1], null);
                            Log.info("Logger @ now has no explicit level set.", args[1]);
                        } else {
                            final Level level;
                            try {
                                level = Level.valueOf(args[2].toUpperCase(Locale.ROOT));
                            } catch (final IllegalArgumentException e) {
                                Log.err(
                                        "Invalid log level @, accepted values are @ or 'clear'.",
                                        args[2],
                                        Arrays.toString(Level.values()));
                                return;
                            }
                            MindustryLoggerMod.setLoggerLevel(args[1], level);
                            Log.info("Set log level of @ to @.", args[1], level);
                        }
                    }
                    break;

                case "log-level-list":
                    if (MindustryLoggerMod.levels.isEmpty()) {
                        Log.info("No custom log levels have been set.");
                    } else {
                        Log.info(">>> SLF4MD >>> Custom Log Levels >>>");
                        for (final Map.Entry<String, Level> entry : MindustryLoggerMod.levels.entrySet()) {
                            Log.info("@ -> @", entry.getKey(), entry.getValue());
                        }
                    }
                    break;

                case "enable-trace":
                    if (args.length == 1) {
                        Log.info(
                                "Trace logging is currently @.",
                                MindustryLoggerMod.isTraceEnabled() ? "enabled" : "disabled");
                    } else {
                        final String stringValue = args[1].toLowerCase(Locale.ROOT);
                        if (!stringValue.equals("true") && !stringValue.equals("false")) {
                            Log.err("Usage: enable-trace [true|false]");
                            return;
                        }
                        final boolean value = Boolean.parseBoolean(stringValue);
                        MindustryLoggerMod.setTraceEnabled(value);
                        Log.info("Trace logging is now @.", value ? "enabled" : "disabled");
                    }
                    break;

                case "show-mod-name":
                    if (args.length == 1) {
                        Log.info(
                                "Mod name display is currently @.",
                                MindustryLoggerMod.isShowModName() ? "enabled" : "disabled");
                    } else {
                        final String stringValue = args[1].toLowerCase(Locale.ROOT);
                        if (!stringValue.equals("true") && !stringValue.equals("false")) {
                            Log.err("Usage: show-mod-name [true|false]");
                            return;
                        }
                        final boolean value = Boolean.parseBoolean(stringValue);
                        MindustryLoggerMod.setShowModName(value);
                        Log.info("Mod name display is now @.", value ? "enabled" : "disabled");
                    }
                    break;

                case "show-class-name":
                    if (args.length == 1) {
                        Log.info(
                                "Class name display is currently @.",
                                MindustryLoggerMod.isShowClassName() ? "enabled" : "disabled");
                    } else {
                        final String stringValue = args[1].toLowerCase(Locale.ROOT);
                        if (!stringValue.equals("true") && !stringValue.equals("false")) {
                            Log.err("Usage: show-class-name [true|false]");
                            return;
                        }
                        final boolean value = Boolean.parseBoolean(stringValue);
                        MindustryLoggerMod.setShowClassName(value);
                        Log.info("Class name display is now @.", value ? "enabled" : "disabled");
                    }
                    break;

                default:
                    Log.err("Unknown subcommand: @. Run 'slf4md' without arguments for help.", args[0]);
                    break;
            }
        });
    }
}
