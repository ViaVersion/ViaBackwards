package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data;

public class ParticleMapping {

    public static int getOldId(int newId) {
        switch (newId) {
            case 58: // dripping honey -> dripping lava
                return 9;
            case 59: // falling honey -> falling lava
                return 10;
            case 60: // landing honey -> landing lava
                return 11;
            case 61: // falling nectar -> falling water
                return 13;
            default:
                return newId;
        }
    }
}
