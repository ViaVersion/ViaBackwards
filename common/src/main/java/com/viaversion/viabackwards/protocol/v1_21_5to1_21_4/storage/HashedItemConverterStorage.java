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
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.libs.mcstructs.itemcomponents.ItemComponentRegistry;
import com.viaversion.viaversion.rewriter.item.ItemDataComponentConverter;
import com.viaversion.viaversion.rewriter.item.ItemDataComponentConverter.RegistryAccess;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.ArrayList;
import java.util.List;

public class HashedItemConverterStorage implements StorableObject {

    private final List<String> enchantments = new ArrayList<>();
    private final ItemDataComponentConverter itemComponentConverter;

    public HashedItemConverterStorage(final MappingData mappingData) {
        final RegistryAccess registryAccess = RegistryAccess.of(this.enchantments, ItemComponentRegistry.V1_21_5.getRegistries(), mappingData);
        this.itemComponentConverter = new ItemDataComponentConverter(SerializerVersion.V1_21_5, registryAccess);
    }

    public void setEnchantments(final List<String> enchantments) {
        this.enchantments.clear();
        this.enchantments.addAll(enchantments);
    }

    public ItemDataComponentConverter converter() {
        return itemComponentConverter;
    }
}
