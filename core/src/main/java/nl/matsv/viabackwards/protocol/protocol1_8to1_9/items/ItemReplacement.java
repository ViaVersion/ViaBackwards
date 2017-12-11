package nl.matsv.viabackwards.protocol.protocol1_8to1_9.items;

import nl.matsv.viabackwards.protocol.protocol1_8to1_9.chunks.BlockStorage;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

public enum ItemReplacement {
	END_ROD(198, 85, "End Rod"),
	CHORUS_PLANT(199, 0, 35, 10, "Chorus Plant"),
	CHORUS_FLOWER(200, 0, 35, 2, "Chorus Flower"),
	PURPUR_BLOCK(201, 155, "Purpur Block"),
	PURPUR_PILLAR(202, 0, 155, 2, "Purpur Pillar"),
	PURPUR_STAIRS(203, 156, "Purpur Stairs"),
	PURPUR_DOUBLE_SLAB(204, 0, 43, 7, "Purpur Double Slab"),
	PURPUR_SLAB(205, 0, 44, 7, "Purpur Slab"),
	ENDSTONE_BRICKS(206, 121, "Endstone Bricks"),
	BEETROOT_BLOCK(207, 141, "Beetroot Block"),
	GRASS_PATH(208, 2, "Grass Path"),
	END_GATEWAY(209, 90, "End Gateway"),
	REPEATING_COMMAND_BLOCK(210, 137, "Repeating Command Block"),
	CHAIN_COMMAND_BLOCK(211, 137, "Chain Command Block"),
	FROSTED_ICE(212, 79, "Frosted Ice"),
	NETHER_WART_BLOCK(214, 87, "Nether Wart Block"),
	RED_NETHER_BRICK(215, 112, "Red Nether Brick"),
	STRUCTURE_VOID(217, 166, "Structure Void"),
	STRUCTURE_BLOCK(255, 137, "Structure Block"),
	DRAGON_HEAD(397, 5, 397, 0, "Dragon Head"),
	BEETROOT(434, 391, "Beetroot"),
	BEETROOT_SOUP(436, 282, "Beetroot Soup"),
	BEETROOT_SEEDS(435, 361, "Beetroot Seeds"),
	CHORUS_FRUIT(432, 392, "Chorus Fruit"),
	POPPED_CHORUS_FRUIT(433, 393, "Popped Chorus Fruit"),
	DRAGONS_BREATH(437, 373, "Dragons Breath"),
	ELYTRA(443, 299, "Elytra"),
	END_CRYSTAL(426, 410, "End Crystal"),
	LINGERING_POTION(441, 438, "Lingering Potion"),
	SHIELD(442, 425, "Shield"),
	SPECTRAL_ARROW(439, 262, "Spectral Arrow"),
	TIPPED_ARROW(440, 262, "Tipped Arrow"),
	;

	private int oldId, replacementId, oldData, replacementData;
	private String name, resetName, bracketName;

	ItemReplacement(int oldId, int replacementId) {
		this(oldId, replacementId, null);
	}

	ItemReplacement(int oldId, int replacementId, String name) {
		this(oldId, -1, replacementId, -1, name);
	}

	ItemReplacement(int oldId, int oldData, int replacementId, int replacementData) {
		this(oldId, -1, replacementId, -1, null);
	}

	ItemReplacement(int oldId, int oldData, int replacementId, int replacementData, String name) {
		this.oldId = oldId;
		this.oldData = oldData;
		this.replacementId = replacementId;
		this.replacementData = replacementData;
		this.name = name;
		this.resetName = "§r" + name;
		this.bracketName = " §r§7(" + name + "§r§7)";
	}

	public static BlockStorage.BlockState replaceBlock(BlockStorage.BlockState block) {
		for (ItemReplacement replacement : ItemReplacement.values()) {
			if (replacement.oldId==block.getId() && (replacement.oldData==-1 || replacement.oldData==block.getData())) {
				return new BlockStorage.BlockState(replacement.replacementId, replacement.replacementData==-1 ? block.getData() : replacement.replacementData);
			}
		}
		return block;
	}

	public static ItemReplacement findReplacement(Item oldItem) {
		if (oldItem==null) return null;
		for (ItemReplacement replacement : ItemReplacement.values()) {
			if (replacement.canReplace(oldItem)) return replacement;
		}
		return null;
	}

	public static void toClient(Item item) {
		if (item==null) return;
		ItemReplacement replacement = findReplacement(item);
		if (replacement!=null) replacement.toClientInternal(item);
	}

	public void toClientInternal(Item item) {
		if (item==null) return;
		item.setId((short)replacementId);
		if (replacementData!=-1) item.setData((short)replacementData);
		if (name!=null) {
			CompoundTag compoundTag = item.getTag()==null ? new CompoundTag("") : item.getTag();
			if (!compoundTag.contains("display")) compoundTag.put(new CompoundTag("display"));
			CompoundTag display = compoundTag.get("display");
			if (display.contains("Name")) {
				StringTag name = display.get("Name");
				if (!name.getValue().equals(resetName) && !name.getValue().endsWith(bracketName))
					name.setValue(name.getValue() + bracketName);
			} else {
				display.put(new StringTag("Name", resetName));
			}
			item.setTag(compoundTag);
		}
	}

	public boolean canReplace(Item item) {
		return item != null && item.getId() == oldId && (oldData == -1 || item.getData() == oldData);
	}

	public boolean isReplacement(Item item) {
		return item != null && item.getId() == replacementId && (replacementData == -1 || item.getData() == replacementData) && checkName(item);
	}

	private boolean checkName(Item item) {
		String name = item.getTag() != null && item.getTag().contains("display") && ((CompoundTag)item.getTag().get("display")).contains("Name") ? (String) ((CompoundTag)item.getTag().get("display")).get("Name").getValue() : null;
		return (name==null && this.name==null) || (name!=null && (name.equals(resetName) || name.endsWith(bracketName)));
	}

	public int getOldId() {
		return oldId;
	}

	public int getReplacementId() {
		return replacementId;
	}

	public int getOldData() {
		return oldData;
	}

	public int getReplacementData() {
		return replacementData;
	}

	public String getName() {
		return name;
	}
}
