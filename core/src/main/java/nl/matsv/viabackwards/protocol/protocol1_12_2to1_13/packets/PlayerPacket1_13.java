package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;

public class PlayerPacket1_13 extends Rewriter<Protocol1_12_2To1_13> {
	@Override
	protected void registerPackets(Protocol1_12_2To1_13 protocol) {

		//Scoreboard Objective
		protocol.out(State.PLAY, 0x45, 0x42, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						byte mode = wrapper.get(Type.BYTE, 0);
						if (mode == 0 || mode == 2) {
							String value = wrapper.read(Type.STRING);
							value = ChatRewriter.jsonTextToLegacy(value);
							if (value.length() > 32) value = value.substring(0, 32);
							wrapper.write(Type.STRING, value);
							int type = wrapper.read(Type.VAR_INT);
							wrapper.write(Type.STRING, type == 1 ? "hearts" : "integer");
						}
					}
				});
			}
		});

		//Teams
		protocol.out(State.PLAY, 0x47, 0x44, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				map(Type.BYTE);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						byte action = wrapper.get(Type.BYTE, 0);
						if (action == 0 || action == 2) {
							String displayName = wrapper.read(Type.STRING);
							displayName = ChatRewriter.jsonTextToLegacy(displayName);
							if (displayName.length() > 32) displayName = displayName.substring(0, 32);
							wrapper.write(Type.STRING, displayName);

							String prefix = wrapper.read(Type.STRING);
							String suffix = wrapper.read(Type.STRING);

							wrapper.passthrough(Type.BYTE); //Flags

							wrapper.passthrough(Type.STRING); //Name Tag Visibility
							wrapper.passthrough(Type.STRING); //Collision Rule

							int colour = wrapper.read(Type.VAR_INT);
							if (colour == 21) {
								colour = -1;
							}

							wrapper.write(Type.BYTE, (byte) colour);

							wrapper.write(Type.STRING, ChatRewriter.jsonTextToLegacy(prefix));
							wrapper.write(Type.STRING, ChatRewriter.jsonTextToLegacy(suffix));
						}

						if (action == 0 || action == 3 || action == 4) {
							wrapper.passthrough(Type.STRING_ARRAY); // Entities
						}
					}
				});
			}
		});


	}

	@Override
	protected void registerRewrites() {

	}
}
