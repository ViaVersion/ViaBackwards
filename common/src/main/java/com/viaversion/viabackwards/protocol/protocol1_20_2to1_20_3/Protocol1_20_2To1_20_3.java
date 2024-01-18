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
package com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.rewriter.BlockItemPacketRewriter1_20_3;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.rewriter.EntityPacketRewriter1_20_3;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.storage.ResourcepackIDStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_2to1_20_3.storage.SpawnPositionStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_3;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.Pair;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.Protocol1_20_3To1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.BitSet;
import java.util.UUID;

public final class Protocol1_20_2To1_20_3 extends BackwardsProtocol<ClientboundPackets1_20_3, ClientboundPackets1_20_2, ServerboundPackets1_20_3, ServerboundPackets1_20_2> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.20.3", "1.20.2", Protocol1_20_3To1_20_2.class);
    private final EntityPacketRewriter1_20_3 entityRewriter = new EntityPacketRewriter1_20_3(this);
    private final BlockItemPacketRewriter1_20_3 itemRewriter = new BlockItemPacketRewriter1_20_3(this);
    private final TranslatableRewriter<ClientboundPackets1_20_3> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);

    public Protocol1_20_2To1_20_3() {
        super(ClientboundPackets1_20_3.class, ClientboundPackets1_20_2.class, ServerboundPackets1_20_3.class, ServerboundPackets1_20_2.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final TagRewriter<ClientboundPackets1_20_3> tagRewriter = new TagRewriter<>(this);
        tagRewriter.registerGeneric(ClientboundPackets1_20_3.TAGS);

        final SoundRewriter<ClientboundPackets1_20_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_3.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_3.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_3.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_3.STATISTICS);
        new CommandRewriter1_19_4<ClientboundPackets1_20_3>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) throws Exception {
                if (argumentType.equals("minecraft:style")) {
                    wrapper.write(Type.VAR_INT, 1); // Phrase
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_20_3.DECLARE_COMMANDS);

        registerClientbound(ClientboundPackets1_20_3.RESET_SCORE, ClientboundPackets1_20_2.UPDATE_SCORE, wrapper -> {
            wrapper.passthrough(Type.STRING); // Owner
            wrapper.write(Type.VAR_INT, 1); // Reset score

            final String objectiveName = wrapper.read(Type.OPTIONAL_STRING);
            wrapper.write(Type.STRING, objectiveName != null ? objectiveName : ""); // Objective name
        });
        registerClientbound(ClientboundPackets1_20_3.UPDATE_SCORE, wrapper -> {
            wrapper.passthrough(Type.STRING); // Owner
            wrapper.write(Type.VAR_INT, 0); // Change score
            wrapper.passthrough(Type.STRING); // Objective name
            wrapper.passthrough(Type.VAR_INT); // Score

            // Remove display and number format
            wrapper.clearInputBuffer();
        });
        registerClientbound(ClientboundPackets1_20_3.SCOREBOARD_OBJECTIVE, wrapper -> {
            wrapper.passthrough(Type.STRING); // Objective Name
            final byte action = wrapper.passthrough(Type.BYTE); // Method
            if (action == 0 || action == 2) {
                convertComponent(wrapper); // Display Name
                wrapper.passthrough(Type.VAR_INT); // Render type

                // Remove number format
                wrapper.clearInputBuffer();
            }
        });

        cancelClientbound(ClientboundPackets1_20_3.TICKING_STATE);
        cancelClientbound(ClientboundPackets1_20_3.TICKING_STEP);

        registerServerbound(ServerboundPackets1_20_2.UPDATE_JIGSAW_BLOCK, wrapper -> {
            wrapper.passthrough(Type.POSITION1_14); // Position
            wrapper.passthrough(Type.STRING); // Name
            wrapper.passthrough(Type.STRING); // Target
            wrapper.passthrough(Type.STRING); // Pool
            wrapper.passthrough(Type.STRING); // Final state
            wrapper.passthrough(Type.STRING); // Joint type
            wrapper.write(Type.VAR_INT, 0); // Selection priority
            wrapper.write(Type.VAR_INT, 0); // Placement priority
        });

        // Components are now (mostly) written as nbt instead of json strings
        registerClientbound(ClientboundPackets1_20_3.ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Type.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING); // Identifier

                // Parent
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.STRING);
                }

                // Display data
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    convertComponent(wrapper); // Title
                    convertComponent(wrapper); // Description
                    itemRewriter.handleItemToClient(wrapper.passthrough(Type.ITEM1_20_2)); // Icon
                    wrapper.passthrough(Type.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Type.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING); // Background texture
                    }
                    wrapper.passthrough(Type.FLOAT); // X
                    wrapper.passthrough(Type.FLOAT); // Y
                }

                final int requirements = wrapper.passthrough(Type.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY);
                }

                wrapper.passthrough(Type.BOOLEAN); // Send telemetry
            }
        });
        registerClientbound(ClientboundPackets1_20_3.TAB_COMPLETE, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Transaction id
            wrapper.passthrough(Type.VAR_INT); // Start
            wrapper.passthrough(Type.VAR_INT); // Length

            final int suggestions = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < suggestions; i++) {
                wrapper.passthrough(Type.STRING); // Suggestion
                convertOptionalComponent(wrapper); // Tooltip
            }
        });
        registerClientbound(ClientboundPackets1_20_3.MAP_DATA, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Map id
            wrapper.passthrough(Type.BYTE); // Scale
            wrapper.passthrough(Type.BOOLEAN); // Locked
            if (wrapper.passthrough(Type.BOOLEAN)) {
                final int icons = wrapper.passthrough(Type.VAR_INT);
                for (int i = 0; i < icons; i++) {
                    wrapper.passthrough(Type.VAR_INT); // Type
                    wrapper.passthrough(Type.BYTE); // X
                    wrapper.passthrough(Type.BYTE); // Y
                    wrapper.passthrough(Type.BYTE); // Rotation
                    convertOptionalComponent(wrapper); // Display name
                }
            }
        });
        registerClientbound(ClientboundPackets1_20_3.BOSSBAR, wrapper -> {
            wrapper.passthrough(Type.UUID); // Id

            final int action = wrapper.passthrough(Type.VAR_INT);
            if (action == 0 || action == 3) {
                convertComponent(wrapper);
            }
        });
        registerClientbound(ClientboundPackets1_20_3.PLAYER_CHAT, wrapper -> {
            wrapper.passthrough(Type.UUID); // Sender
            wrapper.passthrough(Type.VAR_INT); // Index
            wrapper.passthrough(Type.OPTIONAL_SIGNATURE_BYTES); // Signature
            wrapper.passthrough(Type.STRING); // Plain content
            wrapper.passthrough(Type.LONG); // Timestamp
            wrapper.passthrough(Type.LONG); // Salt

            final int lastSeen = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < lastSeen; i++) {
                final int index = wrapper.passthrough(Type.VAR_INT);
                if (index == 0) {
                    wrapper.passthrough(Type.SIGNATURE_BYTES);
                }
            }

            convertOptionalComponent(wrapper); // Unsigned content

            final int filterMaskType = wrapper.passthrough(Type.VAR_INT);
            if (filterMaskType == 2) {
                wrapper.passthrough(Type.LONG_ARRAY_PRIMITIVE); // Mask
            }

            wrapper.passthrough(Type.VAR_INT); // Chat type
            convertComponent(wrapper); // Sender
            convertOptionalComponent(wrapper); // Target
        });
        registerClientbound(ClientboundPackets1_20_3.TEAMS, wrapper -> {
            wrapper.passthrough(Type.STRING); // Team Name
            final byte action = wrapper.passthrough(Type.BYTE); // Mode
            if (action == 0 || action == 2) {
                convertComponent(wrapper); // Display Name
                wrapper.passthrough(Type.BYTE); // Flags
                wrapper.passthrough(Type.STRING); // Name Tag Visibility
                wrapper.passthrough(Type.STRING); // Collision rule
                wrapper.passthrough(Type.VAR_INT); // Color
                convertComponent(wrapper); // Prefix
                convertComponent(wrapper); // Suffix
            }
        });

        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_2.DISCONNECT.getId(), ClientboundConfigurationPackets1_20_2.DISCONNECT.getId(), this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.DISCONNECT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.RESOURCE_PACK_PUSH, ClientboundPackets1_20_2.RESOURCE_PACK, resourcePackHandler());
        registerClientbound(ClientboundPackets1_20_3.SERVER_DATA, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.ACTIONBAR, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.TITLE_TEXT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.TITLE_SUBTITLE, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.DISGUISED_CHAT, wrapper -> {
            convertComponent(wrapper);
            wrapper.passthrough(Type.VAR_INT); // Chat type
            convertComponent(wrapper); // Name
            convertOptionalComponent(wrapper); // Target name
        });
        registerClientbound(ClientboundPackets1_20_3.SYSTEM_CHAT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.OPEN_WINDOW, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Container id

            final int containerTypeId = wrapper.read(Type.VAR_INT);
            final int mappedContainerTypeId = MAPPINGS.getMenuMappings().getNewId(containerTypeId);
            if (mappedContainerTypeId == -1) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Type.VAR_INT, mappedContainerTypeId);

            convertComponent(wrapper);
        });
        registerClientbound(ClientboundPackets1_20_3.TAB_LIST, wrapper -> {
            convertComponent(wrapper);
            convertComponent(wrapper);
        });

        registerClientbound(ClientboundPackets1_20_3.COMBAT_KILL, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Duration
                handler(wrapper -> convertComponent(wrapper));
            }
        });
        registerClientbound(ClientboundPackets1_20_3.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.passthrough(Type.PROFILE_ACTIONS_ENUM);
            final int entries = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Type.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Type.STRING); // Player Name

                    final int properties = wrapper.passthrough(Type.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Type.STRING); // Name
                        wrapper.passthrough(Type.STRING); // Value
                        wrapper.passthrough(Type.OPTIONAL_STRING); // Signature
                    }
                }
                if (actions.get(1) && wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.UUID); // Session UUID
                    wrapper.passthrough(Type.PROFILE_KEY);
                }
                if (actions.get(2)) {
                    wrapper.passthrough(Type.VAR_INT); // Gamemode
                }
                if (actions.get(3)) {
                    wrapper.passthrough(Type.BOOLEAN); // Listed
                }
                if (actions.get(4)) {
                    wrapper.passthrough(Type.VAR_INT); // Latency
                }
                if (actions.get(5)) {
                    convertOptionalComponent(wrapper); // Display name
                }
            }
        });
        registerClientbound(ClientboundPackets1_20_3.SPAWN_POSITION, wrapper -> {
            final Position position = wrapper.passthrough(Type.POSITION1_14);
            final float angle = wrapper.passthrough(Type.FLOAT);

            wrapper.user().get(SpawnPositionStorage.class).setSpawnPosition(Pair.of(position, angle));
        });
        registerClientbound(ClientboundPackets1_20_3.GAME_EVENT, wrapper -> {
            final short reason = wrapper.passthrough(Type.UNSIGNED_BYTE);

            if (reason == 13) { // Level chunks load start
                wrapper.cancel();
                final Pair<Position, Float> spawnPositionAndAngle = wrapper.user().get(SpawnPositionStorage.class).getSpawnPosition();

                // To emulate the old behavior, we send a fake spawn pos packet containing the actual spawn pos which forces
                // the 1.20.2 client to close the downloading terrain screen like the new game state does
                final PacketWrapper spawnPosition = wrapper.create(ClientboundPackets1_20_2.SPAWN_POSITION);
                spawnPosition.write(Type.POSITION1_14, spawnPositionAndAngle.first()); // position
                spawnPosition.write(Type.FLOAT, spawnPositionAndAngle.second()); // angle
                spawnPosition.send(Protocol1_20_2To1_20_3.class, true);
            }
        });

        cancelClientbound(ClientboundPackets1_20_3.RESOURCE_PACK_POP);
        registerServerbound(ServerboundPackets1_20_2.RESOURCE_PACK_STATUS, resourcePackStatusHandler());

        cancelClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_POP.getId());
        registerServerbound(State.CONFIGURATION, ServerboundConfigurationPackets1_20_2.RESOURCE_PACK, resourcePackStatusHandler());
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_PUSH.getId(), ClientboundConfigurationPackets1_20_2.RESOURCE_PACK.getId(), resourcePackHandler());
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_3.UPDATE_TAGS.getId(), ClientboundConfigurationPackets1_20_2.UPDATE_TAGS.getId(), tagRewriter.getGenericHandler());
        // TODO Auto map via packet types provider
        registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_3.UPDATE_ENABLED_FEATURES.getId(), ClientboundConfigurationPackets1_20_2.UPDATE_ENABLED_FEATURES.getId());
    }

    private PacketHandler resourcePackStatusHandler() {
        return wrapper -> {
            final ResourcepackIDStorage storage = wrapper.user().get(ResourcepackIDStorage.class);
            wrapper.write(Type.UUID, storage != null ? storage.uuid() : UUID.randomUUID());
        };
    }

    private PacketHandler resourcePackHandler() {
        return wrapper -> {
            final UUID uuid = wrapper.read(Type.UUID);
            wrapper.user().put(new ResourcepackIDStorage(uuid));

            wrapper.passthrough(Type.STRING); // Url
            wrapper.passthrough(Type.STRING); // Hash
            wrapper.passthrough(Type.BOOLEAN); // Required
            convertOptionalComponent(wrapper);
        };
    }

    private void convertComponent(final PacketWrapper wrapper) throws Exception {
        final Tag tag = wrapper.read(Type.TAG);
        translatableRewriter.processTag(tag);
        wrapper.write(Type.COMPONENT, ComponentUtil.tagToJson(tag));
    }

    private void convertOptionalComponent(final PacketWrapper wrapper) throws Exception {
        final Tag tag = wrapper.read(Type.OPTIONAL_TAG);
        translatableRewriter.processTag(tag);
        wrapper.write(Type.OPTIONAL_COMPONENT, ComponentUtil.tagToJson(tag));
    }

    @Override
    public void init(final UserConnection connection) {
        connection.put(new SpawnPositionStorage());
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_20_3.PLAYER));
    }

    @Override
    protected ServerboundPacketType serverboundFinishConfigurationPacket() {
        return ServerboundConfigurationPackets1_20_2.FINISH_CONFIGURATION;
    }

    @Override
    protected ClientboundPacketType clientboundFinishConfigurationPacket() {
        return ClientboundConfigurationPackets1_20_3.FINISH_CONFIGURATION;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public BlockItemPacketRewriter1_20_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public EntityPacketRewriter1_20_3 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_20_3> getTranslatableRewriter() {
        return translatableRewriter;
    }
}