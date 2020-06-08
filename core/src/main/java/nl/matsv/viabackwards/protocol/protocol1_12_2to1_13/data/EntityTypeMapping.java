package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.EntityTypeRewriter;
import us.myles.ViaVersion.util.fastutil.CollectionUtil;
import us.myles.ViaVersion.util.fastutil.IntMap;

import java.lang.reflect.Field;

public class EntityTypeMapping {
    private static final IntMap TYPES = CollectionUtil.createIntMap();

    static {
        try {
            Field field = EntityTypeRewriter.class.getDeclaredField("ENTITY_TYPES");
            field.setAccessible(true);
            IntMap entityTypes = (IntMap) field.get(null);
            entityTypes.getMap().forEach((type1_12, type1_13) -> EntityTypeMapping.TYPES.put(type1_13, type1_12));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static int getOldId(int type1_13) {
        return TYPES.get(type1_13);
    }
}
