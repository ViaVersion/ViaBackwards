/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;

public class BungeePlugin extends Plugin implements ViaBackwardsPlatform, Listener {

    @Override
    public void onLoad() {
        Via.getManager().addEnableListener(() -> this.init(getDataFolder()));
        getProxy().getPluginManager().registerListener(this, this);
    }

    @Override
    public void disable() {
    }

    @EventHandler(priority = -110) // Slightly later than VV
    public void serverConnected(ServerConnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        if (player.getServer() == null) return;

        UserConnection connection = Via.getManager().getConnection(player.getUniqueId());
        if (connection == null) return;

        ProtocolInfo info = connection.getProtocolInfo();
        if (info == null || !info.getPipeline().contains(Protocol1_15_2To1_16.class)) return;

        // Need to send a dummy respawn with a different dimension before the actual respawn
        // We also don't know what dimension it's sent to, so just send 2 dummies :>
        sendRespawn(connection, -1);
        sendRespawn(connection, 0);
    }

    private void sendRespawn(UserConnection connection, int dimension) {
        PacketWrapper packet = new PacketWrapper(ClientboundPackets1_15.RESPAWN.ordinal(), null, connection);
        packet.write(Type.INT, dimension);
        packet.write(Type.LONG, 0L);
        packet.write(Type.UNSIGNED_BYTE, (short) 0);
        packet.write(Type.STRING, "default");
        try {
            packet.send(Protocol1_15_2To1_16.class, true, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
