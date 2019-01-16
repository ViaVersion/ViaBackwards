package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import java.util.HashMap;
import java.util.Map;

public class PaintingMapping {
	private static Map<Integer, String> paintings = new HashMap<>();

	public static void init() {
		add("kebab");
		add("aztec");
		add("alban");
		add("aztec2");
		add("bomb");
		add("plant");
		add("wasteland");
		add("pool");
		add("courbet");
		add("sea");
		add("sunset");
		add("creebet");
		add("wanderer");
		add("graham");
		add("match");
		add("bust");
		add("stage");
		add("void");
		add("skullandroses");
		add("wither");
		add("fighters");
		add("pointer");
		add("pigscene");
		add("burningskull");
		add("skeleton");
		add("donkeykong");
	}

	private static void add(String motive) {
		paintings.put(paintings.size(), motive);
	}

	public static String getStringId(int id) {
		return paintings.getOrDefault(id, "kebab");
	}
}
