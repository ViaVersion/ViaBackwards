/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets.BlockItemPackets1_18;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets.EntityPackets1_18;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;

public final class Protocol1_17_1To1_18 extends BackwardsProtocol<ClientboundPackets1_18, ClientboundPackets1_17_1, ServerboundPackets1_17, ServerboundPackets1_17> {

    private static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private final EntityPackets1_18 entityRewriter = new EntityPackets1_18(this);
    private final BlockItemPackets1_18 itemRewriter = new BlockItemPackets1_18(this);
    private final TranslatableRewriter<ClientboundPackets1_18> translatableRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);

    public Protocol1_17_1To1_18() {
        super(ClientboundPackets1_18.class, ClientboundPackets1_17_1.class, ServerboundPackets1_17.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.CHAT_MESSAGE);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_18.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_18.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_18.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_18.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_18.COMBAT_KILL);
        translatableRewriter.registerPing();

        final SoundRewriter<ClientboundPackets1_18> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerSound(ClientboundPackets1_18.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_18.ENTITY_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_18.STOP_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_18.NAMED_SOUND);

        final TagRewriter<ClientboundPackets1_18> tagRewriter = new TagRewriter<>(this);
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:lava_pool_stone_replaceables");
        tagRewriter.registerGeneric(ClientboundPackets1_18.TAGS);

        registerServerbound(ServerboundPackets1_17.CLIENT_SETTINGS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Language
                map(Type.BYTE); // View distance
                map(Type.VAR_INT); // Chat visibility
                map(Type.BOOLEAN); // Chat colors
                map(Type.UNSIGNED_BYTE); // Model customization
                map(Type.VAR_INT); // Main hand
                map(Type.BOOLEAN); // Text filtering enabled
                create(Type.BOOLEAN, true); // Allow listing in server list preview
            }
        });

        registerClientbound(ClientboundPackets1_18.SCOREBOARD_OBJECTIVE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.DISPLAY_SCOREBOARD, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.BYTE); // Slot
                map(Type.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.TEAMS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                handler(cutName(0, 16));
            }
        });
        registerClientbound(ClientboundPackets1_18.UPDATE_SCORE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Owner
                map(Type.VAR_INT); // Method
                map(Type.STRING); // Name
                handler(cutName(0, 40));
                handler(cutName(1, 16));
            }
        });
    }

    private PacketHandler cutName(final int index, final int maxLength) {
        // May in some case cause clashes or bad ordering, but nothing we can do about that
        return wrapper -> {
            final String s = wrapper.get(Type.STRING, index);
            if (s.length() > maxLength) {
                wrapper.set(Type.STRING, index, s.substring(0, maxLength));
            }
        };
    }

    @Override
    public void init(final UserConnection connection) {
        addEntityTracker(connection, new EntityTrackerBase(connection, EntityTypes1_17.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_18 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_18 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_18> getTranslatableRewriter() {
        return translatableRewriter;
    }
}
