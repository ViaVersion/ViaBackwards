/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.data.BackwardsMappingData1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter.BlockItemPacketRewriter1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter.CommandRewriter1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter.EntityPacketRewriter1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter.PlayerPacketRewriter1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter.SoundPacketRewriter1_14;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.DifficultyStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;

public class Protocol1_14To1_13_2 extends BackwardsProtocol<ClientboundPackets1_14, ClientboundPackets1_13, ServerboundPackets1_14, ServerboundPackets1_13> {

    public static final BackwardsMappingData1_14 MAPPINGS = new BackwardsMappingData1_14();
    private final EntityPacketRewriter1_14 entityRewriter = new EntityPacketRewriter1_14(this);
    private final BlockItemPacketRewriter1_14 itemRewriter = new BlockItemPacketRewriter1_14(this);
    private final BlockRewriter<ClientboundPackets1_14> blockRewriter = BlockRewriter.legacy(this);
    private final ParticleRewriter<ClientboundPackets1_14> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_14> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_14> tagRewriter = new TagRewriter<>(this);

    public Protocol1_14To1_13_2() {
        super(ClientboundPackets1_14.class, ClientboundPackets1_13.class, ServerboundPackets1_14.class, ServerboundPackets1_13.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        new CommandRewriter1_14(this).registerDeclareCommands(ClientboundPackets1_14.COMMANDS);
        new PlayerPacketRewriter1_14(this).register();
        new SoundPacketRewriter1_14(this).register();

        cancelClientbound(ClientboundPackets1_14.SET_CHUNK_CACHE_CENTER);
        cancelClientbound(ClientboundPackets1_14.SET_CHUNK_CACHE_RADIUS);

        registerClientbound(ClientboundPackets1_14.UPDATE_TAGS, wrapper -> {
            tagRewriter.handle(wrapper, RegistryType.BLOCK);
            tagRewriter.handle(wrapper, RegistryType.ITEM);
            tagRewriter.handle(wrapper, RegistryType.FLUID);

            // Eat entity tags
            int entityTagsSize = wrapper.read(Types.VAR_INT);
            for (int i = 0; i < entityTagsSize; i++) {
                wrapper.read(Types.STRING);
                wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE);
            }
        });

        registerClientbound(ClientboundPackets1_14.LIGHT_UPDATE, null, wrapper -> {
            int x = wrapper.read(Types.VAR_INT);
            int z = wrapper.read(Types.VAR_INT);
            int skyLightMask = wrapper.read(Types.VAR_INT);
            int blockLightMask = wrapper.read(Types.VAR_INT);
            int emptySkyLightMask = wrapper.read(Types.VAR_INT);
            int emptyBlockLightMask = wrapper.read(Types.VAR_INT);

            byte[][] skyLight = new byte[16][];
            // we don't need void and +256 light
            if (isSet(skyLightMask, 0)) {
                wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
            }
            for (int i = 0; i < 16; i++) {
                if (isSet(skyLightMask, i + 1)) {
                    skyLight[i] = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                } else if (isSet(emptySkyLightMask, i + 1)) {
                    skyLight[i] = ChunkLightStorage.EMPTY_LIGHT;
                }
            }
            if (isSet(skyLightMask, 17)) {
                wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
            }

            byte[][] blockLight = new byte[16][];
            if (isSet(blockLightMask, 0)) {
                wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
            }
            for (int i = 0; i < 16; i++) {
                if (isSet(blockLightMask, i + 1)) {
                    blockLight[i] = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                } else if (isSet(emptyBlockLightMask, i + 1)) {
                    blockLight[i] = ChunkLightStorage.EMPTY_LIGHT;
                }
            }
            if (isSet(blockLightMask, 17)) {
                wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
            }

            //TODO Soft memory leak: Don't store light if chunk is already loaded
            wrapper.user().get(ChunkLightStorage.class).setStoredLight(skyLight, blockLight, x, z);
            wrapper.cancel();
        });
    }

    private static boolean isSet(int mask, int i) {
        return (mask & (1 << i)) != 0;
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_14.PLAYER));
        user.addClientWorld(this.getClass(), new ClientWorld());

        if (!user.has(ChunkLightStorage.class)) {
            user.put(new ChunkLightStorage());
        }

        user.put(new DifficultyStorage());
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_14 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_14 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public BlockRewriter<ClientboundPackets1_14> getBlockRewriter() {
        return blockRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_14> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_14> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_14> getTagRewriter() {
        return tagRewriter;
    }
}
