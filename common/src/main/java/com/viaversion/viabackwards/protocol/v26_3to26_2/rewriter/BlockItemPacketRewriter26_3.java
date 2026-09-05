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
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPackets26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPackets26_3;
import com.viaversion.viaversion.rewriter.text.NBTComponentRewriter;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.downgradeData;
import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockItemPacketRewriter26_3.upgradeData;

public final class BlockItemPacketRewriter26_3 extends BackwardsStructuredItemRewriter<ClientboundPacket26_3, ServerboundPacket26_1, Protocol26_3To26_2> {

    private static final int DEFAULT_RANDOMIZATION = 0;
    private static final int ALTERNATIVE_WITH_SPEED_RANDOMIZATION = 2;

    public BlockItemPacketRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        protocol.replaceClientbound(ClientboundPackets26_3.LEVEL_PARTICLES, wrapper -> {
            final Particle particle = wrapper.read(protocol.getParticleRewriter().particleType());
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), particle);

            final boolean overrideLimiter = wrapper.read(Types.BOOLEAN);
            final boolean alwaysShow = wrapper.read(Types.BOOLEAN);
            final double x = wrapper.read(Types.DOUBLE);
            final double y = wrapper.read(Types.DOUBLE);
            final double z = wrapper.read(Types.DOUBLE);
            final float offsetX = wrapper.read(Types.FLOAT);
            final float offsetY = wrapper.read(Types.FLOAT);
            final float offsetZ = wrapper.read(Types.FLOAT);
            final float maxSpeedX = wrapper.read(Types.FLOAT);
            final float maxSpeedY = wrapper.read(Types.FLOAT);
            final float maxSpeedZ = wrapper.read(Types.FLOAT);
            final int count = wrapper.read(Types.VAR_INT);
            final int randomizationType = wrapper.read(Types.VAR_INT);

            final boolean singleSpeed = maxSpeedX == maxSpeedY && maxSpeedY == maxSpeedZ;
            if (count <= 0) {
                // When the count is 0, the client uses the offsets multiplied by the max speed as the particle's velocity,
                // or as extra data for particles like note or dust
                if (singleSpeed) {
                    writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeedX, count);
                } else {
                    // Write max speed of 1 to keep values intact
                    writeParticle(
                        wrapper, particle, overrideLimiter, alwaysShow,
                        x, y, z,
                        (float) ((double) maxSpeedX * offsetX), (float) ((double) maxSpeedY * offsetY), (float) ((double) maxSpeedZ * offsetZ),
                        1, count
                    );
                }
                return;
            }

            if (randomizationType == DEFAULT_RANDOMIZATION && singleSpeed) { // Same as 26.2
                writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeedX, count);
                return;
            }

            if (count > 3) {
                // Instead of spamming individual particle packets, spread them uniformly
                final float maxSpeed = Math.max(maxSpeedX, Math.max(maxSpeedY, maxSpeedZ));
                if (randomizationType == DEFAULT_RANDOMIZATION) {
                    writeParticle(wrapper, particle, overrideLimiter, alwaysShow, x, y, z, offsetX, offsetY, offsetZ, maxSpeed, count);
                } else {
                    writeParticle(
                        wrapper, particle, overrideLimiter, alwaysShow,
                        x + offsetX / 2, y + offsetY / 2, z + offsetZ / 2,
                        offsetX / 2, offsetY / 2, offsetZ / 2,
                        maxSpeed, count
                    );
                }
                return;
            }

            wrapper.cancel();

            // Do the randomization here and send the particles one by one with a count of 0 and exact positions
            final ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                final double varianceX, varianceY, varianceZ;
                final double speedX, speedY, speedZ;
                if (randomizationType == DEFAULT_RANDOMIZATION) {
                    varianceX = random.nextGaussian() * offsetX;
                    varianceY = random.nextGaussian() * offsetY;
                    varianceZ = random.nextGaussian() * offsetZ;
                    speedX = random.nextGaussian() * maxSpeedX;
                    speedY = random.nextGaussian() * maxSpeedY;
                    speedZ = random.nextGaussian() * maxSpeedZ;
                } else {
                    varianceX = random.nextDouble() * offsetX;
                    varianceY = random.nextDouble() * offsetY;
                    varianceZ = random.nextDouble() * offsetZ;
                    final boolean randomizeSpeed = randomizationType == ALTERNATIVE_WITH_SPEED_RANDOMIZATION;
                    speedX = randomizeSpeed ? maxSpeedX * random.nextDouble() : maxSpeedX;
                    speedY = randomizeSpeed ? maxSpeedY * random.nextDouble() : maxSpeedY;
                    speedZ = randomizeSpeed ? maxSpeedZ * random.nextDouble() : maxSpeedZ;
                }

                final PacketWrapper particlePacket = wrapper.create(ClientboundPackets26_1.LEVEL_PARTICLES);
                writeParticle(particlePacket, particle, overrideLimiter, alwaysShow,
                    x + varianceX, y + varianceY, z + varianceZ,
                    (float) speedX, (float) speedY, (float) speedZ,
                    1, 0
                );
                particlePacket.send(Protocol26_3To26_2.class);
            }
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

    private void writeParticle(final PacketWrapper wrapper, final Particle particle, final boolean overrideLimiter, final boolean alwaysShow,
                               final double x, final double y, final double z, final float offsetX, final float offsetY, final float offsetZ,
                               final float maxSpeed, final int count) {
        wrapper.write(Types.BOOLEAN, overrideLimiter);
        wrapper.write(Types.BOOLEAN, alwaysShow);
        wrapper.write(Types.DOUBLE, x);
        wrapper.write(Types.DOUBLE, y);
        wrapper.write(Types.DOUBLE, z);
        wrapper.write(Types.FLOAT, offsetX);
        wrapper.write(Types.FLOAT, offsetY);
        wrapper.write(Types.FLOAT, offsetZ);
        wrapper.write(Types.FLOAT, maxSpeed);
        wrapper.write(Types.INT, count);
        wrapper.write(protocol.getParticleRewriter().mappedParticleType(), particle);
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
