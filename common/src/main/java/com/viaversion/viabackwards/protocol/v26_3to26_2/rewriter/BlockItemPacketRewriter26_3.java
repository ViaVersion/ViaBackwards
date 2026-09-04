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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.rewriter.text.NBTComponentRewriter;
import com.viaversion.viaversion.util.MathUtil;
import java.util.BitSet;

import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.downgradeData;
import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.upgradeData;

public final class BlockItemPacketRewriter26_3 extends BackwardsStructuredItemRewriter<ClientboundPacket26_3, ServerboundPacket26_1, Protocol26_3To26_2> {

    public BlockItemPacketRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        protocol.replaceClientbound(ClientboundPackets26_3.LEVEL_PARTICLES, wrapper -> {
            final Particle particle = wrapper.read(protocol.getParticleRewriter().particleType());
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), particle);

            wrapper.passthrough(Types.BOOLEAN); // Override limiter
            wrapper.passthrough(Types.BOOLEAN); // Always show
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Offset X
            wrapper.passthrough(Types.FLOAT); // Offset Y
            wrapper.passthrough(Types.FLOAT); // Offset Z

            final float maxSpeedX = wrapper.read(Types.FLOAT);
            final float maxSpeedY = wrapper.read(Types.FLOAT);
            final float maxSpeedZ = wrapper.read(Types.FLOAT);
            wrapper.write(Types.FLOAT, Math.max(Math.max(maxSpeedX, maxSpeedY), maxSpeedZ));

            wrapper.passthroughAndMap(Types.VAR_INT, Types.INT); // Particle Count
            wrapper.read(Types.VAR_INT); // Randomization type

            wrapper.write(protocol.getParticleRewriter().mappedParticleType(), particle);
        });

        protocol.registerClientbound(ClientboundPackets26_3.OPEN_SIGN_EDITOR, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14);
            final boolean frontText = wrapper.read(Types.VAR_INT) == 1;
            wrapper.write(Types.BOOLEAN, frontText);
        });

        protocol.registerServerbound(ServerboundPackets26_1.SIGN_UPDATE, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14);

            wrapper.write(Types.VAR_INT, 1); // Front text - replace below if needed

            for (int i = 0; i < 4; i++) {
                wrapper.passthrough(Types.STRING); // Line
            }

            final boolean frontText = wrapper.read(Types.BOOLEAN);
            if (!frontText) {
                wrapper.set(Types.VAR_INT, 0, 0);
            }
        });

        protocol.registerClientbound(ClientboundPackets26_3.LIGHT_UPDATE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // X
            wrapper.passthrough(Types.VAR_INT); // Y
            handleLightMasks(wrapper);
        });
        protocol.appendClientbound(ClientboundPackets26_3.LEVEL_CHUNK_WITH_LIGHT, this::handleLightMasks);

        protocol.replaceClientbound(ClientboundPackets26_3.UPDATE_ADVANCEMENTS, wrapper -> {
            int lastPositionIndex = 0; // Index of the x and y display data position, easier than keeping state while reading

            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                final boolean hasDisplayData = wrapper.passthrough(Types.BOOLEAN);
                if (hasDisplayData) {
                    final Tag title = wrapper.passthrough(Types.TRUSTED_TAG);
                    final Tag description = wrapper.passthrough(Types.TRUSTED_TAG);
                    final NBTComponentRewriter<ClientboundPacket26_3> componentRewriter = protocol.getComponentRewriter();
                    componentRewriter.processTag(wrapper.user(), title);
                    componentRewriter.processTag(wrapper.user(), description);

                    passthroughClientboundItemTemplate(wrapper); // Icon
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }

                    // X and Y, set at the end of the loop
                    wrapper.write(Types.FLOAT, 0F);
                    wrapper.write(Types.FLOAT, 0F);
                }

                final int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry

                final float x = wrapper.read(Types.FLOAT);
                final float y = wrapper.read(Types.FLOAT);
                if (hasDisplayData) {
                    wrapper.set(Types.FLOAT, lastPositionIndex, x);
                    wrapper.set(Types.FLOAT, lastPositionIndex, y);
                    lastPositionIndex++;
                }
            }
        });
    }

    private void handleLightMasks(final PacketWrapper wrapper) {
        for (int i = 0; i < 4; i++) {
            final BitSet mask = wrapper.read(Types.BIT_SET);
            wrapper.write(Types.LONG_ARRAY_PRIMITIVE, mask.toLongArray());
        }
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        // TODO
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
        // TODO
    }
}
