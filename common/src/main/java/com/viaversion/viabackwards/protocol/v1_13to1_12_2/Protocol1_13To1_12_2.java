/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.BackwardsMappingData1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.PaintingMapping;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.provider.BackwardsBlockEntityProvider;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter.BlockItemPacketRewriter1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter.EntityPacketRewriter1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter.PlayerPacketRewriter1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter.SoundPacketRewriter1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.NoteBlockStorage;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.PlayerPositionStorage1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.TabCompleteStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Protocol1_13To1_12_2 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_12_1, ServerboundPackets1_13, ServerboundPackets1_12_1> {

    public static final BackwardsMappingData1_13 MAPPINGS = new BackwardsMappingData1_13();
    private final EntityPacketRewriter1_13 entityRewriter = new EntityPacketRewriter1_13(this);
    private final BlockItemPacketRewriter1_13 blockItemPackets = new BlockItemPacketRewriter1_13(this);
    private final TranslatableRewriter<ClientboundPackets1_13> translatableRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON) {
        @Override
        protected void handleTranslate(JsonObject root, String translate) {
            String mappedKey = mappedTranslationKey(translate);
            if (mappedKey != null || (mappedKey = getMappingData().getTranslateMappings().get(translate)) != null) {
                root.addProperty("translate", mappedKey);
            }
        }
    };
    private final TranslatableRewriter<ClientboundPackets1_13> translatableToLegacyRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON) {
        @Override
        protected void handleTranslate(JsonObject root, String translate) {
            String mappedKey = mappedTranslationKey(translate);
            if (mappedKey != null || (mappedKey = getMappingData().getTranslateMappings().get(translate)) != null) {
                root.addProperty("translate", Protocol1_12_2To1_13.MAPPINGS.getMojangTranslation().getOrDefault(mappedKey, mappedKey));
            }
        }
    };

    public Protocol1_13To1_12_2() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_12_1.class, ServerboundPackets1_13.class, ServerboundPackets1_12_1.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_12_2To1_13.class, () -> {
            MAPPINGS.load();
            PaintingMapping.init();
            Via.getManager().getProviders().register(BackwardsBlockEntityProvider.class, new BackwardsBlockEntityProvider());
        });

        translatableRewriter.registerPing();
        translatableRewriter.registerBossEvent(ClientboundPackets1_13.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.CHAT);
        translatableRewriter.registerLegacyOpenWindow(ClientboundPackets1_13.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerPlayerCombat(ClientboundPackets1_13.PLAYER_COMBAT);
        translatableRewriter.registerTitle(ClientboundPackets1_13.SET_TITLES);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);

        blockItemPackets.register();
        entityRewriter.register();
        new PlayerPacketRewriter1_13(this).register();
        new SoundPacketRewriter1_13(this).register();

        cancelClientbound(ClientboundPackets1_13.TAG_QUERY);
        cancelClientbound(ClientboundPackets1_13.PLACE_GHOST_RECIPE);
        cancelClientbound(ClientboundPackets1_13.RECIPE);
        cancelClientbound(ClientboundPackets1_13.UPDATE_ADVANCEMENTS);
        cancelClientbound(ClientboundPackets1_13.UPDATE_RECIPES);
        cancelClientbound(ClientboundPackets1_13.UPDATE_TAGS);

        cancelServerbound(ServerboundPackets1_12_1.PLACE_RECIPE);
        cancelServerbound(ServerboundPackets1_12_1.RECIPE_BOOK_UPDATE);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld());
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_13.EntityType.PLAYER));

        user.put(new BackwardsBlockStorage());
        user.put(new TabCompleteStorage());

        if (ViaBackwards.getConfig().isFix1_13FacePlayer() && !user.has(PlayerPositionStorage1_13.class)) {
            user.put(new PlayerPositionStorage1_13());
        }

        user.put(new NoteBlockStorage());
    }

    @Override
    public BackwardsMappingData1_13 getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_13 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_13 getItemRewriter() {
        return blockItemPackets;
    }

    // Don't override the parent method
    public TranslatableRewriter<ClientboundPackets1_13> translatableRewriter() {
        return translatableRewriter;
    }

    public String jsonToLegacy(UserConnection connection, String value) {
        if (value.isEmpty()) {
            return "";
        }

        try {
            return jsonToLegacy(connection, JsonParser.parseString(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String jsonToLegacy(UserConnection connection, @Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }

        translatableToLegacyRewriter.processText(connection, value);
        return ComponentUtil.jsonToLegacy(value);
    }
}
