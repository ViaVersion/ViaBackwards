package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.NamedSoundRewriter;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class NamedSoundMapping {
    private static Map<String, String> sounds = new HashMap<>();

    static {
        try {
            Field field = NamedSoundRewriter.class.getDeclaredField("oldToNew");
            field.setAccessible(true);
            Map<String, String> sounds = (Map<String, String>) field.get(null);
            sounds.forEach((sound1_12, sound1_13) -> NamedSoundMapping.sounds.put(sound1_13, sound1_12));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static String getOldId(String sound1_13) {
        return sounds.get(sound1_13);
    }
}
