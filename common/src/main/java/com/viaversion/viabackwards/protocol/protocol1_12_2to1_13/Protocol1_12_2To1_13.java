/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets.EntityPackets1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets.PlayerPacket1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets.SoundPackets1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.PlayerPositionStorage1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_13Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Protocol1_12_2To1_13 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_12_1, ServerboundPackets1_13, ServerboundPackets1_12_1> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private final EntityPackets1_13 entityRewriter = new EntityPackets1_13(this);
    private final BlockItemPackets1_13 blockItemPackets = new BlockItemPackets1_13(this);
    private final TranslatableRewriter<ClientboundPackets1_13> translatableRewriter = new TranslatableRewriter<ClientboundPackets1_13>(this) {
        @Override
        protected void handleTranslate(JsonObject root, String translate) {
            String newTranslate = mappedTranslationKey(translate);
            if (newTranslate != null || (newTranslate = getMappingData().getTranslateMappings().get(translate)) != null) {
                root.addProperty("translate", newTranslate);
            }
        }
    };
    private final TranslatableRewriter<ClientboundPackets1_13> translatableToLegacyRewriter = new TranslatableRewriter<ClientboundPackets1_13>(this) {
        @Override
        protected void handleTranslate(JsonObject root, String translate) {
            String newTranslate = mappedTranslationKey(translate);
            if (newTranslate != null || (newTranslate = getMappingData().getTranslateMappings().get(translate)) != null) {
                root.addProperty("translate", Protocol1_13To1_12_2.MAPPINGS.getMojangTranslation().getOrDefault(newTranslate, newTranslate));
            }
        }
    };

    public Protocol1_12_2To1_13() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_12_1.class, ServerboundPackets1_13.class, ServerboundPackets1_12_1.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_13To1_12_2.class, () -> {
            MAPPINGS.load();
            PaintingMapping.init();
            Via.getManager().getProviders().register(BackwardsBlockEntityProvider.class, new BackwardsBlockEntityProvider());
        });

        translatableRewriter.registerPing();
        translatableRewriter.registerBossBar(ClientboundPackets1_13.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_13.CHAT_MESSAGE);
        translatableRewriter.registerLegacyOpenWindow(ClientboundPackets1_13.OPEN_WINDOW);
        translatableRewriter.registerDisconnect(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_13.COMBAT_EVENT);
        translatableRewriter.registerTitle(ClientboundPackets1_13.TITLE);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);

        blockItemPackets.register();
        entityRewriter.register();
        new PlayerPacket1_13(this).register();
        new SoundPackets1_13(this).register();

        cancelClientbound(ClientboundPackets1_13.NBT_QUERY);
        cancelClientbound(ClientboundPackets1_13.CRAFT_RECIPE_RESPONSE);
        cancelClientbound(ClientboundPackets1_13.UNLOCK_RECIPES);
        cancelClientbound(ClientboundPackets1_13.ADVANCEMENTS);
        cancelClientbound(ClientboundPackets1_13.DECLARE_RECIPES);
        cancelClientbound(ClientboundPackets1_13.TAGS);

        cancelServerbound(ServerboundPackets1_12_1.CRAFT_RECIPE_REQUEST);
        cancelServerbound(ServerboundPackets1_12_1.RECIPE_BOOK_DATA);
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, Entity1_13Types.EntityType.PLAYER));

        user.put(new BackwardsBlockStorage());
        user.put(new TabCompleteStorage());

        if (ViaBackwards.getConfig().isFix1_13FacePlayer() && !user.has(PlayerPositionStorage1_13.class)) {
            user.put(new PlayerPositionStorage1_13());
        }
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_13 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_13 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_13> getTranslatableRewriter() {
        return translatableRewriter;
    }

    public String jsonToLegacy(String value) {
        if (value.isEmpty()) {
            return "";
        }

        try {
            return jsonToLegacy(JsonParser.parseString(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public String jsonToLegacy(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }

        translatableToLegacyRewriter.processText(value);

        try {
            Component component = ChatRewriter.HOVER_GSON_SERIALIZER.deserializeFromTree(value);
            return LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            ViaBackwards.getPlatform().getLogger().warning("Error converting json text to legacy: " + value);
            e.printStackTrace();
        }
        return "";
    }
}
