package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage;

import nl.matsv.viabackwards.api.entities.storage.object.PlayerPositionStorage;
import us.myles.ViaVersion.api.data.UserConnection;

public class PlayerPositionStorage1_13 extends PlayerPositionStorage {

    private int entityId;

    public PlayerPositionStorage1_13(UserConnection user) {
        super(user);
    }

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
    }
}
