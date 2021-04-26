/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.viaversion.viabackwards.fabric.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LoggerWrapper extends Logger {
    private final org.apache.logging.log4j.Logger base;

    public LoggerWrapper(org.apache.logging.log4j.Logger logger) {
        super("logger", null);
        this.base = logger;
    }

    public void log(LogRecord record) {
        this.log(record.getLevel(), record.getMessage());
    }

    public void log(Level level, String msg) {
        if (level == Level.FINE) {
            this.base.debug(msg);
        } else if (level == Level.WARNING) {
            this.base.warn(msg);
        } else if (level == Level.SEVERE) {
            this.base.error(msg);
        } else if (level == Level.INFO) {
            this.base.info(msg);
        } else {
            this.base.trace(msg);
        }

    }

    public void log(Level level, String msg, Object param1) {
        if (level == Level.FINE) {
            this.base.debug(msg, param1);
        } else if (level == Level.WARNING) {
            this.base.warn(msg, param1);
        } else if (level == Level.SEVERE) {
            this.base.error(msg, param1);
        } else if (level == Level.INFO) {
            this.base.info(msg, param1);
        } else {
            this.base.trace(msg, param1);
        }

    }

    public void log(Level level, String msg, Object[] params) {
        log(level, MessageFormat.format(msg, params));
    }

    public void log(Level level, String msg, Throwable params) {
        if (level == Level.FINE) {
            this.base.debug(msg, params);
        } else if (level == Level.WARNING) {
            this.base.warn(msg, params);
        } else if (level == Level.SEVERE) {
            this.base.error(msg, params);
        } else if (level == Level.INFO) {
            this.base.info(msg, params);
        } else {
            this.base.trace(msg, params);
        }

    }
}
