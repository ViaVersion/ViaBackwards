package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.NamedSoundMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.SoundMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets1_13 extends Rewriter<Protocol1_12_2To1_13> {
	@Override
	protected void registerPackets(Protocol1_12_2To1_13 protocol) {

		//Named Sound Event
		protocol.out(State.PLAY, 0x1A, 0x19, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.STRING);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						String newSound = wrapper.get(Type.STRING, 0);
						String oldSound = NamedSoundMapping.getOldId(newSound);
						if (oldSound != null) {
							wrapper.set(Type.STRING, 0, oldSound);
						}
					}
				});
			}
		});

		//Sound Effect
		protocol.out(State.PLAY, 0x4D, 0x49, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						int newSound = wrapper.get(Type.VAR_INT, 0);
						int oldSound = SoundMapping.getOldSound(newSound);
						if (oldSound == -1) {
							wrapper.cancel();
						} else {
							wrapper.set(Type.VAR_INT, 0, oldSound);
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
