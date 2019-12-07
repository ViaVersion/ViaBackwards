package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data;

import us.myles.ViaVersion.api.entities.Entity1_14Types;

public class EntityTypeMapping {

    // There's only the bee, so not much to do here
    public static int getOldEntityId(int entityId) {
        if (entityId == 4) return Entity1_14Types.EntityType.PUFFER_FISH.getId(); // Flying pufferfish!
        return entityId >= 5 ? entityId - 1 : entityId;
    }
}
