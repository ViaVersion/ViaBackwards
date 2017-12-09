package nl.matsv.viabackwards.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.Direction;
import us.myles.ViaVersion.protocols.base.ProtocolInfo;

import java.util.ArrayList;
import java.util.List;

public class PacketUtil {

	public static void sendToServer(PacketWrapper packet, Class<? extends Protocol> packetProtocol, boolean skipCurrentPipeline, boolean currentThread) throws Exception {
		if (packet.isCancelled()) return;
		ByteBuf raw = constructPacket(packet, packetProtocol, skipCurrentPipeline);

		final SocketChannel channel = packet.user().getChannel();
		final ChannelHandlerContext context = channel.pipeline().context("decoder");

		if (currentThread) {
			((ChannelInboundHandler)context.handler()).channelRead(context, raw);
		} else {
			channel.eventLoop().submit(new Runnable() {
				@Override
				public void run() {
					try {
						((ChannelInboundHandler)context.handler()).channelRead(context, raw);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			});
		}
	}

	private static ByteBuf constructPacket(PacketWrapper packet, Class<? extends Protocol> packetProtocol, boolean skipCurrentPipeline) throws Exception {
		List<Protocol> protocols = new ArrayList(packet.user().get(ProtocolInfo.class).getPipeline().pipes());
		int index = 0;

		for(int i = 0; i < protocols.size(); ++i) {
			if (((Protocol)protocols.get(i)).getClass().equals(packetProtocol)) {
				index = skipCurrentPipeline ? i + 1 : i;
				break;
			}
		}

		packet.resetReader();
		packet.apply(Direction.INCOMING, packet.user().get(ProtocolInfo.class).getState(), index, protocols);
		ByteBuf output = Unpooled.buffer();
		Type.VAR_INT.write(output, 1000);
		packet.writeToBuffer(output);
		return output;
	}

}
