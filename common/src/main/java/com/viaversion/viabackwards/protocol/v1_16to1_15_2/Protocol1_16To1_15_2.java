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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.data.BackwardsMappingData1_16;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter.BlockItemPacketRewriter1_16;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter.CommandRewriter1_16;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter.EntityPacketRewriter1_16;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter.TranslatableRewriter1_16;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage.PlayerAttributesStorage;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage.PlayerSneakStorage;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage.WorldNameTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ClientboundStatusPackets;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ServerboundPackets1_16;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.GsonUtil;
import java.util.UUID;

public class Protocol1_16To1_15_2 extends BackwardsProtocol<ClientboundPackets1_16, ClientboundPackets1_15, ServerboundPackets1_16, ServerboundPackets1_14> {

    public static final BackwardsMappingData1_16 MAPPINGS = new BackwardsMappingData1_16();
    private final EntityPacketRewriter1_16 entityRewriter = new EntityPacketRewriter1_16(this);
    private final BlockItemPacketRewriter1_16 blockItemPackets = new BlockItemPacketRewriter1_16(this);
    private final ParticleRewriter<ClientboundPackets1_16> particleRewriter = new ParticleRewriter<>(this);
    private final TranslatableRewriter1_16 translatableRewriter = new TranslatableRewriter1_16(this);
    private final TagRewriter<ClientboundPackets1_16> tagRewriter = new TagRewriter<>(this);

    public Protocol1_16To1_15_2() {
        super(ClientboundPackets1_16.class, ClientboundPackets1_15.class, ServerboundPackets1_16.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerBossEvent(ClientboundPackets1_16.BOSS_EVENT);
        translatableRewriter.registerPlayerCombat(ClientboundPackets1_16.PLAYER_COMBAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_16.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_16.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_16.SET_PLAYER_TEAM);
        translatableRewriter.registerTitle(ClientboundPackets1_16.SET_TITLES);
        translatableRewriter.registerSetObjective(ClientboundPackets1_16.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_13(ClientboundPackets1_16.LEVEL_PARTICLES, Types.DOUBLE);

        new CommandRewriter1_16(this).registerDeclareCommands(ClientboundPackets1_16.COMMANDS);

        registerClientbound(State.STATUS, ClientboundStatusPackets.STATUS_RESPONSE, wrapper -> {
            String original = wrapper.passthrough(Types.STRING);
            JsonObject object = GsonUtil.getGson().fromJson(original, JsonObject.class);
            JsonElement description = object.get("description");
            if (description == null) return;

            translatableRewriter.processText(wrapper.user(), description);
            wrapper.set(Types.STRING, 0, object.toString());
        });

        registerClientbound(ClientboundPackets1_16.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> translatableRewriter.processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT)));
                map(Types.BYTE);
                read(Types.UUID); // Sender
            }
        });

        registerClientbound(ClientboundPackets1_16.OPEN_SCREEN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Window Id
                map(Types.VAR_INT); // Window Type
                handler(wrapper -> translatableRewriter.processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT)));
                handler(wrapper -> {
                    int windowType = wrapper.get(Types.VAR_INT, 1);
                    if (windowType == 20) { // Smithing table
                        wrapper.set(Types.VAR_INT, 1, 7); // Open anvil inventory
                    } else if (windowType > 20) {
                        wrapper.set(Types.VAR_INT, 1, --windowType);
                    }
                });
            }
        });

        SoundRewriter<ClientboundPackets1_16> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_16.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_16.SOUND_ENTITY);
        soundRewriter.registerNamedSound(ClientboundPackets1_16.CUSTOM_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_16.STOP_SOUND);

        // Login success
        registerClientbound(State.LOGIN, ClientboundLoginPackets.LOGIN_FINISHED, wrapper -> {
            // Transform uuid to plain string
            UUID uuid = wrapper.read(Types.UUID);
            wrapper.write(Types.STRING, uuid.toString());
        });

        tagRewriter.register(ClientboundPackets1_16.UPDATE_TAGS, RegistryType.ENTITY);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_16.AWARD_STATS);

        registerServerbound(ServerboundPackets1_14.PLAYER_COMMAND, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // player id
            int action = wrapper.passthrough(Types.VAR_INT);
            if (action == 0) {
                wrapper.user().get(PlayerSneakStorage.class).setSneaking(true);
            } else if (action == 1) {
                wrapper.user().get(PlayerSneakStorage.class).setSneaking(false);
            }
        });

        registerServerbound(ServerboundPackets1_14.INTERACT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity Id
            int action = wrapper.passthrough(Types.VAR_INT);
            if (action == 0 || action == 2) {
                if (action == 2) {
                    // Location
                    wrapper.passthrough(Types.FLOAT);
                    wrapper.passthrough(Types.FLOAT);
                    wrapper.passthrough(Types.FLOAT);
                }

                wrapper.passthrough(Types.VAR_INT); // Hand
            }

            // New boolean: Whether the client is sneaking
            wrapper.write(Types.BOOLEAN, wrapper.user().get(PlayerSneakStorage.class).isSneaking());
        });

        registerServerbound(ServerboundPackets1_14.PLAYER_ABILITIES, wrapper -> {
            byte flags = wrapper.read(Types.BYTE);
            flags &= 2; // Only take the isFlying value (everything else has been removed and wasn't used anyways)
            wrapper.write(Types.BYTE, flags);

            wrapper.read(Types.FLOAT);
            wrapper.read(Types.FLOAT);
        });

        cancelServerbound(ServerboundPackets1_14.SET_JIGSAW_BLOCK);
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_16.PLAYER));
        user.addClientWorld(this.getClass(), new ClientWorld());

        user.put(new PlayerSneakStorage());
        user.put(new WorldNameTracker());
        user.put(new PlayerAttributesStorage());
    }

    @Override
    public TranslatableRewriter1_16 getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public BackwardsMappingData1_16 getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_16 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_16 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_16> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_16> getTagRewriter() {
        return tagRewriter;
    }
}
