package nl.matsv.viabackwards.protocol.protocol1_8to1_9.storage;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

public class BlockPlaceDestroyTracker extends StoredObject {
	private long blockPlaced, lastMining;
	boolean mining;

	public BlockPlaceDestroyTracker(UserConnection user) {
		super(user);
	}

	public long getBlockPlaced() {
		return blockPlaced;
	}

	public void place() {
		blockPlaced = System.currentTimeMillis();
	}

	public boolean isMining() {
		return mining;
	}

	public void setMining(boolean mining) {
		this.mining = mining && getUser().get(EntityTracker.class).getPlayerGamemode()!=1;
		lastMining = System.currentTimeMillis();
	}

	public long getLastMining() {
		return lastMining;
	}

	public void updateMinig() {
		if (this.mining) {
			lastMining = System.currentTimeMillis();
		}
	}
}
