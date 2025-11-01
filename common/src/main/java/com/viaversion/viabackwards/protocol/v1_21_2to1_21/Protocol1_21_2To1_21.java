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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.BlockItemPacketRewriter1_21_2;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.ComponentRewriter1_21_2;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.EntityPacketRewriter1_21_2;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter.ParticleRewriter1_21_2;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.InventoryStateIdStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.ItemTagStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.RecipeStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.SignStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.item.data.ChatType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.Protocol1_21To1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.ArrayUtil;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_21_2To1_21 extends BackwardsProtocol<ClientboundPacket1_21_2, ClientboundPacket1_21, ServerboundPacket1_21_2, ServerboundPacket1_20_5> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.2", "1.21", Protocol1_21To1_21_2.class);
    private final EntityPacketRewriter1_21_2 entityRewriter = new EntityPacketRewriter1_21_2(this);
    private final BlockItemPacketRewriter1_21_2 itemRewriter = new BlockItemPacketRewriter1_21_2(this);
    private final ParticleRewriter1_21_2 particleRewriter = new ParticleRewriter1_21_2(this);
    private final JsonNBTComponentRewriter<ClientboundPacket1_21_2> translatableRewriter = new ComponentRewriter1_21_2(this);
    private final TagRewriter<ClientboundPacket1_21_2> tagRewriter = new TagRewriter<>(this);

    public Protocol1_21_2To1_21() {
        super(ClientboundPacket1_21_2.class, ClientboundPacket1_21.class, ServerboundPacket1_21_2.class, ServerboundPacket1_20_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        registerClientbound(ClientboundPackets1_21_2.UPDATE_TAGS, this::storeTags);
        registerClientbound(ClientboundConfigurationPackets1_21.UPDATE_TAGS, this::storeTags);

        final SoundRewriter<ClientboundPacket1_21_2> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_2.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_2.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_2.STOP_SOUND);

        particleRewriter.registerLevelParticles1_20_5(ClientboundPackets1_21_2.LEVEL_PARTICLES);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_2.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_2.UPDATE_ATTRIBUTES);

        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_2.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_2.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_2.PLAYER_COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_2.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_2.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat(ClientboundPackets1_21_2.PLAYER_CHAT, ChatType.TYPE);
        translatableRewriter.registerPing();

        registerServerbound(ServerboundPackets1_20_5.CLIENT_INFORMATION, this::clientInformation);
        registerServerbound(ServerboundConfigurationPackets1_20_5.CLIENT_INFORMATION, this::clientInformation);

        registerClientbound(ClientboundConfigurationPackets1_21.UPDATE_ENABLED_FEATURES, wrapper -> {
            final String[] enabledFeatures = wrapper.read(Types.STRING_ARRAY);
            wrapper.write(Types.STRING_ARRAY, ArrayUtil.add(enabledFeatures, "bundle"));
        });

        cancelClientbound(ClientboundPackets1_21_2.MOVE_MINECART_ALONG_TRACK); // TODO

        registerClientbound(State.LOGIN, ClientboundLoginPackets.LOGIN_FINISHED, wrapper -> {
            wrapper.passthrough(Types.UUID); // UUID
            wrapper.passthrough(Types.STRING); // Name
            wrapper.passthrough(Types.PROFILE_PROPERTY_ARRAY);
            wrapper.write(Types.BOOLEAN, true); // Strict error handling. Always enabled for newer clients, so mimic that behavior
        });

        registerClientbound(ClientboundPackets1_21_2.SET_TIME, wrapper -> {
            wrapper.passthrough(Types.LONG); // Game time

            long dayTime = wrapper.read(Types.LONG);
            final boolean daylightCycle = wrapper.read(Types.BOOLEAN);
            if (!daylightCycle) {
                if (dayTime == 0) {
                    dayTime = -1;
                } else {
                    dayTime = -dayTime;
                }
            }
            wrapper.write(Types.LONG, dayTime);
        });
    }

    private void storeTags(final PacketWrapper wrapper) {
        tagRewriter.handleGeneric(wrapper);
        wrapper.resetReader();
        wrapper.user().get(ItemTagStorage.class).readItemTags(wrapper);
    }

    private void clientInformation(final PacketWrapper wrapper) {
        wrapper.passthrough(Types.STRING); // Locale
        wrapper.passthrough(Types.BYTE); // View distance
        wrapper.passthrough(Types.VAR_INT); // Chat visibility
        wrapper.passthrough(Types.BOOLEAN); // Chat colors
        wrapper.passthrough(Types.UNSIGNED_BYTE); // Skin parts
        wrapper.passthrough(Types.VAR_INT); // Main hand
        wrapper.passthrough(Types.BOOLEAN); // Text filtering enabled
        wrapper.passthrough(Types.BOOLEAN); // Allow listing
        wrapper.write(Types.VAR_INT, 0); // Particle status, assume 'all'
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new InventoryStateIdStorage());
        user.put(new ItemTagStorage());
        user.put(new RecipeStorage(this));
        user.put(new PlayerStorage());
        user.put(new SignStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_2 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_2 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter1_21_2 getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPacket1_21_2> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_2> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_21_2;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_2, ClientboundPacket1_21, ServerboundPacket1_21_2, ServerboundPacket1_20_5> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_2.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21.class, ClientboundConfigurationPackets1_21.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_2.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}
