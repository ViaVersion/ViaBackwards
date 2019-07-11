package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.EntityTypeRewriter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntityTypeMapping {
    private static Map<Integer, Integer> entityTypes = new HashMap<>();

    static {
        try {
            Field field = EntityTypeRewriter.class.getDeclaredField("entityTypes");
            field.setAccessible(true);
            Map<Integer, Integer> entityTypes = (Map<Integer, Integer>) field.get(null);
            entityTypes.forEach((type1_12, type1_13) -> EntityTypeMapping.entityTypes.put(type1_13, type1_12));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static Optional<Integer> getOldId(int type1_13) {
        return Optional.ofNullable(entityTypes.get(type1_13));
    }
}
