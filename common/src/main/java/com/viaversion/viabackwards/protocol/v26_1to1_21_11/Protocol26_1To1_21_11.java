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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.BlockItemPacketRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.ComponentRewriter26_1;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter.EntityPacketRewriter26_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.Protocol1_21_11To26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;
import static com.viaversion.viaversion.util.TagUtil.removeNamespaced;

public final class Protocol26_1To1_21_11 extends BackwardsProtocol<ClientboundPacket26_1, ClientboundPacket1_21_11, ServerboundPacket1_21_9, ServerboundPacket1_21_9> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("26.1", "1.21.11", Protocol1_21_11To26_1.class);
    private final EntityPacketRewriter26_1 entityRewriter = new EntityPacketRewriter26_1(this);
    private final BlockItemPacketRewriter26_1 itemRewriter = new BlockItemPacketRewriter26_1(this);
    private final ParticleRewriter<ClientboundPacket26_1> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket26_1> translatableRewriter = new ComponentRewriter26_1(this);
    private final TagRewriter<ClientboundPacket26_1> tagRewriter = new TagRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(this);

    public Protocol26_1To1_21_11() {
        super(ClientboundPacket26_1.class, ClientboundPacket1_21_11.class, ServerboundPacket1_21_9.class, ServerboundPacket1_21_9.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        // Remove new environment attributes
        registryDataRewriter.addHandler("dimension_type", (key, tag) -> {
            final CompoundTag attributes = tag.getCompoundTag("attributes");
            removeNamespaced(attributes, "visual/block_light_tint");
            removeNamespaced(attributes, "visual/night_vision_color");
            removeNamespaced(attributes, "visual/ambient_light_color");
        });

        // Move around entity variant names and sounds
        registryDataRewriter.addHandler("wolf_sound_variant", (key, tag) -> {
            final CompoundTag sounds = tag.getCompoundTag("adult_sounds");
            tag.remove("baby_sounds");
            tag.putAll(sounds);
        });
        registryDataRewriter.addHandler("frog_variant", (key, tag) -> swapEntityNameAffix("frog", tag));
        registryDataRewriter.addHandler("chicken_variant", (key, tag) -> swapEntityNameAffix("chicken", tag));
        registryDataRewriter.addHandler("cow_variant", (key, tag) -> swapEntityNameAffix("cow", tag));
        registryDataRewriter.addHandler("pig_variant", (key, tag) -> swapEntityNameAffix("pig", tag));
        registryDataRewriter.addHandler("cat_variant", (key, tag) -> removeEntityNamePrefix("cat", tag));
        registerClientbound(ClientboundConfigurationPackets1_21_9.REGISTRY_DATA, registryDataRewriter::handle);

        tagRewriter.registerGeneric(ClientboundPackets26_1.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21_9.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket26_1> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets26_1.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets26_1.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets26_1.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets26_1.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets26_1.UPDATE_ATTRIBUTES);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets26_1.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets26_1.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets26_1.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets26_1.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets26_1.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets26_1.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets26_1.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_21_5(ClientboundPackets26_1.SET_PLAYER_TEAM);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets26_1.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets26_1.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets26_1.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets26_1.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets26_1.PLAYER_CHAT);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets26_1.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_9(ClientboundPackets26_1.EXPLODE);

        cancelClientbound(ClientboundPackets26_1.LOW_DISK_SPACE_WARNING);
    }

    private void removeEntityNamePrefix(final String key, final CompoundTag tag) {
        final StringTag assetIdTag = tag.getStringTag("asset_id");
        final String assetId = assetIdTag.getValue();
        assetIdTag.setValue(assetId.replace(key + "_", ""));
    }

    private void swapEntityNameAffix(final String key, final CompoundTag tag) {
        final StringTag assetIdTag = tag.getStringTag("asset_id");
        final String assetId = assetIdTag.getValue();
        if (assetId.startsWith(key + "_")) {
            assetIdTag.setValue(assetId.substring(0, assetId.length() - (key.length() + 1)) + "_" + key);
        }
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_21_11.PLAYER));
        addItemHasher(connection, new ItemHasherBase(this, connection));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter26_1 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter26_1 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket26_1> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket26_1> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket26_1> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V26_1;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_11;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket26_1, ClientboundPacket1_21_11, ServerboundPacket1_21_9, ServerboundPacket1_21_9> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets26_1.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_11.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }
}
