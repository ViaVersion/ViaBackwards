package nl.matsv.viabackwards.protocol.protocol1_8to1_9.chunks;

/*
 *https://github.com/Steveice10/MCProtocolLib/blob/4ed72deb75f2acb0a81d641717b7b8074730f701/src/main/java/org/spacehq/mc/protocol/data/game/chunk/BlockStorage.java#L42
 */

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import us.myles.ViaVersion.api.type.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlockStorage {

	public static class BlockState {
		private int id;
		private int data;

		public BlockState(int id, int data) {
			this.id = id;
			this.data = data;
		}

		public int getId() {
			return this.id;
		}

		public int getData() {
			return this.data;
		}
	}

	private static final BlockState AIR = new BlockState(0, 0);

	private int bitsPerEntry;

	private List<BlockState> states;
	private FlexibleStorage storage;

	public BlockStorage() {
		this.bitsPerEntry = 4;

		this.states = new ArrayList<>();
		this.states.add(AIR);

		this.storage = new FlexibleStorage(this.bitsPerEntry, 4096);
	}

	public BlockStorage(ByteBuf in) throws Exception {

		this.bitsPerEntry = in.readUnsignedByte();

		this.states = new ArrayList<>();
		int stateCount = Type.VAR_INT.read(in);
		for (int i = 0; i < stateCount; i++) {
			int rawId = Type.VAR_INT.read(in);
			this.states.add(new BlockState(rawId >> 4, rawId & 0xF));
		}

		long[] data = new long[Type.VAR_INT.read(in)];
		for (int i = 0; i<data.length; i++) {
			data[i] = Type.LONG.read(in);
		}

		this.storage = new FlexibleStorage(this.bitsPerEntry, data);
	}

	public void write(ByteBuf out) throws Exception {
		out.writeByte(this.bitsPerEntry);

		Type.VAR_INT.write(out, this.states.size());
		for (BlockState state : this.states) {
			Type.VAR_INT.write(out, (state.getId() << 4) | (state.getData() & 0xF));
		}

		long[] data = this.storage.getData();
		Type.VAR_INT.write(out, data.length);
		for (long l : data) {
			Type.LONG.write(out, l);
		}
	}

	public byte[] write() throws Exception {
		ByteBuf buf = Unpooled.buffer();
		write(buf);
		byte[] data = new byte[buf.readableBytes()];
		buf.readBytes(data);
		buf.release();
		return data;
	}

	public int getBitsPerEntry() {
		return this.bitsPerEntry;
	}

	public List<BlockState> getStates() {
		return Collections.unmodifiableList(this.states);
	}

	public FlexibleStorage getStorage() {
		return this.storage;
	}

	public BlockState get(int x, int y, int z) {
		int id = this.storage.get(index(x, y, z));
		return this.bitsPerEntry <= 8 ? (id >= 0 && id < this.states.size() ? this.states.get(id) : AIR) : rawToState(id);
	}

	public void set(int x, int y, int z, BlockState state) {
		int id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : stateToRaw(state);
		if (id == -1) {
			this.states.add(state);
			if (this.states.size() > 1 << this.bitsPerEntry) {
				this.bitsPerEntry++;

				List<BlockState> oldStates = this.states;
				if (this.bitsPerEntry > 8) {
					oldStates = new ArrayList<>(this.states);
					this.states.clear();
					this.bitsPerEntry = 13;
				}

				FlexibleStorage oldStorage = this.storage;
				this.storage = new FlexibleStorage(this.bitsPerEntry, this.storage.getSize());
				for (int index = 0; index < this.storage.getSize(); index++) {
					this.storage.set(index, this.bitsPerEntry <= 8 ? oldStorage.get(index) : stateToRaw(oldStates.get(index)));
				}
			}

			id = this.bitsPerEntry <= 8 ? this.states.indexOf(state) : stateToRaw(state);
		}

		this.storage.set(index(x, y, z), id);
	}

	public boolean isEmpty() {
		for (int index = 0; index < this.storage.getSize(); index++) {
			if (this.storage.get(index) != 0) {
				return false;
			}
		}

		return true;
	}

	private static int index(int x, int y, int z) {
		return y << 8 | z << 4 | x;
	}

	public static BlockState rawToState(int raw) {
		return new BlockState(raw >> 4, raw & 0xF);
	}

	public static int stateToRaw(BlockState state) {
		return (state.getId() << 4) | (state.getData() & 0xF);
	}
}
