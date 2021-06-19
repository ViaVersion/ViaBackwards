/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TabCompleteStorage implements StorableObject {
    private final Map<UUID, String> usernames = new HashMap<>();
    private final Set<String> commands = new HashSet<>();
    private int lastId;
    private String lastRequest;
    private boolean lastAssumeCommand;

    public Map<UUID, String> usernames() {
        return usernames;
    }

    public Set<String> commands() {
        return commands;
    }

    public int lastId() {
        return lastId;
    }

    public void setLastId(final int lastId) {
        this.lastId = lastId;
    }

    public String lastRequest() {
        return lastRequest;
    }

    public void setLastRequest(String lastRequest) {
        this.lastRequest = lastRequest;
    }

    public boolean isLastAssumeCommand() {
        return lastAssumeCommand;
    }

    public void setLastAssumeCommand(boolean lastAssumeCommand) {
        this.lastAssumeCommand = lastAssumeCommand;
    }
}
