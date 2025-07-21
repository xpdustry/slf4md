/*
 * This file is part of SLF4MD. A basic SLF4J implementation for Mindustry.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
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

import arc.util.serialization.Json;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mindustry.mod.Mod;
import mindustry.mod.ModClassLoader;
import mindustry.mod.Mods;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public final class MindustryLoggerFactory implements ILoggerFactory {

    private static final String[] LOGGING_PACKAGES = {"org.slf4j", "java.util.logging", "sun.util.logging"};
    private static final String[] MOD_METADATA_NAMES = {"mod.json", "mod.hjson", "plugin.json", "plugin.hjson"};

    private final Map<String, MindustryLogger> loggers = new ConcurrentHashMap<>();

    {
        this.loggers.put(Logger.ROOT_LOGGER_NAME, new MindustryLogger(Logger.ROOT_LOGGER_NAME, null));
    }

    @Override
    public Logger getLogger(final String name) {
        if (this.loggers.containsKey(name)) {
            return this.loggers.get(name);
        }

        Class<?> caller;
        boolean cache = true;

        try {
            caller = Class.forName(name);
        } catch (final ClassNotFoundException ignored1) {
            final String candidate = this.tryFindCaller(Thread.currentThread().getStackTrace());
            if (candidate == null) {
                return new MindustryLogger(name, null);
            }
            try {
                caller = Class.forName(candidate);
                cache = false;
            } catch (final ClassNotFoundException ignored2) {
                return new MindustryLogger(name, null);
            }
        }

        if (Mod.class.isAssignableFrom(caller)) {
            final String display = this.getModDisplayName(caller.getClassLoader());
            if (display == null) {
                return new MindustryLogger(name, null);
            }
            // Mod loggers are found on the first lookup, thus if the cache flag is false,
            // it means a custom logger has been created inside the mod class
            final MindustryLogger logger =
                    cache ? new MindustryLogger(display, display) : new MindustryLogger(name, display);
            if (cache) {
                this.loggers.put(name, logger);
            }
            return logger;
        }

        ClassLoader loader = caller.getClassLoader();
        String display = null;
        while (loader != null) {
            if (loader.getParent() instanceof ModClassLoader) {
                display = this.getModDisplayName(loader);
                break;
            }
            loader = loader.getParent();
        }

        final MindustryLogger logger = new MindustryLogger(name, display);
        if (cache) {
            this.loggers.put(name, logger);
        }
        return logger;
    }

    private @Nullable String getModDisplayName(final ClassLoader loader) {
        InputStream resource = null;
        for (final String name : MOD_METADATA_NAMES) {
            resource = loader.getResourceAsStream(name);
            if (resource != null) {
                break;
            }
        }
        if (resource == null) {
            return null;
        }
        try (final InputStream input = resource) {
            final Mods.ModMeta meta = new Json().fromJson(Mods.ModMeta.class, input);
            meta.cleanup();
            return meta.displayName;
        } catch (final Exception e) {
            return null;
        }
    }

    private @Nullable String tryFindCaller(final StackTraceElement[] stacktrace) {
        loop:
        for (int i = 0; i <= stacktrace.length; i++) {
            // 0: stacktrace call, 1: DistributorLoggerFactory#getLogger, 2: LoggerFactory#getLogger
            if (i < 3) {
                continue;
            }
            final String name = stacktrace[i].getClassName();
            // Skip the logger wrappers
            for (final String pkg : LOGGING_PACKAGES) {
                if (name.startsWith(pkg)) {
                    continue loop;
                }
            }
            return name;
        }
        return null;
    }
}
