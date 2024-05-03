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
package com.viaversion.viabackwards.protocol.protocol1_20_5to1_21;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.rewriter.BlockItemPacketRewriter1_21;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.rewriter.EntityPacketRewriter1_21;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.provider.PacketTypesProvider;
import com.viaversion.viaversion.api.protocol.packet.provider.SimplePacketTypesProvider;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viaversion.rewriter.AttributeRewriter;
import com.viaversion.viaversion.rewriter.ComponentRewriter.ReadType;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

import static com.viaversion.viaversion.util.ProtocolUtil.packetTypeMap;

public final class Protocol1_20_5To1_21 extends BackwardsProtocol<ClientboundPacket1_20_5, ClientboundPacket1_20_5, ServerboundPacket1_20_5, ServerboundPacket1_20_5> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.21", "1.20.5", Protocol1_21To1_20_5.class);
    private final EntityPacketRewriter1_21 entityRewriter = new EntityPacketRewriter1_21(this);
    private final BlockItemPacketRewriter1_21 itemRewriter = new BlockItemPacketRewriter1_21(this);
    private final TranslatableRewriter<ClientboundPacket1_20_5> translatableRewriter = new TranslatableRewriter<>(this, ReadType.NBT);
    private final TagRewriter<ClientboundPacket1_20_5> tagRewriter = new TagRewriter<>(this);

    public Protocol1_20_5To1_21() {
        super(ClientboundPacket1_20_5.class, ClientboundPacket1_20_5.class, ServerboundPacket1_20_5.class, ServerboundPacket1_20_5.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        tagRewriter.registerGeneric(ClientboundPackets1_20_5.TAGS);
        tagRewriter.registerGeneric(ClientboundConfigurationPackets1_20_5.UPDATE_TAGS);

        final SoundRewriter<ClientboundPacket1_20_5> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_20_5.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_20_5.STOP_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_20_5.STATISTICS);
        new AttributeRewriter<>(this).register1_20_5(ClientboundPackets1_20_5.ENTITY_PROPERTIES);

        translatableRewriter.registerOpenWindow(ClientboundPackets1_20_5.OPEN_WINDOW);
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
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_20_5.PLAYER));
        user.put(new EnchantmentsPaintingsStorage());
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_21 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_21 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPacket1_20_5> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPacket1_20_5> getTagRewriter() {
        return tagRewriter;
    }

    @Override
    protected PacketTypesProvider<ClientboundPacket1_20_5, ClientboundPacket1_20_5, ServerboundPacket1_20_5, ServerboundPacket1_20_5> createPacketTypesProvider() {
        return new SimplePacketTypesProvider<>(
            packetTypeMap(unmappedClientboundPacketType, ClientboundPackets1_20_5.class, ClientboundConfigurationPackets1_20_5.class),
            packetTypeMap(mappedClientboundPacketType, ClientboundPackets1_20_5.class, ClientboundConfigurationPackets1_20_5.class),
            packetTypeMap(mappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class),
            packetTypeMap(unmappedServerboundPacketType, ServerboundPackets1_20_5.class, ServerboundConfigurationPackets1_20_5.class)
        );
    }
}