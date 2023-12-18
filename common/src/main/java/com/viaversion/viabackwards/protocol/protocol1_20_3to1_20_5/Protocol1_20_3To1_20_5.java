/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.BlockItemPacketRewriter1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter.EntityPacketRewriter1_20_5;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundConfigurationPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundConfigurationPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public final class Protocol1_20_3To1_20_5 extends BackwardsProtocol<ClientboundPackets1_20_3, ClientboundPackets1_20_3, ServerboundPackets1_20_3, ServerboundPackets1_20_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.20.5", "1.20.3", Protocol1_20_5To1_20_3.class);
    private final EntityPacketRewriter1_20_5 entityRewriter = new EntityPacketRewriter1_20_5(this);
    private final BlockItemPacketRewriter1_20_5 itemRewriter = new BlockItemPacketRewriter1_20_5(this);
    private final TranslatableRewriter<ClientboundPackets1_20_3> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);

    public Protocol1_20_3To1_20_5() {
        super(ClientboundPackets1_20_3.class, ClientboundPackets1_20_3.class, ServerboundPackets1_20_3.class, ServerboundPackets1_20_3.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final TagRewriter<ClientboundPackets1_20_3> tagRewriter = new TagRewriter<>(this);
        tagRewriter.registerGeneric(ClientboundPackets1_20_3.TAGS);
        tagRewriter.registerGeneric(State.CONFIGURATION, ClientboundConfigurationPackets1_20_3.UPDATE_TAGS);

        final SoundRewriter<ClientboundPackets1_20_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_3.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_20_3.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_3.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_3.STATISTICS);

        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_20_3.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_20_3.TAB_LIST);
        translatableRewriter.registerCombatKill1_20(ClientboundPackets1_20_3.COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_3.DISGUISED_CHAT);
        translatableRewriter.registerPing();
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_20_5 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_20_5 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_20_3> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    protected ClientboundPacketType clientboundFinishConfigurationPacket() {
        return ClientboundConfigurationPackets1_20_3.FINISH_CONFIGURATION;
    }

    @Override
    protected ServerboundPacketType serverboundFinishConfigurationPacket() {
        return ServerboundConfigurationPackets1_20_2.FINISH_CONFIGURATION;
    }
}