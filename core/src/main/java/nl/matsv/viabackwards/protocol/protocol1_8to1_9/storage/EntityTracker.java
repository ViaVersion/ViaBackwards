package nl.matsv.viabackwards.protocol.protocol1_8to1_9.storage;

import nl.matsv.viabackwards.protocol.protocol1_8to1_9.metadata.MetadataRewriter;
import nl.matsv.viabackwards.protocol.protocol1_8to1_9.Protocol1_8TO1_9;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_10Types;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_8;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityTracker extends StoredObject {
	private final Map<Integer, ArrayList<Integer>> vehicleMap = new ConcurrentHashMap();
	private final Map<Integer, Entity1_10Types.EntityType> clientEntityTypes = new ConcurrentHashMap();
	private final Map<Integer, List<Metadata>> metadataBuffer = new ConcurrentHashMap();
	private int playerId;
	private int playerGamemode = 0;

	public EntityTracker(UserConnection user) {
		super(user);
	}

	public void setPlayerId(int entityId) {
		playerId = entityId;
	}

	public int getPlayerId() {
		return playerId;
	}

	public int getPlayerGamemode() {
		return playerGamemode;
	}

	public void setPlayerGamemode(int playerGamemode) {
		this.playerGamemode = playerGamemode;
	}

	public void removeEntity(int entityId) {
		vehicleMap.remove(entityId);
		vehicleMap.forEach((vehicle, passengers) -> passengers.remove((Integer)entityId));
		vehicleMap.entrySet().removeIf(entry -> entry.getValue().isEmpty());
		clientEntityTypes.remove(entityId);
	}

	public ArrayList<Integer> getPassengers(int entityId) {
		return vehicleMap.getOrDefault(entityId, new ArrayList<>());
	}

	public void setPassengers(int entityId, ArrayList<Integer> passengers) {
		vehicleMap.put(entityId, passengers);
	}

	public Map<Integer, Entity1_10Types.EntityType> getClientEntityTypes() {
		return this.clientEntityTypes;
	}

	public void addMetadataToBuffer(int entityID, List<Metadata> metadataList) {
		if (this.metadataBuffer.containsKey(entityID)) {
			this.metadataBuffer.get(entityID).addAll(metadataList);
		} else if (!metadataList.isEmpty()) {
			this.metadataBuffer.put(entityID, metadataList);
		}
	}

	public List<Metadata> getBufferedMetadata(int entityId) {
		return metadataBuffer.get(entityId);
	}

	public boolean isInsideVehicle(int entityId) {
		for (ArrayList<Integer> vehicle : vehicleMap.values()) {
			if (vehicle.contains(entityId)) return true;
		}
		return false;
	}

	public int getVehicle(int passenger) {
		for (Map.Entry<Integer, ArrayList<Integer>> vehicle : vehicleMap.entrySet()) {
			if (vehicle.getValue().contains(passenger)) return vehicle.getKey();
		}
		return -1;
	}

	public boolean isPassenger(int vehicle, int passenger) {
		return vehicleMap.containsKey(vehicle) && vehicleMap.get(vehicle).contains(passenger);
	}

	public void sendMetadataBuffer(int entityId) {
		if (!this.metadataBuffer.containsKey(entityId)) return;
		PacketWrapper wrapper = new PacketWrapper(0x1C, null, this.getUser());
		wrapper.write(Type.VAR_INT, entityId);
		wrapper.write(Types1_8.METADATA_LIST, this.metadataBuffer.get(entityId));
		MetadataRewriter.transform(this.getClientEntityTypes().get(entityId), this.metadataBuffer.get(entityId));
		if (!this.metadataBuffer.get(entityId).isEmpty()) {
			try {
				wrapper.send(Protocol1_8TO1_9.class);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		this.metadataBuffer.remove(entityId);
	}
}
