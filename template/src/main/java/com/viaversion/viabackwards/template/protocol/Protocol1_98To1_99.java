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
package com.viaversion.viabackwards.template.protocol;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.template.protocol.rewriter.BlockItemPacketRewriter1_99;
import com.viaversion.viabackwards.template.protocol.rewriter.EntityPacketRewriter1_99;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

// Placeholders to replace (in the entire package):
//   Protocol1_98To_99, EntityPacketRewriter1_99, BlockItemPacketRewriter1_99
//   Protocol1_20_5To1_20_3 (the ViaVersion protocol class the mappings depend on)
//   ClientboundPacket1_20_5
//   ServerboundPacket1_20_5
//   ClientboundConfigurationPackets1_20_5
//   ServerboundConfigurationPackets1_20_5
//   EntityTypes1_20_5 (UNMAPPED type)
//   1.99, 1.98
public final class Protocol1_98To1_99 extends BackwardsProtocol<ClientboundPacket1_20_5, ClientboundPacket1_20_5, ServerboundPacket1_20_5, ServerboundPacket1_20_5> {

    // ViaBackwards uses its own mappings and also needs a translatablerewriter for translation mappings
    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.99", "1.98", Protocol1_20_5To1_20_3.class);
    private final EntityPacketRewriter1_99 entityRewriter = new EntityPacketRewriter1_99(this);
    private final BlockItemPacketRewriter1_99 itemRewriter = new BlockItemPacketRewriter1_99(this);
    private final TranslatableRewriter<ClientboundPacket1_20_5> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);

    public Protocol1_98To1_99() {
        super(ClientboundPacket1_20_5.class, ClientboundPacket1_20_5.class, ServerboundPacket1_20_5.class, ServerboundPacket1_20_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final TagRewriter<ClientboundPacket1_20_5> tagRewriter = new TagRewriter<>(this);
        tagRewriter.registerGeneric(ClientboundPackets1_20_5.TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_20_5.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_20_5> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_5.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_5.STATISTICS);

        // Registers translatable mappings (missing a whole bunch still)
        //translatableRewriter.registerOpenWindow(ClientboundPackets1_20_5.OPEN_WINDOW); // Handled by registerOpenWindow in item rewriters
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_20_5.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_20_5.TAB_LIST);
        translatableRewriter.registerCombatKill1_20(ClientboundPackets1_20_5.COMBAT_KILL);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_20_5.DISGUISED_CHAT);
        translatableRewriter.registerPing();

        // TODO Attributes
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
    public EntityPacketRewriter1_99 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_99 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPacket1_20_5> getTranslatableRewriter() {
        return translatableRewriter;
    }

    // createPacketTypesProvider
}