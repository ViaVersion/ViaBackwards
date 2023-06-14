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
package com.viaversion.viabackwards.protocol.protocol1_19_4to1_20;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.packets.BlockItemPackets1_20;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.packets.EntityPackets1_20;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ServerboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import java.util.Arrays;

public final class Protocol1_19_4To1_20 extends BackwardsProtocol<ClientboundPackets1_19_4, ClientboundPackets1_19_4, ServerboundPackets1_19_4, ServerboundPackets1_19_4> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private final TranslatableRewriter<ClientboundPackets1_19_4> translatableRewriter = new TranslatableRewriter<>(this);
    private final EntityPackets1_20 entityRewriter = new EntityPackets1_20(this);
    private final BlockItemPackets1_20 itemRewriter = new BlockItemPackets1_20(this);

    public Protocol1_19_4To1_20() {
        super(ClientboundPackets1_19_4.class, ClientboundPackets1_19_4.class, ServerboundPackets1_19_4.class, ServerboundPackets1_19_4.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final TagRewriter<ClientboundPackets1_19_4> tagRewriter = new TagRewriter<>(this);
        tagRewriter.registerGeneric(ClientboundPackets1_19_4.TAGS);

        final SoundRewriter<ClientboundPackets1_19_4> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_4.STOP_SOUND);
        soundRewriter.register1_19_3Sound(ClientboundPackets1_19_4.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_19_4.ENTITY_SOUND);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19_4.STATISTICS);

        //TODO open window
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19_4.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_19_4.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_4.TAB_LIST);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_4.DISGUISED_CHAT);
        translatableRewriter.registerPing();

        registerClientbound(ClientboundPackets1_19_4.UPDATE_ENABLED_FEATURES, wrapper -> {
            String[] enabledFeatures = wrapper.read(Type.STRING_ARRAY);
            final int length = enabledFeatures.length;
            enabledFeatures = Arrays.copyOf(enabledFeatures, length + 1);
            enabledFeatures[length] = "minecraft:update_1_20";
            wrapper.write(Type.STRING_ARRAY, enabledFeatures);
        });

        registerClientbound(ClientboundPackets1_19_4.COMBAT_END, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Duration
            wrapper.write(Type.INT, -1); // Killer ID - unused (who knows for how long?)
        });
        registerClientbound(ClientboundPackets1_19_4.COMBAT_KILL, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Duration
            wrapper.write(Type.INT, -1); // Killer ID - unused (who knows for how long?)
            translatableRewriter.processText(wrapper.passthrough(Type.COMPONENT));
        });
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_19_4Types.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_20 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPackets1_20 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_19_4> getTranslatableRewriter() {
        return translatableRewriter;
    }
}