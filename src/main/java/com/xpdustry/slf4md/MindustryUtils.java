// SPDX-License-Identifier: MIT
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
