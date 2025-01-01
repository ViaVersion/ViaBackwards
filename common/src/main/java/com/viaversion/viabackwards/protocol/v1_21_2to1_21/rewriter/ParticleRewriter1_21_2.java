/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.rewriter.ParticleRewriter;

public final class ParticleRewriter1_21_2 extends ParticleRewriter<ClientboundPacket1_21_2> {

    public ParticleRewriter1_21_2(final Protocol<ClientboundPacket1_21_2, ?, ?, ?> protocol) {
        super(protocol, Types1_21_2.PARTICLE, Types1_21.PARTICLE);
    }

    @Override
    public void rewriteParticle(final UserConnection connection, final Particle particle) {
        final String identifier = protocol.getMappingData().getParticleMappings().identifier(particle.id());
        super.rewriteParticle(connection, particle);

        if (identifier.equals("minecraft:dust_color_transition")) {
            argbToVector(particle, 0);
            argbToVector(particle, 3);
        } else if (identifier.equals("minecraft:dust")) {
            argbToVector(particle, 0);
        } else if (identifier.equals("minecraft:trail")) {
            // Remove target
            particle.removeArgument(2);
            particle.removeArgument(1);
            particle.removeArgument(0);
        }
    }

    private void argbToVector(final Particle particle, final int index) {
        final int argb = particle.<Integer>removeArgument(index).getValue();
        final float r = ((argb >> 16) & 0xFF) / 255F;
        final float g = ((argb >> 8) & 0xFF) / 255F;
        final float b = (argb & 0xFF) / 255F;
        particle.add(index, Types.FLOAT, r);
        particle.add(index + 1, Types.FLOAT, g);
        particle.add(index + 2, Types.FLOAT, b);
    }
}
