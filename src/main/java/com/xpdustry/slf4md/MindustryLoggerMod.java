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

import arc.util.Log;
import mindustry.mod.Mod;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public final class MindustryLoggerMod extends Mod {

    static {
        // Class loader trickery to use the ModClassLoader instead of the root
        final ClassLoader temp = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(MindustryLoggerMod.class.getClassLoader());
        try {
            if (!(LoggerFactory.getILoggerFactory() instanceof MindustryLoggerFactory)) {
                Log.err(
                        "The slf4j Logger factory isn't provided by SLF4MD (got @ instead of SimpleLoggerFactory). Make sure another mod/plugin doesn't set it's own logging implementation or that it's logging implementation is relocated correctly.",
                        LoggerFactory.getILoggerFactory().getClass().getName());
            } else {
                // Redirect JUL to SLF4J
                SLF4JBridgeHandler.removeHandlersForRootLogger();
                SLF4JBridgeHandler.install();
                LoggerFactory.getLogger(MindustryLoggerMod.class).info("Initialized SLF4MD");
            }
        } catch (final Exception e) {
            Log.err("Failed to initialize SLF4MD logger", e);
        } finally {
            // Restore the class loader
            Thread.currentThread().setContextClassLoader(temp);
        }
    }
}
