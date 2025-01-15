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
package com.viaversion.viabackwards.api.rewriters.text;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.TranslatableMappings;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.libs.gson.JsonObject;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NBTComponentRewriter<C extends ClientboundPacketType> extends com.viaversion.viaversion.rewriter.text.NBTComponentRewriter<C> implements TranslatableRewriter {

    private final Map<String, String> translatables;

    public NBTComponentRewriter(final BackwardsProtocol<C, ?, ?, ?> protocol) {
        super(protocol);
        this.translatables = TranslatableMappings.translatablesFor(protocol);
    }

    public NBTComponentRewriter(final BackwardsProtocol<C, ?, ?, ?> protocol, final String version) {
        super(protocol);
        this.translatables = TranslatableMappings.translatablesFor(version);
    }

    @Override
    protected void handleTranslate(final JsonObject root, final String translate) {
        final String newTranslate = mappedTranslationKey(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }

    @Override
    protected void handleTranslate(final UserConnection connection, final CompoundTag parentTag, final StringTag translateTag) {
        final String newTranslate = mappedTranslationKey(translateTag.getValue());
        if (newTranslate != null) {
            parentTag.put("translate", new StringTag(newTranslate));
        }
    }

    @Override
    public @Nullable String mappedTranslationKey(final String translationKey) {
        return translatables.get(translationKey);
    }
}
