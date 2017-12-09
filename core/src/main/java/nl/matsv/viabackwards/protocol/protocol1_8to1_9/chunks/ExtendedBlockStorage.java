package nl.matsv.viabackwards.protocol.protocol1_8to1_9.chunks;

import us.myles.ViaVersion.api.minecraft.chunks.NibbleArray;

public class ExtendedBlockStorage {
	private int yBase;
	private BlockStorage blockStorage;
	private NibbleArray blocklightArray;
	private NibbleArray skylightArray;

	public ExtendedBlockStorage(int paramInt, boolean paramBoolean) {
		this.yBase = paramInt;
		this.blocklightArray = new NibbleArray(4096);
		if (paramBoolean) {
			this.skylightArray = new NibbleArray(4096);
		}
	}

	public int getYLocation() {
		return this.yBase;
	}

	public void setExtSkylightValue(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
		this.skylightArray.set(paramInt1, paramInt2, paramInt3, paramInt4);
	}

	public int getExtSkylightValue(int paramInt1, int paramInt2, int paramInt3) {
		return this.skylightArray.get(paramInt1, paramInt2, paramInt3);
	}

	public void setExtBlocklightValue(int paramInt1, int paramInt2, int paramInt3, int paramInt4) {
		this.blocklightArray.set(paramInt1, paramInt2, paramInt3, paramInt4);
	}

	public int getExtBlocklightValue(int paramInt1, int paramInt2, int paramInt3) {
		return this.blocklightArray.get(paramInt1, paramInt2, paramInt3);
	}

	public NibbleArray getBlocklightArray() {
		return this.blocklightArray;
	}

	public BlockStorage getBlockStorage() {
		return blockStorage;
	}

	public void setBlockStorage(BlockStorage blockStorage) {
		this.blockStorage = blockStorage;
	}

	public boolean isEmpty() {
		return this.blockStorage==null;
	}

	public NibbleArray getSkylightArray() {
		return this.skylightArray;
	}

	public void setBlocklightArray(NibbleArray paramNibbleArray) {
		this.blocklightArray = paramNibbleArray;
	}

	public void setSkylightArray(NibbleArray paramNibbleArray) {
		this.skylightArray = paramNibbleArray;
	}
}
