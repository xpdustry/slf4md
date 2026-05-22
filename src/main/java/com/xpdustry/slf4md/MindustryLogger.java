// SPDX-License-Identifier: MIT
package com.xpdustry.slf4md;

import arc.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import mindustry.Vars;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;
import org.slf4j.helpers.MessageFormatter;

public final class MindustryLogger extends AbstractLogger {

    private static final long serialVersionUID = 3476499937056865545L;

    private final @Nullable String mod;

    MindustryLogger(final String name, final @Nullable String mod) {
        this.name = name;
        this.mod = mod;
    }

    @Override
    public boolean isTraceEnabled() {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.TRACE);
    }

    @Override
    public boolean isTraceEnabled(final Marker marker) {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.TRACE);
    }

    @Override
    public boolean isDebugEnabled() {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.DEBUG);
    }

    @Override
    public boolean isDebugEnabled(final Marker marker) {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.DEBUG);
    }

    @Override
    public boolean isInfoEnabled() {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.INFO);
    }

    @Override
    public boolean isInfoEnabled(final Marker marker) {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.WARN);
    }

    @Override
    public boolean isWarnEnabled(final Marker marker) {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.WARN);
    }

    @Override
    public boolean isErrorEnabled() {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.ERROR);
    }

    @Override
    public boolean isErrorEnabled(final Marker marker) {
        return MindustryLoggerMod.hasAtLeastLevel(this.name, Level.ERROR);
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
        final StringBuilder builder = new StringBuilder();

        if (!this.name.equals(Logger.ROOT_LOGGER_NAME)) {
            if (this.mod != null && MindustryLoggerMod.isShowModName()) {
                builder.append(this.getColorCode(level))
                        .append('[')
                        .append(this.mod)
                        .append("]&fr ");
            }
            if (MindustryLoggerMod.isShowClassName()) {
                builder.append(this.getColorCode(level))
                        .append('[')
                        .append(this.name)
                        .append("]&fr ");
            }
        }

        if (level == Level.ERROR) {
            builder.append(this.getColorCode(level));
        }

        if (throwable == null
                && arguments != null
                && arguments.length != 0
                && arguments[arguments.length - 1] instanceof Throwable) {
            throwable = (Throwable) arguments[arguments.length - 1];
            arguments = arguments.length == 1 ? null : Arrays.copyOf(arguments, arguments.length - 1);
        }

        builder.append(MessageFormatter.basicArrayFormat(messagePattern.replace("{}", "&fb&lb{}&fr"), arguments));

        if (throwable != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            builder.append(": ").append(sw);
        }

        final String message = Vars.headless ? builder.toString() : Log.removeColors(builder.toString());
        Log.log(MindustryLogger.fromSlf4jToArcLevel(level), message);
    }

    private String getColorCode(final Level level) {
        switch (level) {
            case TRACE:
            case DEBUG:
                return "&lc&fb";
            case WARN:
                return "&ly&fb";
            case ERROR:
                return "&lr&fb";
            case INFO:
            default:
                return "&lb&fb";
        }
    }

    public static Log.LogLevel fromSlf4jToArcLevel(final Level level) {
        switch (level) {
            case TRACE:
            case DEBUG:
                return Log.LogLevel.debug;
            case WARN:
                return Log.LogLevel.warn;
            case ERROR:
                return Log.LogLevel.err;
            case INFO:
            default:
                return Log.LogLevel.info;
        }
    }
}
