package nl.matsv.viabackwards.protocol.protocol1_8to1_9.items;

import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.lang.reflect.Field;
import java.util.Map;

import static us.myles.ViaVersion.protocols.protocol1_9to1_8.ItemRewriter.potionNameFromDamage;

@SuppressWarnings({"MismatchedQueryAndUpdateOfCollection", "unused"})
public class ItemRewriter {
	private static Map<String, Integer> ENTTIY_NAME_TO_ID;
	private static Map<Integer, String> ENTTIY_ID_TO_NAME;
	private static Map<String, Integer> POTION_NAME_TO_ID;
	private static Map<Integer, String> POTION_ID_TO_NAME;
	private static Map<Integer, Integer> POTION_INDEX;

	static {
		for (Field field : ItemRewriter.class.getDeclaredFields()) {
			try {
				Field other = us.myles.ViaVersion.protocols.protocol1_9to1_8.ItemRewriter.class.getDeclaredField(field.getName());
				other.setAccessible(true);
				field.setAccessible(true);
				field.set(null, other.get(null));
			} catch (Exception ignored) {}
		}
	}

	public static Item toClient(Item item) {
		if (item==null) return null;

		CompoundTag tag = item.getTag();
		if (tag==null) item.setTag(tag = new CompoundTag(""));

		CompoundTag viaVersionTag = new CompoundTag("ViaBackwards1_8to1_9");
		tag.put(viaVersionTag);

		viaVersionTag.put(new ShortTag("id", item.getId()));
		viaVersionTag.put(new ShortTag("data", item.getData()));

		CompoundTag display = tag.get("display");
		if (display!=null && display.contains("Name")) {
			viaVersionTag.put(new StringTag("displayName", (String) display.get("Name").getValue()));
		}

		if (tag.contains("AttributeModifiers")) {
			viaVersionTag.put(tag.get("AttributeModifiers").clone());
		}

		if (item.getId()==373 || item.getId()==438) {
			int data = 0;
			if (tag.contains("Potion")) {
				StringTag potion = tag.remove("Potion");
				String potionName = potion.getValue().replace("minecraft:", "");
				if (POTION_NAME_TO_ID.containsKey(potionName)) {
					data = POTION_NAME_TO_ID.get(potionName);
				}
			}

			if (item.getId()==438) {
				item.setId((short) 373);
				data += 8192;
			}

			item.setData((short)data);
		}

		if (item.getId()==383 && item.getData()==0) {
			int data = 0;
			if (tag.contains("EntityTag")) {
				CompoundTag entityTag = tag.remove("EntityTag");
				if (entityTag.contains("id")) {
					StringTag id = entityTag.get("id");
					if (ENTTIY_NAME_TO_ID.containsKey(id.getValue())) {
						data = ENTTIY_NAME_TO_ID.get(id.getValue());
					}
				}
			}

			item.setData((short)data);
		}

		ItemReplacement.toClient(item);

		if (tag.contains("AttributeModifiers")) {
			ListTag attributes = tag.get("AttributeModifiers");
			for (int i = 0; i<attributes.size(); i++) {
				CompoundTag attribute = attributes.get(i);
				String name = (String) attribute.get("AttributeName").getValue();
				if (name.equals("generic.armor") || name.equals("generic.armorToughness") || name.equals("generic.attackSpeed") || name.equals("generic.luck")) {
					attributes.remove(attribute);
					i--;
				}
			}
		}

		return item;
	}

	public static Item toServer(Item item) {
		if (item==null) return null;

		CompoundTag tag = item.getTag();

		if (item.getId()==383 && item.getData()!=0) {
			if (tag==null) item.setTag(tag = new CompoundTag(""));

			if (ENTTIY_ID_TO_NAME.containsKey((int) item.getData())) {
				CompoundTag entityTag = new CompoundTag("EntityTag");
				entityTag.put(new StringTag("id", ENTTIY_ID_TO_NAME.get((int) item.getData())));
				tag.put(entityTag);
			}

			item.setData((short)0);
		}

		if (item.getId() == 373) {
			if (tag==null) item.setTag(tag = new CompoundTag(""));

			if (item.getData() >= 16384) {
				item.setId((short)438);
				item.setData((short)(item.getData() - 8192));
			}

			String name = potionNameFromDamage(item.getData());
			tag.put(new StringTag("Potion", "minecraft:" + name));
			item.setData((short)0);
		}

		 if (tag==null || !item.getTag().contains("ViaBackwards1_8to1_9")) return item;


		CompoundTag viaVersionTag = tag.remove("ViaBackwards1_8to1_9");

		item.setId((Short) viaVersionTag.get("id").getValue());
		item.setData((Short) viaVersionTag.get("data").getValue());

		if (viaVersionTag.contains("displayName")) {
			CompoundTag display = tag.get("display");
			if (display==null) tag.put(display = new CompoundTag("display"));
			StringTag name = display.get("Name");
			if (name==null) display.put(new StringTag("Name", (String) viaVersionTag.get("displayName").getValue()));
			else name.setValue((String) viaVersionTag.get("displayName").getValue());
		} else if (tag.contains("display")) {
			((CompoundTag)tag.get("display")).remove("Name");
		}

		tag.remove("AttributeModifiers");
		if (viaVersionTag.contains("AttributeModifiers")) {
			tag.put(viaVersionTag.get("AttributeModifiers"));
		}

		return item;
	}
}
