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
package com.viaversion.viabackwards.protocol.v1_16_2to1_16_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.rewriter.BlockItemPacketRewriter1_16_2;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.rewriter.CommandRewriter1_16_2;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.rewriter.EntityPacketRewriter1_16_2;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.storage.BiomeStorage;
import com.viaversion.viabackwards.utils.BackwardsProtocolLogger;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ServerboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.ProtocolLogger;

public class Protocol1_16_2To1_16_1 extends BackwardsProtocol<ClientboundPackets1_16_2, ClientboundPackets1_16, ServerboundPackets1_16_2, ServerboundPackets1_16> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.16.2", "1.16", Protocol1_16_1To1_16_2.class);
    public static final ProtocolLogger LOGGER = new BackwardsProtocolLogger(Protocol1_16_2To1_16_1.class);
    private final EntityPacketRewriter1_16_2 entityRewriter = new EntityPacketRewriter1_16_2(this);
    private final BlockItemPacketRewriter1_16_2 blockItemPackets = new BlockItemPacketRewriter1_16_2(this);
    private final ParticleRewriter<ClientboundPackets1_16_2> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_16_2> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_16_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_16_2To1_16_1() {
        super(ClientboundPackets1_16_2.class, ClientboundPackets1_16.class, ServerboundPackets1_16_2.class, ServerboundPackets1_16.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerBossEvent(ClientboundPackets1_16_2.BOSS_EVENT);
        translatableRewriter.registerPlayerCombat(ClientboundPackets1_16_2.PLAYER_COMBAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_16_2.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_16_2.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_16_2.SET_TITLES);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_16_2.SET_PLAYER_TEAM);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_16_2.OPEN_SCREEN);
        translatableRewriter.registerSetObjective(ClientboundPackets1_16_2.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_13(ClientboundPackets1_16_2.LEVEL_PARTICLES, Types.DOUBLE);

        new CommandRewriter1_16_2(this).registerDeclareCommands(ClientboundPackets1_16_2.COMMANDS);

        SoundRewriter<ClientboundPackets1_16_2> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_16_2.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_16_2.SOUND_ENTITY);
        soundRewriter.registerNamedSound(ClientboundPackets1_16_2.CUSTOM_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_16_2.STOP_SOUND);

        registerClientbound(ClientboundPackets1_16_2.CHAT, wrapper -> {
            JsonElement message = wrapper.passthrough(Types.COMPONENT);
            translatableRewriter.processText(wrapper.user(), message);
            byte position = wrapper.passthrough(Types.BYTE);
            if (position == 2) { // https://bugs.mojang.com/browse/MC-119145
                wrapper.clearPacket();
                wrapper.setPacketType(ClientboundPackets1_16.SET_TITLES);
                wrapper.write(Types.VAR_INT, 2);
                wrapper.write(Types.COMPONENT, message);
            }
        });

        // Recipe book data has been split into 2 separate packets
        registerServerbound(ServerboundPackets1_16.RECIPE_BOOK_UPDATE, ServerboundPackets1_16_2.RECIPE_BOOK_CHANGE_SETTINGS, wrapper -> {
            int type = wrapper.read(Types.VAR_INT);
            if (type == 0) {
                // Shown, change to its own packet
                wrapper.passthrough(Types.STRING); // Recipe
                wrapper.setPacketType(ServerboundPackets1_16_2.RECIPE_BOOK_SEEN_RECIPE);
            } else {
                wrapper.cancel();

                // Settings
                for (int i = 0; i < 3; i++) {
                    sendSeenRecipePacket(i, wrapper);
                }
            }
        });

        tagRewriter.register(ClientboundPackets1_16_2.UPDATE_TAGS, RegistryType.ENTITY);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_16_2.AWARD_STATS);
    }

    private static void sendSeenRecipePacket(int recipeType, PacketWrapper wrapper) {
        boolean open = wrapper.read(Types.BOOLEAN);
        boolean filter = wrapper.read(Types.BOOLEAN);

        PacketWrapper newPacket = wrapper.create(ServerboundPackets1_16_2.RECIPE_BOOK_CHANGE_SETTINGS);
        newPacket.write(Types.VAR_INT, recipeType);
        newPacket.write(Types.BOOLEAN, open);
        newPacket.write(Types.BOOLEAN, filter);
        newPacket.sendToServer(Protocol1_16_2To1_16_1.class);
    }

    @Override
    public void init(UserConnection user) {
        user.put(new BiomeStorage());
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_16_2.PLAYER));
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_16_2> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public ProtocolLogger getLogger() {
        return LOGGER;
    }

    @Override
    public EntityPacketRewriter1_16_2 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_16_2 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_16_2> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_16_2> getTagRewriter() {
        return tagRewriter;
    }
}
