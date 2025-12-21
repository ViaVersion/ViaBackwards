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
package com.viaversion.viabackwards.protocol.v1_21_11to1_21_9;

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter.BlockItemPacketRewriter1_21_11;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter.ComponentRewriter1_21_11;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.rewriter.EntityPacketRewriter1_21_11;
import com.viaversion.viabackwards.protocol.v1_21_11to1_21_9.storage.GameTimeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_11;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.api.type.types.version.VersionedTypesHolder;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundConfigurationPackets1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.Protocol1_21_9To1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPacket1_21_11;
import com.viaversion.viaversion.protocols.v1_21_9to1_21_11.packet.ClientboundPackets1_21_11;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.TagUtil;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;
import static com.viaversion.viaversion.util.TagUtil.getNamespacedTag;

public final class Protocol1_21_11To1_21_9 extends BackwardsProtocol<ClientboundPacket1_21_11, ClientboundPacket1_21_9, ServerboundPacket1_21_9, ServerboundPacket1_21_9> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.21.11", "1.21.9", Protocol1_21_9To1_21_11.class);
    private static final int END_FOG_COLOR = 10518688;
    private static final int OVERWORLD_FOG_COLOR = -4138753;
    private final EntityPacketRewriter1_21_11 entityRewriter = new EntityPacketRewriter1_21_11(this);
    private final BlockItemPacketRewriter1_21_11 itemRewriter = new BlockItemPacketRewriter1_21_11(this);
    private final ParticleRewriter<ClientboundPacket1_21_11> particleRewriter = new ParticleRewriter<>(this);
    private final NBTComponentRewriter<ClientboundPacket1_21_11> translatableRewriter = new ComponentRewriter1_21_11(this);
    private final TagRewriter<ClientboundPacket1_21_11> tagRewriter = new TagRewriter<>(this);
    private final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(this) {
        @Override
        protected void updateType(final CompoundTag tag, final String key, final FullMappings mappings) {
            super.updateType(tag, key, mappings);

            if (key.equals("sound") && tag.get(key) instanceof ListTag<?> listTag) {
                // From a compact list to a single value
                final Tag first;
                if (listTag.isEmpty()) {
                    first = new StringTag(mappings.mappedIdentifier(0)); // Dummy
                } else {
                    first = listTag.get(0);
                }
                tag.put(key, first);
            }
        }
    };

    public Protocol1_21_11To1_21_9() {
        super(ClientboundPacket1_21_11.class, ClientboundPacket1_21_9.class, ServerboundPacket1_21_9.class, ServerboundPacket1_21_9.class);
    }

    private void moveAttribute(final CompoundTag baseTag, @Nullable final CompoundTag attributes, final String key, final String mappedKey, final Function<Tag, Tag> tagMapper, @Nullable final Tag defaultTag) {
        final Tag attributeTag;
        if (attributes != null && (attributeTag = getNamespacedTag(attributes, key)) != null) {
            baseTag.put(mappedKey, tagMapper.apply(attributeTag));
        } else if (defaultTag != null) {
            baseTag.put(mappedKey, defaultTag);
        }
    }

    private int floatsToARGB(final float r, final float g, final float b) {
        return 255 << 24 | (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        // Add back mandatory fields from attributes, though most don't have any use in the client
        registryDataRewriter.addHandler("dimension_type", (key, tag) -> {
            if (Key.equals(key, "the_nether")) {
                tag.putString("effects", "minecraft:the_nether");
                tag.putBoolean("natural", false);
            } else if (Key.equals(key, "the_end")) {
                tag.putString("effects", "minecraft:the_end");
                tag.putBoolean("natural", false);
            } else {
                tag.putString("effects", "minecraft:overworld");
                tag.putBoolean("natural", true);
            }

            final ByteTag trueTag = new ByteTag((byte) 1);
            final CompoundTag attributes = tag.getCompoundTag("attributes");
            moveAttribute(tag, attributes, "visual/cloud_height", "cloud_height", Function.identity(), null);
            moveAttribute(tag, attributes, "gameplay/can_start_raid", "has_raids", Function.identity(), trueTag);
            moveAttribute(tag, attributes, "gameplay/can_start_raid", "has_raids", Function.identity(), trueTag);
            moveAttribute(tag, attributes, "gameplay/piglins_zombify", "piglin_safe", attributeTag -> ((NumberTag) attributeTag).asBoolean() ? ByteTag.ZERO : trueTag, ByteTag.ZERO);
            moveAttribute(tag, attributes, "gameplay/respawn_anchor_works", "respawn_anchor_works", Function.identity(), trueTag);
            moveAttribute(tag, attributes, "gameplay/bed_rule", "bed_works", attributeTag -> {
                final CompoundTag bedRule = (CompoundTag) attributeTag;
                return bedRule.getBoolean("can_sleep") || bedRule.getBoolean("can_set_spawn") ? trueTag : ByteTag.ZERO;
            }, trueTag);
            // Many different functions back into one, all have different effects on the client, so pick the most important one...
            moveAttribute(tag, attributes, "gameplay/fast_lava", "ultrawarm", Function.identity(), ByteTag.ZERO);
        });
        registryDataRewriter.addHandler("worldgen/biome", (key, tag) -> {
            final CompoundTag effects = tag.getCompoundTag("effects");

            final CompoundTag attributes = tag.removeUnchecked("attributes");
            moveAttribute(effects, attributes, "visual/sky_color", "sky_color", this::mapColor, new IntTag(0));
            moveAttribute(effects, attributes, "visual/water_fog_color", "water_fog_color", this::mapColor, new IntTag(-16448205));
            moveAttribute(effects, attributes, "visual/fog_color", "fog_color", this::mapColor, new IntTag(Key.equals(key, "the_end") ? END_FOG_COLOR : OVERWORLD_FOG_COLOR)); // overworld fog color as default

            moveAttribute(effects, effects, "water_color", "water_color", this::mapColor, new IntTag(4159204));
            moveAttribute(effects, effects, "foliage_color", "foliage_color", this::mapColor, null);
            moveAttribute(effects, effects, "dry_foliage_color", "dry_foliage_color", this::mapColor, null);
            moveAttribute(effects, effects, "grass_color", "grass_color", this::mapColor, null);
        });
        registryDataRewriter.addHandler("enchantment", (key, tag) -> {
            final CompoundTag effects = tag.getCompoundTag("effects");
            if (effects != null) {
                TagUtil.removeNamespaced(effects, "post_piercing_attack");
            }
        });
        registryDataRewriter.remove("zombie_nautilus_variant");
        registryDataRewriter.remove("timeline");
        registerClientbound(ClientboundConfigurationPackets1_21_9.REGISTRY_DATA, registryDataRewriter::handle);

        tagRewriter.removeTags("timeline");
        tagRewriter.registerGeneric(ClientboundPackets1_21_11.UPDATE_TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_21_9.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_21_11> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_11.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_21_11.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_21_11.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_21_11.AWARD_STATS);
        new AttributeRewriter<>(this).register1_21(ClientboundPackets1_21_11.UPDATE_ATTRIBUTES);

        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_21_11.OPEN_SCREEN);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_21_11.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.DISCONNECT);
        translatableRewriter.registerComponentPacket(ClientboundConfigurationPackets1_21_9.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_21_11.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill1_20(ClientboundPackets1_21_11.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPlayerInfoUpdate1_21_4(ClientboundPackets1_21_11.PLAYER_INFO_UPDATE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_21_11.SYSTEM_CHAT);
        translatableRewriter.registerDisguisedChat(ClientboundPackets1_21_11.DISGUISED_CHAT);
        translatableRewriter.registerPlayerChat1_21_5(ClientboundPackets1_21_11.PLAYER_CHAT);
        translatableRewriter.registerSetObjective(ClientboundPackets1_21_11.SET_OBJECTIVE);
        translatableRewriter.registerSetScore1_20_3(ClientboundPackets1_21_11.SET_SCORE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_21_4(ClientboundPackets1_21_11.LEVEL_PARTICLES);
        particleRewriter.registerExplode1_21_9(ClientboundPackets1_21_11.EXPLODE);
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_21_11.PLAYER));
        addItemHasher(connection, new ItemHasherBase(this, connection));
        connection.put(new GameTimeStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21_11 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21_11 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public RegistryDataRewriter getRegistryDataRewriter() {
        return registryDataRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPacket1_21_11> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public NBTComponentRewriter<ClientboundPacket1_21_11> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_21_11> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    public VersionedTypesHolder types() {
        return VersionedTypes.V1_21_11;
    }

    @Override
    public VersionedTypesHolder mappedTypes() {
        return VersionedTypes.V1_21_9;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_21_11, ClientboundPacket1_21_9, ServerboundPacket1_21_9, ServerboundPacket1_21_9> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_21_11.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_21_9.class, ClientboundConfigurationPackets1_21_9.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_21_6.class, ServerboundConfigurationPackets1_21_9.class)
        );
    }

    private Tag mapColor(final Tag attributeTag) {
        if (attributeTag instanceof ListTag<?> listTag) {
            final NumberTag r = ((NumberTag) listTag.get(0));
            final NumberTag g = ((NumberTag) listTag.get(1));
            final NumberTag b = ((NumberTag) listTag.get(2));
            return new IntTag(floatsToARGB(r.asFloat(), g.asFloat(), b.asFloat()));
        } else if (attributeTag instanceof StringTag stringTag) {
            // Remove '#' and parse hex string
            return new IntTag(Integer.parseInt(stringTag.getValue().substring(1), 16));
        }
        return attributeTag;
    }
}
