/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.rewriter.ParticleRewriter;

public final class ParticleRewriter1_21_9 extends ParticleRewriter<ClientboundPacket1_21_9> {

    public ParticleRewriter1_21_9(final Protocol<ClientboundPacket1_21_9, ?, ?, ?> protocol) {
        super(protocol);
    }

    @Override
    public void rewriteParticle(final UserConnection connection, final Particle particle) {
        super.rewriteParticle(connection, particle);

        final String identifier = protocol.getMappingData().getParticleMappings().mappedIdentifier(particle.id());
        if ("minecraft:dragon_breath".equals(identifier)) {
            particle.removeArgument(0); // Power
        } else if ("minecraft:flash".equals(identifier)) {
            particle.removeArgument(0); // Color
        } else if ("minecraft:effect".equals(identifier) || "minecraft:instant_effect".equals(identifier)) {
            particle.removeArgument(1); // Power
            particle.removeArgument(0); // Color
        }
    }
}
