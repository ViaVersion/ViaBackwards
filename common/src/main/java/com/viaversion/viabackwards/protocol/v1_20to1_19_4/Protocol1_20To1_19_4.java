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
package com.viaversion.viabackwards.protocol.v1_20to1_19_4;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.data.BackwardsMappingData1_20;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.rewriter.BlockItemPacketRewriter1_20;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.rewriter.EntityPacketRewriter1_20;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ServerboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.ArrayUtil;

public final class Protocol1_20To1_19_4 extends BackwardsProtocol<ClientboundPackets1_19_4, ClientboundPackets1_19_4, ServerboundPackets1_19_4, ServerboundPackets1_19_4> {

    public static final BackwardsMappingData1_20 MAPPINGS = new BackwardsMappingData1_20();
    private final JsonNBTComponentRewriter<ClientboundPackets1_19_4> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final EntityPacketRewriter1_20 entityRewriter = new EntityPacketRewriter1_20(this);
    private final BlockItemPacketRewriter1_20 itemRewriter = new BlockItemPacketRewriter1_20(this);
    private final ParticleRewriter<ClientboundPackets1_19_4> particleRewriter = new ParticleRewriter<>(this);
    private final TagRewriter<ClientboundPackets1_19_4> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20To1_19_4() {
        super(ClientboundPackets1_19_4.class, ClientboundPackets1_19_4.class, ServerboundPackets1_19_4.class, ServerboundPackets1_19_4.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:replaceable_plants");
        tagRewriter.registerGeneric(ClientboundPackets1_19_4.UPDATE_TAGS);

        final SoundRewriter<ClientboundPackets1_19_4> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_4.STOP_SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_19_4.SOUND);
        soundRewriter.registerSound1_19_3(ClientboundPackets1_19_4.SOUND_ENTITY);

        particleRewriter.registerLevelParticles1_19(ClientboundPackets1_19_4.LEVEL_PARTICLES);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19_4.AWARD_STATS);

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_19_4.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_4.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_19_4.SET_PLAYER_TEAM);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.DISGUISED_CHAT);
        translatableRewriter.registerSetObjective(ClientboundPackets1_19_4.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        registerClientbound(ClientboundPackets1_19_4.UPDATE_ENABLED_FEATURES, wrapper -> {
            String[] enabledFeatures = wrapper.read(Types.STRING_ARRAY);
            wrapper.write(Types.STRING_ARRAY, ArrayUtil.add(enabledFeatures, "minecraft:update_1_20"));
        });

        registerClientbound(ClientboundPackets1_19_4.PLAYER_COMBAT_END, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Duration
            wrapper.write(Types.INT, -1); // Killer ID - unused (who knows for how long?)
        });
        registerClientbound(ClientboundPackets1_19_4.PLAYER_COMBAT_KILL, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Player ID
            wrapper.write(Types.INT, -1); // Killer ID - unused (who knows for how long?)
            translatableRewriter.processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT));
        });
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_19_4.PLAYER));
    }

    @Override
    public BackwardsMappingData1_20 getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_20 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_20 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_19_4> getParticleRewriter() {
        return particleRewriter;
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
