/*
 * This file is part of SLF4MD. A set of plugins providing various SLF4J implementations for Mindustry.
 *
 * MIT License
 *
 * Copyright (c) 2025 xpdustry
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
package com.xpdustry.slf4md.simple;

import arc.util.serialization.Json;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods;
import mindustry.mod.Plugin;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleLoggerFactory implements ILoggerFactory {

    private static final List<String> LOGGING_PACKAGES = List.of("org.slf4j", "java.util.logging", "sun.util.logging");

    private static final List<Class<?>> LOGGING_CLASSES = List.of(SimpleLoggerFactory.class, LoggerFactory.class);

    private final Map<String, SimpleLogger> loggers = new ConcurrentHashMap<>();

    {
        this.loggers.put(Logger.ROOT_LOGGER_NAME, new SimpleLogger(Logger.ROOT_LOGGER_NAME, null));
    }

    @Override
    public Logger getLogger(final String name) {
        if (this.loggers.containsKey(name)) {
            return this.loggers.get(name);
        }

        Class<?> caller;
        var cache = true;

        try {
            caller = Class.forName(name);
        } catch (final ClassNotFoundException ignored) {
            final var candidate = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(stream -> stream.map(StackWalker.StackFrame::getDeclaringClass)
                            .dropWhile(clazz -> LOGGING_CLASSES.stream().noneMatch(skip -> skip.isAssignableFrom(clazz))
                                    || LOGGING_PACKAGES.stream().anyMatch(clazz.getPackageName()::startsWith))
                            .findFirst());
            if (candidate.isEmpty()) {
                return new SimpleLogger(name, null);
            }
            cache = false;
            caller = candidate.get();
        }

        if (Plugin.class.isAssignableFrom(caller)) {
            final var display = getPluginDisplayName(caller.getClassLoader());
            if (display == null) {
                return new SimpleLogger(name, null);
            }
            // Plugin loggers are found on the first lookup, thus if the cache flag is false,
            // it means a custom logger has been created inside the plugin class
            final var logger = cache ? new SimpleLogger(display, display) : new SimpleLogger(name, display);
            if (cache) {
                this.loggers.put(name, logger);
            }
            return logger;
        }

        ClassLoader loader = caller.getClassLoader();
        String display = null;
        while (loader != null) {
            if (loader.getParent() instanceof ModClassLoader) {
                display = getPluginDisplayName(loader);
                break;
            }
            loader = loader.getParent();
        }

        final var logger = new SimpleLogger(name, display);
        if (cache) {
            this.loggers.put(name, logger);
        }
        return logger;
    }

    private @Nullable String getPluginDisplayName(final ClassLoader loader) {
        var resource = loader.getResourceAsStream("plugin.json");
        if (resource == null) {
            resource = loader.getResourceAsStream("plugin.hjson");
            if (resource == null) {
                return null;
            }
        }
        try (final var input = resource) {
            final var meta = new Json().fromJson(Mods.ModMeta.class, input);
            meta.cleanup();
            return meta.displayName;
        } catch (final Exception e) {
            return null;
        }
    }
}
