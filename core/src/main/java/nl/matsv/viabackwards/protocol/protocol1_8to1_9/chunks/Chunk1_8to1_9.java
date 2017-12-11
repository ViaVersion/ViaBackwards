package nl.matsv.viabackwards.protocol.protocol1_8to1_9.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import nl.matsv.viabackwards.protocol.protocol1_8to1_9.items.ItemReplacement;
import us.myles.ViaVersion.api.minecraft.chunks.NibbleArray;

public class Chunk1_8to1_9 {
	public ExtendedBlockStorage[] storageArrays = new ExtendedBlockStorage[16];
	public byte[] blockBiomeArray;
	private boolean skyLight;
	private int primaryBitMask;
	private boolean groundUp;

	public Chunk1_8to1_9(byte[] data, int primaryBitMask, boolean skyLight, boolean groundUp, byte[] blockBiomeArray) {
		this.blockBiomeArray = blockBiomeArray;
		this.primaryBitMask = primaryBitMask;
		this.skyLight = skyLight;
		this.groundUp = groundUp;
		int dataSize = 0;
		for (int i = 0; i < this.storageArrays.length; i++) {
			if ((primaryBitMask & 1 << i) != 0) {
				if (this.storageArrays[i] == null) this.storageArrays[i] = new ExtendedBlockStorage(i << 4, skyLight);

				ByteBuf buf = Unpooled.copiedBuffer(data, dataSize, data.length-dataSize);
				try {
					this.storageArrays[i].setBlockStorage(new BlockStorage(buf));
				} catch (Exception ex) {ex.printStackTrace();}
				dataSize += buf.readerIndex();
				buf.release();

				byte[] blockLight = this.storageArrays[i].getBlocklightArray().getHandle();
				System.arraycopy(data, dataSize, blockLight, 0, blockLight.length);
				dataSize += blockLight.length;
				if (skyLight) {
					byte[] skyLightArray = this.storageArrays[i].getSkylightArray().getHandle();
					System.arraycopy(data, dataSize, skyLightArray, 0, skyLightArray.length);
					dataSize += skyLightArray.length;
				}
			} else if (this.storageArrays[i] != null && groundUp) {
				this.storageArrays[i] = null;
			}
		}
	}

	public byte[] get1_8Data() {
		int finalsize = 0;
		int columns = Integer.bitCount(this.primaryBitMask);
		byte[] buffer = new byte[columns * 10240 + (this.skyLight ? columns * 2048 : 0) + 256];

		for (int i = 0; i < storageArrays.length; ++i) {
			if (storageArrays[i] != null && (this.primaryBitMask & 1 << i) != 0 && (!this.groundUp || !storageArrays[i].isEmpty())) {
				BlockStorage blockStorage = this.storageArrays[i].getBlockStorage();

				for (int ind = 0; ind < 4096; ++ind) {
					int px = ind & 15;
					int py = ind >> 8 & 15;
					int pz = ind >> 4 & 15;
					BlockStorage.BlockState state = blockStorage.get(px, py, pz);
					state = ItemReplacement.replaceBlock(state);
					int id = state.getId();
					int data = state.getData();

					char val = (char) (id << 4 | data);
					buffer[finalsize++] = (byte) (val & 255);
					buffer[finalsize++] = (byte) (val >> 8 & 255);
				}
			}
		}

		for (int i = 0; i < storageArrays.length; ++i) {
			if (storageArrays[i] != null && (this.primaryBitMask & 1 << i) != 0 && (!this.groundUp || !storageArrays[i].isEmpty())) {
				NibbleArray nibblearray = storageArrays[i].getBlocklightArray();
				System.arraycopy(nibblearray.getHandle(), 0, buffer, finalsize, nibblearray.getHandle().length);
				finalsize += nibblearray.getHandle().length;
			}
		}

		if (this.skyLight) {
			for (int i = 0; i < storageArrays.length; ++i) {
				if (storageArrays[i] != null && (this.primaryBitMask & 1 << i) != 0 && (!this.groundUp || !storageArrays[i].isEmpty())) {
					NibbleArray nibblearray = storageArrays[i].getSkylightArray();
					System.arraycopy(nibblearray.getHandle(), 0, buffer, finalsize, nibblearray.getHandle().length);
					finalsize += nibblearray.getHandle().length;
				}
			}
		}

		if (this.groundUp) {
			System.arraycopy(blockBiomeArray, 0, buffer, finalsize, blockBiomeArray.length);
			finalsize += blockBiomeArray.length;
		}

		byte[] finaldata = new byte[finalsize];
		System.arraycopy(buffer, 0, finaldata, 0, finalsize);

		return finaldata;
	}
}
