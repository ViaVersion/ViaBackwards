package nl.matsv.viabackwards.protocol.protocol1_8to1_9.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.CustomByteType;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.types.Chunk1_9_1_2Type;

public class ChunkPacketTransformer {
	public static void transformChunk(PacketWrapper packetWrapper) throws Exception {
		ClientWorld world = packetWrapper.user().get(ClientWorld.class);
		Chunk1_8to1_9 chunk;
		int chunkX, chunkZ, primaryBitMask;
		boolean groundUp;
		if (world!=null) {
			Chunk chunk1_9 = packetWrapper.read(new Chunk1_9_1_2Type(world));
			chunkX = chunk1_9.getX();
			chunkZ = chunk1_9.getZ();
			boolean skyLight = world.getEnvironment()==Environment.NORMAL;
			primaryBitMask = chunk1_9.getBitmask();

			ByteBuf data = Unpooled.buffer();
			for (int i = 0; i < chunk1_9.getSections().length; i++) {
				if ((primaryBitMask & 1 << i) != 0) {
					ChunkSection section = chunk1_9.getSections()[i];
					section.writeBlocks(data);
					section.writeBlockLight(data);
					if (skyLight) section.writeSkyLight(data);
				}
			}
			byte[] rawdata = new byte[data.readableBytes()];
			data.readBytes(rawdata);
			data.release();

			chunk = new Chunk1_8to1_9(rawdata, primaryBitMask, skyLight, groundUp = chunk1_9.isGroundUp(), chunk1_9.getBiomeData());
		} else {
			chunkX = packetWrapper.read(Type.INT);
			chunkZ = packetWrapper.read(Type.INT);
			groundUp = packetWrapper.read(Type.BOOLEAN);
			primaryBitMask = packetWrapper.read(Type.VAR_INT);
			int size = packetWrapper.read(Type.VAR_INT);
			if (groundUp) size -= 256;
			CustomByteType customByteType = new CustomByteType(size);
			byte[] data = packetWrapper.read(customByteType);
			byte[] biomes = groundUp ? packetWrapper.read(new CustomByteType(256)) : new byte[0];

			chunk = new Chunk1_8to1_9(data, primaryBitMask, true, groundUp, biomes);
		}

		packetWrapper.write(Type.INT, chunkX);
		packetWrapper.write(Type.INT, chunkZ);
		packetWrapper.write(Type.BOOLEAN, groundUp);
		packetWrapper.write(Type.UNSIGNED_SHORT, primaryBitMask);
		byte[] finaldata = chunk.get1_8Data();
		packetWrapper.write(Type.VAR_INT, finaldata.length);
		packetWrapper.write(new CustomByteType(finaldata.length), finaldata);
	}
}
