package nl.matsv.viabackwards.protocol.protocol1_8to1_9.metadata;

import us.myles.ViaVersion.api.Pair;
import us.myles.ViaVersion.api.entities.Entity1_10Types;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.metadata.MetaIndex;

import java.util.HashMap;
import java.util.Optional;

public class MetaIndex1_8to1_9 {

	private static final HashMap<Pair<Entity1_10Types.EntityType, Integer>, MetaIndex> metadataRewrites = new HashMap<>();

	static {
		for (MetaIndex index : MetaIndex.values())
			metadataRewrites.put(new Pair<>(index.getClazz(), index.getNewIndex()), index);
	}

	private static Optional<MetaIndex> getIndex(Entity1_10Types.EntityType type, int index) {
		Pair pair = new Pair<>(type, index);
		if (metadataRewrites.containsKey(pair)) {
			return Optional.of(metadataRewrites.get(pair));
		}

		return Optional.empty();
	}

	public static MetaIndex searchIndex(Entity1_10Types.EntityType type, int index) {
		Entity1_10Types.EntityType currentType = type;
		do {
			Optional<MetaIndex> optMeta = getIndex(currentType, index);

			if (optMeta.isPresent()) {
				return optMeta.get();
			}

			currentType = currentType.getParent();
		} while (currentType != null);

		return null;
	}

}
