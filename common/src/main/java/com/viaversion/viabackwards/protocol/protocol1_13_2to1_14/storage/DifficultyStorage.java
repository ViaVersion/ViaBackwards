package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage;

import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;

public class DifficultyStorage extends StoredObject {
	private byte difficulty;

	public DifficultyStorage(UserConnection user) {
		super(user);
	}

	public byte getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(byte difficulty) {
		this.difficulty = difficulty;
	}
}
