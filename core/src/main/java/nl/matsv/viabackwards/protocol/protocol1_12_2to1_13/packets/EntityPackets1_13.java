package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class EntityPackets1_13 extends EntityRewriter<Protocol1_12_2To1_13> {

	@Override
	protected void registerPackets(Protocol1_12_2To1_13 protocol) {

		//Spawn Painting
		protocol.out(State.PLAY, 0x04, 0x04, new PacketRemapper() {
			@Override
			public void registerMap() {
				map(Type.VAR_INT);
				map(Type.UUID);
				handler(new PacketHandler() {
					@Override
					public void handle(PacketWrapper wrapper) throws Exception {
						int motive = wrapper.read(Type.VAR_INT);
						String title = PaintingMapping.getStringId(motive);
						wrapper.write(Type.STRING, title);
					}
				});
			}
		});

	}

	@Override
	protected void registerRewrites() {

	}
}
