package nl.matsv.viabackwards.api.rewriters;

import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rewriter to handle the addition of new enchantments.
 */
public class EnchantmentRewriter {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final ItemRewriter itemRewriter;
    private final boolean jsonFormat;

    public EnchantmentRewriter(ItemRewriter itemRewriter, boolean jsonFormat) {
        this.itemRewriter = itemRewriter;
        this.jsonFormat = jsonFormat;
    }

    public EnchantmentRewriter(ItemRewriter itemRewriter) {
        this(itemRewriter, true);
    }

    public void registerEnchantment(String key, String replacementLore) {
        enchantmentMappings.put(key, replacementLore);
    }

    public void handleToClient(Item item) {
        CompoundTag tag = item.getTag();
        if (tag == null) return;

        if (tag.get("Enchantments") instanceof ListTag) {
            rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.get("StoredEnchantments") instanceof ListTag) {
            rewriteEnchantmentsToClient(tag, true);
        }
    }

    public void handleToServer(Item item) {
        CompoundTag tag = item.getTag();
        if (tag == null) return;

        if (tag.contains(itemRewriter.getNbtTagName() + "|Enchantments")) {
            rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.contains(itemRewriter.getNbtTagName() + "|StoredEnchantments")) {
            rewriteEnchantmentsToServer(tag, true);
        }
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        List<Tag> loreToAdd = new ArrayList<>();
        boolean changed = false;

        Iterator<Tag> iterator = enchantments.iterator();
        while (iterator.hasNext()) {
            CompoundTag enchantmentEntry = (CompoundTag) iterator.next();
            StringTag idTag = enchantmentEntry.get("id");
            if (idTag == null) continue;

            String enchantmentId = idTag.getValue();
            String remappedName = enchantmentMappings.get(enchantmentId);
            if (remappedName != null) {
                if (!changed) {
                    // Backup original before doing modifications
                    itemRewriter.saveListTag(tag, enchantments);
                    changed = true;
                }

                iterator.remove();

                Number level = (Number) enchantmentEntry.get("lvl").getValue();
                String loreValue = remappedName + " " + getRomanNumber(level.intValue());
                if (jsonFormat) {
                    loreValue = ChatRewriter.legacyTextToJsonString(loreValue);
                }

                loreToAdd.add(new StringTag("", loreValue));
            }
        }

        if (!loreToAdd.isEmpty()) {
            // Add dummy enchant for the glow effect if there are no actual enchantments left
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag("");
                dummyEnchantment.put(new StringTag("id", ""));
                dummyEnchantment.put(new ShortTag("lvl", (short) 0));
                enchantments.add(dummyEnchantment);
            }

            CompoundTag display = tag.get("display");
            if (display == null) {
                tag.put(display = new CompoundTag("display"));
            }

            ListTag loreTag = display.get("Lore");
            if (loreTag == null) {
                display.put(loreTag = new ListTag("Lore", StringTag.class));
            } else {
                // Save original lore
                itemRewriter.saveListTag(display, loreTag);
            }

            loreToAdd.addAll(loreTag.getValue());
            loreTag.setValue(loreToAdd);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        // Just restore the original tag ig present (lore is always restored in the item rewriter)
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        itemRewriter.restoreListTag(tag, key);
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
