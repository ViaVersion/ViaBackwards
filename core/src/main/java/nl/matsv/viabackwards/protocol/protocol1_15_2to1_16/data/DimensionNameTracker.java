package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

public class DimensionNameTracker extends StoredObject {
    private String dimensionName;

    public DimensionNameTracker(UserConnection user) {
        super(user);
    }

    public String getDimensionName() {
        return dimensionName;
    }

    public void setDimensionName(String dimensionName) {
        this.dimensionName = dimensionName;
    }
}
