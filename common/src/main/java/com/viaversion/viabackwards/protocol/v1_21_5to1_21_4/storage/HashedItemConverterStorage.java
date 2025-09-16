/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.codec.CodecContext;
import com.viaversion.viaversion.api.minecraft.codec.CodecContext.RegistryAccess;
import com.viaversion.viaversion.api.minecraft.codec.hash.Hasher;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.codec.CodecRegistryContext;
import com.viaversion.viaversion.codec.hash.HashFunction;
import com.viaversion.viaversion.codec.hash.HashOps;
import java.util.ArrayList;
import java.util.List;

public class HashedItemConverterStorage implements StorableObject {

    private final List<String> enchantments = new ArrayList<>();
    private final Hasher hasher;

    public HashedItemConverterStorage(final Protocol<?, ?, ?, ?> protocol) {
        final RegistryAccess registryAccess = RegistryAccess.of(this.enchantments, protocol.getMappingData());
        final CodecContext context = new CodecRegistryContext(protocol, registryAccess, false);
        this.hasher = new HashOps(context, HashFunction.CRC32C);
    }

    public void setEnchantments(final List<String> enchantments) {
        this.enchantments.clear();
        this.enchantments.addAll(enchantments);
    }

    public Hasher hasher() {
        return hasher; // reusable
    }
}
