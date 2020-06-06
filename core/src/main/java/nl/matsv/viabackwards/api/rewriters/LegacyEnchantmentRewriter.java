package nl.matsv.viabackwards.api.rewriters;

import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.*;

public class LegacyEnchantmentRewriter {

    private final Map<Short, String> enchantmentMappings = new HashMap<>();
    private final String nbtTagName;
    private Set<Short> hideLevelForEnchants;

    public LegacyEnchantmentRewriter(String nbtTagName) {
        this.nbtTagName = nbtTagName;
    }

    public void registerEnchantment(int id, String replacementLore) {
        enchantmentMappings.put((short) id, replacementLore);
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag enchantments = tag.get(key);
        ListTag remappedEnchantments = new ListTag(nbtTagName + "|" + key, CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        for (Tag enchantmentEntry : enchantments.clone()) {
            Short newId = (Short) ((CompoundTag) enchantmentEntry).get("id").getValue();
            String enchantmentName = enchantmentMappings.get(newId);
            if (enchantmentName != null) {
                enchantments.remove(enchantmentEntry);
                Number level = (Number) ((CompoundTag) enchantmentEntry).get("lvl").getValue();
                if (hideLevelForEnchants != null && hideLevelForEnchants.contains(newId)) {
                    lore.add(new StringTag("", enchantmentName));
                } else {
                    lore.add(new StringTag("", enchantmentName + " " + EnchantmentRewriter.getRomanNumber(level.shortValue())));
                }
                remappedEnchantments.add(enchantmentEntry);
            }
        }
        if (!lore.isEmpty()) {
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag("");
                dummyEnchantment.put(new ShortTag("id", (short) 0));
                dummyEnchantment.put(new ShortTag("lvl", (short) 0));
                enchantments.add(dummyEnchantment);

                tag.put(new ByteTag(nbtTagName + "|dummyEnchant"));

                IntTag hideFlags = tag.get("HideFlags");
                if (hideFlags == null) {
                    hideFlags = new IntTag("HideFlags");
                } else {
                    tag.put(new IntTag(nbtTagName + "|oldHideFlags", hideFlags.getValue()));
                }

                int flags = hideFlags.getValue() | 1;
                hideFlags.setValue(flags);
                tag.put(hideFlags);
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
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag remappedEnchantments = tag.get(nbtTagName + "|" + key);
        ListTag enchantments = tag.get(key);
        if (enchantments == null) {
            enchantments = new ListTag(key, CompoundTag.class);
        }

        if (!storedEnchant && tag.remove(nbtTagName + "|dummyEnchant") != null) {
            for (Tag enchantment : enchantments.clone()) {
                Short id = (Short) ((CompoundTag) enchantment).get("id").getValue();
                Short level = (Short) ((CompoundTag) enchantment).get("lvl").getValue();
                if (id == 0 && level == 0) {
                    enchantments.remove(enchantment);
                }
            }

            IntTag hideFlags = tag.remove(nbtTagName + "|oldHideFlags");
            if (hideFlags != null) {
                tag.put(new IntTag("HideFlags", hideFlags.getValue()));
            } else {
                tag.remove("HideFlags");
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

    public void setHideLevelForEnchants(int... enchants) {
        this.hideLevelForEnchants = new HashSet<>();
        for (int enchant : enchants) {
            hideLevelForEnchants.add((short) enchant);
        }
    }
}
