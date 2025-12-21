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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.rewriter.BlockItemPacketRewriter1_19_4;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.rewriter.EntityPacketRewriter1_19_4;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage.EntityTracker1_19_4;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Protocol1_19_4To1_19_3 extends BackwardsProtocol<ClientboundPackets1_19_4, ClientboundPackets1_19_3, ServerboundPackets1_19_4, ServerboundPackets1_19_3> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.19.4", "1.19.3", Protocol1_19_3To1_19_4.class);
    private final EntityPacketRewriter1_19_4 entityRewriter = new EntityPacketRewriter1_19_4(this);
    private final BlockItemPacketRewriter1_19_4 itemRewriter = new BlockItemPacketRewriter1_19_4(this);
    private final ParticleRewriter<ClientboundPackets1_19_4> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_19_4> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_19_4> tagRewriter = new TagRewriter<>(this);

    public Protocol1_19_4To1_19_3() {
        super(ClientboundPackets1_19_4.class, ClientboundPackets1_19_3.class, ServerboundPackets1_19_4.class, ServerboundPackets1_19_3.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final SoundRewriter<ClientboundPackets1_19_4> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_4.STOP_SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_19_4.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_19_4.SOUND_ENTITY);

        // TODO fallback field in components
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_19_4.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_4.TAB_LIST);
        translatableRewriter.registerPlayerCombatKill(ClientboundPackets1_19_4.PLAYER_COMBAT_KILL);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_19_4.SET_PLAYER_TEAM);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.DISGUISED_CHAT);
        translatableRewriter.registerSetObjective(ClientboundPackets1_19_4.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_19(ClientboundPackets1_19_4.LEVEL_PARTICLES);

        new CommandRewriter<>(this) {
            @Override
            public void handleArgument(final PacketWrapper wrapper, final String argumentType) {
                switch (argumentType) {
                    case "minecraft:heightmap" -> wrapper.write(Types.VAR_INT, 0);
                    case "minecraft:time" -> wrapper.read(Types.INT); // Minimum
                    case "minecraft:resource", "minecraft:resource_or_tag" -> {
                        final String resource = wrapper.read(Types.STRING);
                        // Replace damage types with... something
                        wrapper.write(Types.STRING, resource.equals("minecraft:damage_type") ? "minecraft:mob_effect" : resource);
                    }
                    default -> super.handleArgument(wrapper, argumentType);
                }
            }
        }.registerDeclareCommands1_19(ClientboundPackets1_19_4.COMMANDS);

        tagRewriter.removeTags("minecraft:damage_type");
        tagRewriter.registerGeneric(ClientboundPackets1_19_4.UPDATE_TAGS);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19_4.AWARD_STATS);

        registerClientbound(ClientboundPackets1_19_4.SERVER_DATA, wrapper -> {
            final JsonElement element = wrapper.read(Types.COMPONENT);
            wrapper.write(Types.OPTIONAL_COMPONENT, element);

            final byte[] iconBytes = wrapper.read(Types.OPTIONAL_BYTE_ARRAY_PRIMITIVE);
            final String iconBase64 = iconBytes != null ? "data:image/png;base64," + new String(Base64.getEncoder().encode(iconBytes), StandardCharsets.UTF_8) : null;
            wrapper.write(Types.OPTIONAL_STRING, iconBase64);
        });

        cancelClientbound(ClientboundPackets1_19_4.BUNDLE_DELIMITER);
        cancelClientbound(ClientboundPackets1_19_4.CHUNKS_BIOMES); // We definitely do not want to cache every single chunk just to resent them with new biomes
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTracker1_19_4(user));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public BlockItemPacketRewriter1_19_4 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_19_4> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public EntityPacketRewriter1_19_4 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_19_4> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_19_4> getTagRewriter() {
        return tagRewriter;
    }
}
