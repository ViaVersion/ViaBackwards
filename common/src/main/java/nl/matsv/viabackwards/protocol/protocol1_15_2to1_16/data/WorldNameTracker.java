package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

public class WorldNameTracker extends StoredObject {
    private String worldName;

    public WorldNameTracker(UserConnection user) {
        super(user);
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
}
