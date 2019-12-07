package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import nl.matsv.viabackwards.api.entities.storage.EntityStorage;

public class EntityPositionStorage extends EntityStorage {

    private double x;
    private double y;
    private double z;

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setCoordinates(double x, double y, double z, boolean relative) {
        if (relative) {
            this.x += x;
            this.y += y;
            this.z += z;
        } else {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
