package nl.matsv.viabackwards.api.rewriters;

import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantmentRewriter {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final String nbtTagName;

    public EnchantmentRewriter(final String nbtTagName) {
        this.nbtTagName = nbtTagName;
    }

    public void registerEnchantment(String key, String replacementLore) {
        enchantmentMappings.put(key, replacementLore);
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        ListTag remappedEnchantments = new ListTag(nbtTagName + "|" + key, CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        for (Tag enchantmentEntry : enchantments.clone()) {
            String newId = (String) ((CompoundTag) enchantmentEntry).get("id").getValue();
            String enchantmentName = enchantmentMappings.get(newId);
            if (enchantmentName != null) {
                enchantments.remove(enchantmentEntry);
                Number level = (Number) ((CompoundTag) enchantmentEntry).get("lvl").getValue();
                lore.add(new StringTag("", enchantmentName + " " + getRomanNumber(level.shortValue())));
                remappedEnchantments.add(enchantmentEntry);
            }
        }
        if (!lore.isEmpty()) {
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag("");
                dummyEnchantment.put(new StringTag("id", ""));
                dummyEnchantment.put(new ShortTag("lvl", (short) 0));
                enchantments.add(dummyEnchantment);

                tag.put(new ByteTag(nbtTagName + "|dummyEnchant"));
            }

            tag.put(remappedEnchantments);

            CompoundTag display = tag.get("display");
            if (display == null) {
                tag.put(display = new CompoundTag("display"));
            }
            ListTag loreTag = display.get("Lore");
            if (loreTag == null) {
                display.put(loreTag = new ListTag("Lore", StringTag.class));
            }

            lore.addAll(loreTag.getValue());
            loreTag.setValue(lore);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag remappedEnchantments = tag.get(nbtTagName + "|" + key);
        ListTag enchantments = tag.contains(key) ? tag.get(key) : new ListTag(key, CompoundTag.class);
        if (!storedEnchant && tag.contains(nbtTagName + "|dummyEnchant")) {
            tag.remove(nbtTagName + "|dummyEnchant");

            for (Tag enchantment : enchantments.clone()) {
                String id = (String) ((CompoundTag) enchantment).get("id").getValue();
                if (id.isEmpty()) {
                    enchantments.remove(enchantment);
                }
            }
        }

        CompoundTag display = tag.get("display");
        // A few null checks just to be safe, though they shouldn't actually be
        ListTag lore = display != null ? display.get("Lore") : null;
        for (Tag enchantment : remappedEnchantments.clone()) {
            enchantments.add(enchantment);
            if (lore != null && lore.size() != 0) {
                lore.remove(lore.get(0));
            }
        }
        if (lore != null && lore.size() == 0) {
            display.remove("Lore");
            if (display.isEmpty()) {
                tag.remove("display");
            }
        }
        tag.put(enchantments);
        tag.remove(remappedEnchantments.getName());
    }

    public static String getRomanNumber(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return Integer.toString(number);
        }
    }
}
