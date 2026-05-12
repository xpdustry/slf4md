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

import java.util.function.Function;
import java.util.function.Supplier;
import mindustry.net.Administration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class MindustryUtils {

    private static final Logger log = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    private MindustryUtils() {}

    public static <T> Supplier<T> registerSafeSettingEntry(
            final String name, final String desc, final T def, final Function<String, T> parser) {
        return registerSafeSettingEntry(name, desc, def, parser, () -> {});
    }

    public static <T> Supplier<T> registerSafeSettingEntry(
            final String name,
            final String desc,
            final T def,
            final Function<String, T> parser,
            final Runnable onChange) {
        final Administration.Config entry =
                new Administration.Config(name, desc, def.toString(), "slf4md-" + name, onChange) {
                    @Override
                    public void set(final Object value) {
                        if (value instanceof String) {
                            final String string = (String) value;
                            try {
                                final T ignored = parser.apply(string);
                                super.set(string);
                            } catch (final Exception e) {
                                log.error(
                                        "The value '{}' for the '{}' config entry is not valid", string, this.name, e);
                            }
                        } else {
                            log.error(
                                    "The value '{}' for the '{}' config entry is not a string",
                                    value,
                                    this.name,
                                    new IllegalArgumentException());
                        }
                    }
                };
        return () -> {
            if (!entry.isString()) {
                return def;
            }
            try {
                return parser.apply(entry.string());
            } catch (final Exception ignored) {
                return def;
            }
        };
    }
}
