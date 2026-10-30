/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Small thread-safe cache for connection independent conversions, shared between all connections.
 * Once full, it is cleared as a whole, so it never holds more than {@code maxSize} entries.
 *
 * @param <K> key type, must not be modified after being put into the cache
 * @param <V> value type, must not be modified after being put into the cache
 */
public final class ConversionCache<K, V> {

    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final int maxSize;

    public ConversionCache(final int maxSize) {
        this.maxSize = maxSize;
    }

    public @Nullable V get(final K key) {
        return cache.get(key);
    }

    public void put(final K key, final V value) {
        if (cache.size() >= maxSize) {
            cache.clear();
        }
        cache.put(key, value);
    }

    public int size() {
        return cache.size();
    }
}
