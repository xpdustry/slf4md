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

import arc.util.ColorCodes;
import arc.util.Log;
import arc.util.Log.LogLevel;
import java.io.PrintWriter;
import java.io.Serial;
import java.io.StringWriter;
import java.util.Arrays;
import mindustry.net.Administration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

public final class SimpleLogger extends AbstractLogger {

    @Serial
    private static final long serialVersionUID = 3476499937056865545L;

    private static final Object WRITE_LOCK = new Object();
    private static final Administration.Config ENABLE_TRACE =
            new Administration.Config("loggerTrace", "Enable trace logging when debug is enabled.", false);
    private static final Administration.Config ENABLE_PLUGIN =
            new Administration.Config("loggerDisplayPlugin", "Prepends the owning plugin name of a logger", true);
    private static final Administration.Config ENABLE_CLASS =
            new Administration.Config("loggerDisplayClass", "Prepends the owning class of a logger.", false);

    private final @Nullable String plugin;

    public SimpleLogger(final String name, final @Nullable String plugin) {
        this.name = name;
        this.plugin = plugin;
    }

    @Override
    public boolean isTraceEnabled() {
        return this.isArcLogLevelAtLeast(LogLevel.debug) && ENABLE_TRACE.bool();
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return this.isArcLogLevelAtLeast(LogLevel.debug) && ENABLE_TRACE.bool();
    }

    @Override
    public boolean isDebugEnabled() {
        return this.isArcLogLevelAtLeast(LogLevel.debug);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return this.isArcLogLevelAtLeast(LogLevel.debug);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.isArcLogLevelAtLeast(LogLevel.info);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return this.isArcLogLevelAtLeast(LogLevel.info);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.isArcLogLevelAtLeast(LogLevel.warn);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return this.isArcLogLevelAtLeast(LogLevel.warn);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.isArcLogLevelAtLeast(LogLevel.err);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return this.isArcLogLevelAtLeast(LogLevel.err);
    }

    @Override
    protected @Nullable String getFullyQualifiedCallerName() {
        return null;
    }

    @Override
    protected void handleNormalizedLoggingCall(
            final Level level,
            final @Nullable Marker marker,
            final String messagePattern,
            @Nullable Object @Nullable [] arguments,
            @Nullable Throwable throwable) {
        final var builder = new StringBuilder();

        if (!this.name.equals(ROOT_LOGGER_NAME)) {
            if (this.plugin != null && ENABLE_PLUGIN.isBool() && ENABLE_PLUGIN.bool()) {
                builder.append(this.getColorCode(level))
                        .append('[')
                        .append(this.plugin)
                        .append(']')
                        .append(ColorCodes.reset)
                        .append(' ');
            }
            if (ENABLE_CLASS.isBool() && ENABLE_CLASS.bool()) {
                builder.append(this.getColorCode(level))
                        .append('[')
                        .append(this.name)
                        .append(']')
                        .append(ColorCodes.reset)
                        .append(' ');
            }
        }

        if (level == Level.ERROR) {
            builder.append(this.getColorCode(level));
        }

        if (throwable == null
                && arguments != null
                && arguments.length != 0
                && arguments[arguments.length - 1] instanceof final Throwable last) {
            throwable = last;
            arguments = arguments.length == 1 ? null : Arrays.copyOf(arguments, arguments.length - 1);
        }

        builder.append(MessageFormatter.basicArrayFormat(messagePattern.replace("{}", "&fb&lb{}&fr"), arguments));

        if (throwable != null) {
            final var sw = new StringWriter();
            final var pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            builder.append(": ").append(sw);
        }

        synchronized (WRITE_LOCK) {
            Log.log(this.getArcLogLevel(level), builder.toString());
        }
    }

    @SuppressWarnings("EnumOrdinal")
    private boolean isArcLogLevelAtLeast(final LogLevel level) {
        return level != LogLevel.none && Log.level.ordinal() <= level.ordinal();
    }

    private LogLevel getArcLogLevel(final Level level) {
        return switch (level) {
            case TRACE, DEBUG -> LogLevel.debug;
            case INFO -> LogLevel.info;
            case WARN -> LogLevel.warn;
            case ERROR -> LogLevel.err;
        };
    }

    private String getColorCode(final Level level) {
        return switch (level) {
            case DEBUG, TRACE -> ColorCodes.lightCyan + ColorCodes.bold;
            case INFO -> ColorCodes.lightBlue + ColorCodes.bold;
            case WARN -> ColorCodes.lightYellow + ColorCodes.bold;
            case ERROR -> ColorCodes.lightRed + ColorCodes.bold;
        };
    }
}
