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
package com.viaversion.viabackwards.protocol.v1_20_3to1_20_2;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.rewriter.BlockItemPacketRewriter1_20_3;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.rewriter.EntityPacketRewriter1_20_3;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.ResourcepackIDStorage;
import com.viaversion.viabackwards.protocol.v1_20_3to1_20_2.storage.SpawnPositionStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_3;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.Pair;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.rewriter.CommandRewriter1_19_4;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.Protocol1_20_2To1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPacket1_20_3;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPacket1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPacket1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.BitSet;
import java.util.UUID;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_20_3To1_20_2 extends BackwardsProtocol<ClientboundPacket1_20_3, ClientboundPacket1_20_2, ServerboundPacket1_20_3, ServerboundPacket1_20_2> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.20.3", "1.20.2", Protocol1_20_2To1_20_3.class);
    private final EntityPacketRewriter1_20_3 entityRewriter = new EntityPacketRewriter1_20_3(this);
    private final BlockItemPacketRewriter1_20_3 itemRewriter = new BlockItemPacketRewriter1_20_3(this);
    private final ParticleRewriter<ClientboundPacket1_20_3> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPacket1_20_3> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.NBT);
    private final TagRewriter<ClientboundPacket1_20_3> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_3To1_20_2() {
        super(ClientboundPacket1_20_3.class, ClientboundPacket1_20_2.class, ServerboundPacket1_20_3.class, ServerboundPacket1_20_2.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.registerGeneric(ClientboundPackets1_20_3.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_20_3.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_20_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_20_3.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_20_3.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_20_3.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_3.AWARD_STATS);
        new CommandRewriter1_19_4<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                if (argumentType.equals("minecraft:style")) {
                    wrapper.write(Types.VAR_INT, 1); // Phrase
                } else {
                    super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_20_3.COMMANDS);

        registerClientbound(ClientboundPackets1_20_3.RESET_SCORE, ClientboundPackets1_20_2.SET_SCORE, wrapper -> {
            wrapper.passthrough(Types.STRING); // Owner
            wrapper.write(Types.VAR_INT, 1); // Reset score

            final String objectiveName = wrapper.read(Types.OPTIONAL_STRING);
            wrapper.write(Types.STRING, objectiveName != null ? objectiveName : ""); // Objective name
        });
        registerClientbound(ClientboundPackets1_20_3.SET_SCORE, wrapper -> {
            wrapper.passthrough(Types.STRING); // Owner
            wrapper.write(Types.VAR_INT, 0); // Change score
            wrapper.passthrough(Types.STRING); // Objective name
            wrapper.passthrough(Types.VAR_INT); // Score

            // Remove display and number format
            wrapper.clearInputBuffer();
        });
        registerClientbound(ClientboundPackets1_20_3.SET_OBJECTIVE, wrapper -> {
            wrapper.passthrough(Types.STRING); // Objective Name
            final byte action = wrapper.passthrough(Types.BYTE); // Method
            if (action == 0 || action == 2) {
                convertComponent(wrapper); // Display Name
                wrapper.passthrough(Types.VAR_INT); // Render type

                // Remove number format
                wrapper.clearInputBuffer();
            }
        });

        cancelClientbound(ClientboundPackets1_20_3.TICKING_STATE);
        cancelClientbound(ClientboundPackets1_20_3.TICKING_STEP);

        registerServerbound(ServerboundPackets1_20_2.SET_JIGSAW_BLOCK, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            wrapper.passthrough(Types.STRING); // Name
            wrapper.passthrough(Types.STRING); // Target
            wrapper.passthrough(Types.STRING); // Pool
            wrapper.passthrough(Types.STRING); // Final state
            wrapper.passthrough(Types.STRING); // Joint type
            wrapper.write(Types.VAR_INT, 0); // Selection priority
            wrapper.write(Types.VAR_INT, 0); // Placement priority
        });

        // Components are now (mostly) written as nbt instead of json strings
        registerClientbound(ClientboundPackets1_20_3.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    convertComponent(wrapper); // Title
                    convertComponent(wrapper); // Description
                    final Item icon = itemRewriter.handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_20_2));
                    wrapper.write(Types.ITEM1_20_2, icon);
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Types.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                final int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry
            }
        });
        registerClientbound(ClientboundPackets1_20_3.COMMAND_SUGGESTIONS, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Transaction id
            wrapper.passthrough(Types.VAR_INT); // Start
            wrapper.passthrough(Types.VAR_INT); // Length

            final int suggestions = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < suggestions; i++) {
                wrapper.passthrough(Types.STRING); // Suggestion
                convertOptionalComponent(wrapper); // Tooltip
            }
        });
        registerClientbound(ClientboundPackets1_20_3.MAP_ITEM_DATA, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Map id
            wrapper.passthrough(Types.BYTE); // Scale
            wrapper.passthrough(Types.BOOLEAN); // Locked
            if (wrapper.passthrough(Types.BOOLEAN)) {
                final int icons = wrapper.passthrough(Types.VAR_INT);
                for (int i = 0; i < icons; i++) {
                    wrapper.passthrough(Types.VAR_INT); // Type
                    wrapper.passthrough(Types.BYTE); // X
                    wrapper.passthrough(Types.BYTE); // Y
                    wrapper.passthrough(Types.BYTE); // Rotation
                    convertOptionalComponent(wrapper); // Display name
                }
            }
        });
        registerClientbound(ClientboundPackets1_20_3.BOSS_EVENT, wrapper -> {
            wrapper.passthrough(Types.UUID); // Id

            final int action = wrapper.passthrough(Types.VAR_INT);
            if (action == 0 || action == 3) {
                convertComponent(wrapper);
            }
        });
        registerClientbound(ClientboundPackets1_20_3.PLAYER_CHAT, wrapper -> {
            wrapper.passthrough(Types.UUID); // Sender
            wrapper.passthrough(Types.VAR_INT); // Index
            wrapper.passthrough(Types.OPTIONAL_SIGNATURE_BYTES); // Signature
            wrapper.passthrough(Types.STRING); // Plain content
            wrapper.passthrough(Types.LONG); // Timestamp
            wrapper.passthrough(Types.LONG); // Salt

            final int lastSeen = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < lastSeen; i++) {
                final int index = wrapper.passthrough(Types.VAR_INT);
                if (index == 0) {
                    wrapper.passthrough(Types.SIGNATURE_BYTES);
                }
            }

            convertOptionalComponent(wrapper); // Unsigned content

            final int filterMaskType = wrapper.passthrough(Types.VAR_INT);
            if (filterMaskType == 2) {
                wrapper.passthrough(Types.LONG_ARRAY_PRIMITIVE); // Mask
            }

            wrapper.passthrough(Types.VAR_INT); // Chat type
            convertComponent(wrapper); // Sender
            convertOptionalComponent(wrapper); // Target
        });
        registerClientbound(ClientboundPackets1_20_3.SET_PLAYER_TEAM, wrapper -> {
            wrapper.passthrough(Types.STRING); // Team Name
            final byte action = wrapper.passthrough(Types.BYTE); // Mode
            if (action == 0 || action == 2) {
                convertComponent(wrapper); // Display Name
                wrapper.passthrough(Types.BYTE); // Flags
                wrapper.passthrough(Types.STRING); // Name Tag Visibility
                wrapper.passthrough(Types.STRING); // Collision rule
                wrapper.passthrough(Types.VAR_INT); // Color
                convertComponent(wrapper); // Prefix
                convertComponent(wrapper); // Suffix
            }
        });

        registerClientbound(ClientboundConfigurationPackets1_20_3.DISCONNECT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.DISCONNECT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.RESOURCE_PACK_PUSH, ClientboundPackets1_20_2.RESOURCE_PACK, resourcePackHandler());
        registerClientbound(ClientboundPackets1_20_3.SERVER_DATA, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.SET_ACTION_BAR_TEXT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.SET_TITLE_TEXT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.SET_SUBTITLE_TEXT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.DISGUISED_CHAT, wrapper -> {
            convertComponent(wrapper);
            wrapper.passthrough(Types.VAR_INT); // Chat type
            convertComponent(wrapper); // Name
            convertOptionalComponent(wrapper); // Target name
        });
        registerClientbound(ClientboundPackets1_20_3.SYSTEM_CHAT, this::convertComponent);
        registerClientbound(ClientboundPackets1_20_3.OPEN_SCREEN, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Container id

            final int containerTypeId = wrapper.read(Types.VAR_INT);
            final int mappedContainerTypeId = MAPPINGS.getMenuMappings().getNewId(containerTypeId);
            if (mappedContainerTypeId == -1) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, mappedContainerTypeId);

            convertComponent(wrapper);
        });
        registerClientbound(ClientboundPackets1_20_3.TAB_LIST, wrapper -> {
            convertComponent(wrapper);
            convertComponent(wrapper);
        });

        registerClientbound(ClientboundPackets1_20_3.PLAYER_COMBAT_KILL, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Duration
                handler(wrapper -> convertComponent(wrapper));
            }
        });
        registerClientbound(ClientboundPackets1_20_3.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.passthrough(Types.PROFILE_ACTIONS_ENUM1_19_3);
            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Types.STRING); // Player Name
                    wrapper.passthrough(Types.PROFILE_PROPERTY_ARRAY);
                }
                if (actions.get(1) && wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.UUID); // Session UUID
                    wrapper.passthrough(Types.PROFILE_KEY);
                }
                if (actions.get(2)) {
                    wrapper.passthrough(Types.VAR_INT); // Gamemode
                }
                if (actions.get(3)) {
                    wrapper.passthrough(Types.BOOLEAN); // Listed
                }
                if (actions.get(4)) {
                    wrapper.passthrough(Types.VAR_INT); // Latency
                }
                if (actions.get(5)) {
                    convertOptionalComponent(wrapper); // Display name
                }
            }
        });
        registerClientbound(ClientboundPackets1_20_3.SET_DEFAULT_SPAWN_POSITION, wrapper -> {
            final BlockPosition position = wrapper.passthrough(Types.BLOCK_POSITION1_14);
            final float angle = wrapper.passthrough(Types.FLOAT);

            wrapper.user().get(SpawnPositionStorage.class).setSpawnPosition(Pair.of(position, angle));
        });
        registerClientbound(ClientboundPackets1_20_3.GAME_EVENT, wrapper -> {
            final short reason = wrapper.passthrough(Types.UNSIGNED_BYTE);

            if (reason == 13) { // Level chunks load start
                wrapper.cancel();
                final Pair<BlockPosition, Float> spawnPositionAndAngle = wrapper.user().get(SpawnPositionStorage.class).getSpawnPosition();

                // To emulate the old behavior, we send a fake spawn pos packet containing the actual spawn pos which forces
                // the 1.20.2 client to close the downloading terrain screen like the new game state does
                final PacketWrapper spawnPosition = wrapper.create(ClientboundPackets1_20_2.SET_DEFAULT_SPAWN_POSITION);
                spawnPosition.write(Types.BLOCK_POSITION1_14, spawnPositionAndAngle.first()); // position
                spawnPosition.write(Types.FLOAT, spawnPositionAndAngle.second()); // angle
                spawnPosition.send(Protocol1_20_3To1_20_2.class, true);
            }
        });

        cancelClientbound(ClientboundPackets1_20_3.RESOURCE_PACK_POP);
        registerServerbound(ServerboundPackets1_20_2.RESOURCE_PACK, resourcePackStatusHandler());

        cancelClientbound(ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_POP);
        registerServerbound(ServerboundConfigurationPackets1_20_2.RESOURCE_PACK, resourcePackStatusHandler());
        registerClientbound(ClientboundConfigurationPackets1_20_3.RESOURCE_PACK_PUSH, ClientboundConfigurationPackets1_20_2.RESOURCE_PACK, resourcePackHandler());
    }

    private PacketHandler resourcePackStatusHandler() {
        return wrapper -> {
            final ResourcepackIDStorage storage = wrapper.user().get(ResourcepackIDStorage.class);
            wrapper.write(Types.UUID, storage != null ? storage.uuid() : UUID.randomUUID());
        };
    }

    private PacketHandler resourcePackHandler() {
        return wrapper -> {
            final UUID uuid = wrapper.read(Types.UUID);
            wrapper.user().put(new ResourcepackIDStorage(uuid));

            wrapper.passthrough(Types.STRING); // Url
            wrapper.passthrough(Types.STRING); // Hash
            wrapper.passthrough(Types.BOOLEAN); // Required
            convertOptionalComponent(wrapper);
        };
    }

    private void convertComponent(final PacketWrapper wrapper) {
        final Tag tag = wrapper.read(Types.TAG);
        translatableRewriter.processTag(wrapper.user(), tag);
        wrapper.write(Types.COMPONENT, ComponentUtil.tagToJson(tag));
    }

    private void convertOptionalComponent(final PacketWrapper wrapper) {
        final Tag tag = wrapper.read(Types.OPTIONAL_TAG);
        translatableRewriter.processTag(wrapper.user(), tag);
        wrapper.write(Types.OPTIONAL_COMPONENT, ComponentUtil.tagToJson(tag));
    }

    @Override
    public void init(final UserConnection connection) {
        connection.put(new SpawnPositionStorage());
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_20_3.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public BlockItemPacketRewriter1_20_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_20_3> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public EntityPacketRewriter1_20_3 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPacket1_20_3> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_20_3> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_20_3, ClientboundPacket1_20_2, ServerboundPacket1_20_3, ServerboundPacket1_20_2> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_20_3.class, ClientboundConfigurationPackets1_20_3.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_20_2.class, ClientboundConfigurationPackets1_20_2.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_20_3.class, ServerboundConfigurationPackets1_20_2.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_20_2.class, ServerboundConfigurationPackets1_20_2.class)
        );
    }
}
