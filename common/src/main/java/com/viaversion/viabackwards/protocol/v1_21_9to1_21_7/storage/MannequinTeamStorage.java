/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage;

import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;

import java.util.*;

public class MannequinTeamStorage {
  private final Map<UUID, ScoreboardEntity> scoreboardEntities = new HashMap<>();
  private final Map<String, List<ScoreboardEntity>> entitiesByTeam = new HashMap<>();

  /**
   * Called when the server changes the team for an entity.
   *
   * <p>Attempts to find known mannequins by the UUID in the entry. If not found,
   * it stores the team association in case a mannequin with that UUID does spawn.
   *
   * @param newTeam the new team, or {@code null} to leave the team
   * @param entry the raw entry from the server
   * @return the mannequin/player name for this entry if it should be rewritten, {@code null} otherwise
   */
  public String changeTeamAndRewrite(String newTeam, String entry) {
    UUID uuid;
    // Only consider entity UUIDs, the server uses those to assign teams to mannequins,
    // but we need player names for scoreboard teams
    if (entry.length() != 36) {
      return null;
    }
    try {
      uuid = UUID.fromString(entry);
    } catch (IllegalArgumentException ignored) {
      return null;
    }

    ScoreboardEntity entity = scoreboardEntities.get(uuid);
    if (entity == null) {
      if (newTeam == null) {
        // Untracked entity removed from team, we don't care
        return null;
      }
      entity = new ScoreboardEntity(uuid);
      scoreboardEntities.put(uuid, entity);
    }

    changeServerTeam(entity, newTeam);

    if (newTeam == null && !entity.isMannequin()) {
      // Remove the team registration but only forget the entity
      // now if not a mannequin. Keep entry until the mannequin is
      // despawned, in case server sends another team
      scoreboardEntities.remove(uuid);
    }

    return entity.mannequinName;
  }

  private void changeServerTeam(ScoreboardEntity entity, String newTeam) {
    if (entity.serverTeam != null) {
      final List<ScoreboardEntity> oldTeam = entitiesByTeam.get(entity.serverTeam);
      if (oldTeam != null) {
        oldTeam.remove(entity);
      }
    }
    entity.serverTeam = newTeam;
    if (newTeam != null) {
      entitiesByTeam.computeIfAbsent(newTeam, $ -> new ArrayList<>()).add(entity);
    }
  }

  public List<String> removeTeam(String teamName) {
    final List<ScoreboardEntity> entities = entitiesByTeam.remove(teamName);
    if (entities == null) {
      return List.of();
    }
    final List<String> result = new ArrayList<>();
    for (ScoreboardEntity entity : entities) {
      scoreboardEntities.remove(entity.uuid);
      if (entity.isMannequin()) {
        result.add(entity.mannequinName);
      }
    }
    return result;
  }

  public String registerMannequin(MannequinData data, String playerName) {
    // If server set team before mannequin spawned, scoreboardEntities has it. Otherwise,
    // we still register the entity so that future team changes recognize the entity as a mannequin
    final ScoreboardEntity entity = scoreboardEntities.computeIfAbsent(data.uuid(), ScoreboardEntity::new);
    entity.mannequinName = playerName;
    return entity.serverTeam;
  }

  public void unregisterMannequin(MannequinData data) {
    final ScoreboardEntity entity = scoreboardEntities.get(data.uuid());
    if (entity != null && entity.serverTeam == null) {
      // Remove lingering mannequin entry with no server team that was waiting
      // for the mannequin to despawn
      scoreboardEntities.remove(data.uuid());
    }
  }

  public void setFakeTeam(UserConnection connection, String mannequinName) {
    // Mannequin was removed from server team, apply our fake team again
    final PacketWrapper setTeam = PacketWrapper.create(ClientboundPackets1_21_6.SET_PLAYER_TEAM, connection);
    setTeam.write(Types.STRING, mannequinName);
    setTeam.write(Types.BYTE, (byte) 3); // Add entities
    setTeam.write(Types.STRING_ARRAY, new String[]{mannequinName});
    setTeam.send(Protocol1_21_9To1_21_7.class);
  }

  private static class ScoreboardEntity {
    private final UUID uuid;
    private String serverTeam;
    // Set to VB's mannequin team name (= mannequin profile name) if entity is a known mannequin, null otherwise
    private String mannequinName;

    private ScoreboardEntity(UUID uuid) {
      this.uuid = uuid;
    }

    private boolean isMannequin() {
      return mannequinName != null;
    }
  }
}
