package nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.storage;

import com.google.common.base.Optional;
import lombok.Getter;
import lombok.Setter;
import us.myles.ViaVersion.api.data.ExternalJoinGameListener;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.Entity1_14Types.EntityType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Marco Neuhaus on 15.05.2019 for the Project ViaBackwardsFoorcee.
 */
public class EntityTracker  extends StoredObject implements ExternalJoinGameListener {
    private final Map<Integer, EntityType> clientEntityTypes = new ConcurrentHashMap<>();
    @Getter
    @Setter
    private int clientEntityId;

    public EntityTracker(UserConnection user) {
        super(user);
    }

    public void removeEntity(int entityId) {
        clientEntityTypes.remove(entityId);
    }

    public void addEntity(int entityId, Entity1_14Types.EntityType type) {
        clientEntityTypes.put(entityId, type);
    }

    public boolean has(int entityId) {
        return clientEntityTypes.containsKey(entityId);
    }

    public Optional<EntityType> get(int id) {
        return Optional.fromNullable(clientEntityTypes.get(id));
    }

    @Override
    public void onExternalJoinGame(int playerEntityId) {
        clientEntityId = playerEntityId;
        clientEntityTypes.put(playerEntityId, Entity1_14Types.EntityType.PLAYER);
    }
}