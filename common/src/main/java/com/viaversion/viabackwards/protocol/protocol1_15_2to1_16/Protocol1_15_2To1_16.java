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
package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.chat.TranslatableRewriter1_16;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data.CommandRewriter1_16;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data.WorldNameTracker;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.packets.BlockItemPackets1_16;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.packets.EntityPackets1_16;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.storage.PlayerSneakStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ServerboundPackets1_16;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.GsonUtil;
import java.util.UUID;

public class Protocol1_15_2To1_16 extends BackwardsProtocol<ClientboundPackets1_16, ClientboundPackets1_15, ServerboundPackets1_16, ServerboundPackets1_14> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private final EntityPackets1_16 entityRewriter = new EntityPackets1_16(this);
    private final BlockItemPackets1_16 blockItemPackets = new BlockItemPackets1_16(this);
    private final TranslatableRewriter1_16 translatableRewriter = new TranslatableRewriter1_16(this);

    public Protocol1_15_2To1_16() {
        super(ClientboundPackets1_16.class, ClientboundPackets1_15.class, ServerboundPackets1_16.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerBossBar(ClientboundPackets1_16.BOSSBAR);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_16.COMBAT_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_16.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_16.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_16.TITLE);
        translatableRewriter.registerPing();

        new CommandRewriter1_16(this).registerDeclareCommands(ClientboundPackets1_16.DECLARE_COMMANDS);

        registerClientbound(State.STATUS, 0x00, 0x00, wrapper -> {
            String original = wrapper.passthrough(Type.STRING);
            JsonObject object = GsonUtil.getGson().fromJson(original, JsonObject.class);
            JsonElement description = object.get("description");
            if (description == null) return;

            translatableRewriter.processText(description);
            wrapper.set(Type.STRING, 0, object.toString());
        });

        registerClientbound(ClientboundPackets1_16.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> translatableRewriter.processText(wrapper.passthrough(Type.COMPONENT)));
                map(Type.BYTE);
                read(Type.UUID); // Sender
            }
        });

        registerClientbound(ClientboundPackets1_16.OPEN_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Window Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> translatableRewriter.processText(wrapper.passthrough(Type.COMPONENT)));
                handler(wrapper -> {
                    int windowType = wrapper.get(Type.VAR_INT, 1);
                    if (windowType == 20) { // Smithing table
                        wrapper.set(Type.VAR_INT, 1, 7); // Open anvil inventory
                    } else if (windowType > 20) {
                        wrapper.set(Type.VAR_INT, 1, --windowType);
                    }
                });
            }
        });

        SoundRewriter<ClientboundPackets1_16> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_16.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_16.ENTITY_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_16.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_16.STOP_SOUND);

        // Login success
        registerClientbound(State.LOGIN, 0x02, 0x02, wrapper -> {
            // Transform uuid to plain string
            UUID uuid = wrapper.read(Type.UUID);
            wrapper.write(Type.STRING, uuid.toString());
        });

        new TagRewriter<>(this).register(ClientboundPackets1_16.TAGS, RegistryType.ENTITY);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_16.STATISTICS);

        registerServerbound(ServerboundPackets1_14.ENTITY_ACTION, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // player id
            int action = wrapper.passthrough(Type.VAR_INT);
            if (action == 0) {
                wrapper.user().get(PlayerSneakStorage.class).setSneaking(true);
            } else if (action == 1) {
                wrapper.user().get(PlayerSneakStorage.class).setSneaking(false);
            }
        });

        registerServerbound(ServerboundPackets1_14.INTERACT_ENTITY, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity Id
            int action = wrapper.passthrough(Type.VAR_INT);
            if (action == 0 || action == 2) {
                if (action == 2) {
                    // Location
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                }

                wrapper.passthrough(Type.VAR_INT); // Hand
            }

            // New boolean: Whether the client is sneaking
            wrapper.write(Type.BOOLEAN, wrapper.user().get(PlayerSneakStorage.class).isSneaking());
        });

        registerServerbound(ServerboundPackets1_14.PLAYER_ABILITIES, wrapper -> {
            byte flags = wrapper.read(Type.BYTE);
            flags &= 2; // Only take the isFlying value (everything else has been removed and wasn't used anyways)
            wrapper.write(Type.BYTE, flags);

            wrapper.read(Type.FLOAT);
            wrapper.read(Type.FLOAT);
        });

        cancelServerbound(ServerboundPackets1_14.UPDATE_JIGSAW_BLOCK);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld());
        }

        user.put(new PlayerSneakStorage());
        user.put(new WorldNameTracker());
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_16.PLAYER));
    }

    @Override
    public TranslatableRewriter1_16 getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_16 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_16 getItemRewriter() {
        return blockItemPackets;
    }
}
