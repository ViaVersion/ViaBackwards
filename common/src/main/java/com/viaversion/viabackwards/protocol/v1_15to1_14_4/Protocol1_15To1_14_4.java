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
package com.viaversion.viabackwards.protocol.v1_15to1_14_4;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.rewriter.BlockItemPacketRewriter1_15;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.rewriter.EntityPacketRewriter1_15;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.storage.ImmediateRespawnStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_15;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_3to1_14_4.packet.ClientboundPackets1_14_4;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;

public class Protocol1_15To1_14_4 extends BackwardsProtocol<ClientboundPackets1_15, ClientboundPackets1_14_4, ServerboundPackets1_14, ServerboundPackets1_14> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.15", "1.14", Protocol1_14_4To1_15.class);
    private final EntityPacketRewriter1_15 entityRewriter = new EntityPacketRewriter1_15(this);
    private final BlockItemPacketRewriter1_15 blockItemPackets = new BlockItemPacketRewriter1_15(this);
    private final ParticleRewriter<ClientboundPackets1_15> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_15> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_15> tagRewriter = new TagRewriter<>(this);

    public Protocol1_15To1_14_4() {
        super(ClientboundPackets1_15.class, ClientboundPackets1_14_4.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerBossEvent(ClientboundPackets1_15.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_15.CHAT);
        translatableRewriter.registerPlayerCombat(ClientboundPackets1_15.PLAYER_COMBAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_15.DISCONNECT);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_15.OPEN_SCREEN);
        translatableRewriter.registerTabList(ClientboundPackets1_15.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_15.SET_PLAYER_TEAM);
        translatableRewriter.registerTitle(ClientboundPackets1_15.SET_TITLES);
        translatableRewriter.registerSetObjective(ClientboundPackets1_15.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        SoundRewriter<ClientboundPackets1_15> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_15.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_15.SOUND_ENTITY);
        soundRewriter.registerNamedSound(ClientboundPackets1_15.CUSTOM_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_15.STOP_SOUND);

        // Explosion - manually send an explosion sound
        registerClientbound(ClientboundPackets1_15.EXPLODE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.FLOAT); // x
                map(Types.FLOAT); // y
                map(Types.FLOAT); // z
                handler(wrapper -> {
                    PacketWrapper soundPacket = wrapper.create(ClientboundPackets1_14_4.SOUND);
                    soundPacket.write(Types.VAR_INT, 243); // entity.generic.explode
                    soundPacket.write(Types.VAR_INT, 4); // blocks category
                    soundPacket.write(Types.INT, toEffectCoordinate(wrapper.get(Types.FLOAT, 0))); // x
                    soundPacket.write(Types.INT, toEffectCoordinate(wrapper.get(Types.FLOAT, 1))); // y
                    soundPacket.write(Types.INT, toEffectCoordinate(wrapper.get(Types.FLOAT, 2))); // z
                    soundPacket.write(Types.FLOAT, 4F); // volume
                    soundPacket.write(Types.FLOAT, 1F); // pitch - usually semi randomized by the server, but we don't really have to care about that
                    soundPacket.send(Protocol1_15To1_14_4.class);
                });
            }

            private int toEffectCoordinate(float coordinate) {
                return (int) (coordinate * 8);
            }
        });

        tagRewriter.register(ClientboundPackets1_15.UPDATE_TAGS, RegistryType.ENTITY);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_15.AWARD_STATS);
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(getClass(), new EntityTrackerBase(user, EntityTypes1_15.PLAYER));
        user.addClientWorld(getClass(), new ClientWorld());

        user.put(new ImmediateRespawnStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_15 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_15 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_15> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_15> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_15> getComponentRewriter() {
        return translatableRewriter;
    }
}
