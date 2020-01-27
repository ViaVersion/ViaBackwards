package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import us.myles.ViaVersion.api.entities.Entity1_13Types;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.EntityTypeRewriter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class EntityTypeMapping {
    private static final Map<Integer, Integer> entityTypes = new HashMap<>();
    private static final Map<Integer, Integer> oldEntityToOldObject = new HashMap<>();

    static {
        try {
            Field field = EntityTypeRewriter.class.getDeclaredField("entityTypes");
            field.setAccessible(true);
            Map<Integer, Integer> entityTypes = (Map<Integer, Integer>) field.get(null);
            entityTypes.forEach((type1_12, type1_13) -> EntityTypeMapping.entityTypes.put(type1_13, type1_12));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
        for (Map.Entry<Integer, Integer> newToOld : entityTypes.entrySet()) {
            Entity1_13Types.EntityType type1_13 = Entity1_13Types.getTypeFromId(newToOld.getValue(), false);
            Entity1_13Types.ObjectType object1_13 = null;
            for (Entity1_13Types.ObjectType objectType : Entity1_13Types.ObjectType.values()) {
                if (objectType.getType() == type1_13) {
                    object1_13 = objectType;
                    break;
                }
            }
            if (object1_13 != null) {
                oldEntityToOldObject.put(type1_13.getId(), object1_13.getId());
            }
        }
        for (Entity1_13Types.EntityType type : Entity1_13Types.EntityType.values()) {
            if (!entityTypes.containsValue(type.getId())) {
                entityTypes.put(type.getId(), type.getId());
            }
        }
        entityTypes.put(50, 48);  // ocelot
    }

    public static Integer getOldId(int type1_14) {
        return entityTypes.get(type1_14);
    }
}
