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
package com.viaversion.viabackwards.protocol.v1_18to1_17_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.data.BackwardsMappingData1_18;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.rewriter.BlockItemPacketRewriter1_18;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.rewriter.EntityPacketRewriter1_18;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_17to1_17_1.packet.ClientboundPackets1_17_1;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public final class Protocol1_18To1_17_1 extends BackwardsProtocol<ClientboundPackets1_18, ClientboundPackets1_17_1, ServerboundPackets1_17, ServerboundPackets1_17> {

    private static final BackwardsMappingData1_18 MAPPINGS = new BackwardsMappingData1_18();
    private final EntityPacketRewriter1_18 entityRewriter = new EntityPacketRewriter1_18(this);
    private final BlockItemPacketRewriter1_18 itemRewriter = new BlockItemPacketRewriter1_18(this);
    private final TranslatableRewriter<ClientboundPackets1_18> translatableRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_18> tagRewriter = new TagRewriter<>(this);

    public Protocol1_18To1_17_1() {
        super(ClientboundPackets1_18.class, ClientboundPackets1_17_1.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_18.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_18.TAB_LIST);
        translatableRewriter.registerOpenScreen(ClientboundPackets1_18.OPEN_SCREEN);
        translatableRewriter.registerPlayerCombatKill(ClientboundPackets1_18.PLAYER_COMBAT_KILL);
        translatableRewriter.registerPing();

        final SoundRewriter<ClientboundPackets1_18> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_18.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_18.SOUND_ENTITY);
        soundRewriter.registerStopSound(ClientboundPackets1_18.STOP_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_18.CUSTOM_SOUND);

        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:lava_pool_stone_replaceables");
        tagRewriter.registerGeneric(ClientboundPackets1_18.UPDATE_TAGS);

        registerServerbound(ServerboundPackets1_17.CLIENT_INFORMATION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Language
                map(Types.BYTE); // View distance
                map(Types.VAR_INT); // Chat visibility
                map(Types.BOOLEAN); // Chat colors
                map(Types.UNSIGNED_BYTE); // Model customization
                map(Types.VAR_INT); // Main hand
                map(Types.BOOLEAN); // Text filtering enabled
                create(Types.BOOLEAN, true); // Allow listing in server list preview
            }
        });

        registerClientbound(ClientboundPackets1_18.SET_OBJECTIVE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.SET_DISPLAY_OBJECTIVE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE); // Slot
                map(Types.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.SET_PLAYER_TEAM, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.SET_SCORE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Owner
                map(Types.VAR_INT); // Method
                map(Types.STRING); // Name
                handler(cutName(0, 40));
                handler(cutName(1, 16));
            }
        });
    }

    private PacketHandler cutName(final int index, final int maxLength) {
        // May in some case cause clashes or bad ordering, but nothing we can do about that
        return wrapper -> {
            final String s = wrapper.get(Types.STRING, index);
            if (s.length() > maxLength) {
                wrapper.set(Types.STRING, index, s.substring(0, maxLength));
            }
        };
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_17.PLAYER));
    }

    @Override
    public BackwardsMappingData1_18 getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_18 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_18 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_18> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_18> getTagRewriter() {
        return tagRewriter;
    }
}
