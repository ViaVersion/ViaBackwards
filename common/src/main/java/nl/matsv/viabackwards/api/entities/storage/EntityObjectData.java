package nl.matsv.viabackwards.api.entities.storage;

public class EntityObjectData extends EntityData {
    private final boolean isObject;
    private final int objectData;

    public EntityObjectData(int id, boolean isObject, int replacementId, int objectData) {
        super(id, replacementId);
        this.isObject = isObject;
        this.objectData = objectData;
    }

    @Override
    public boolean isObject() {
        return isObject;
    }

    @Override
    public int getObjectData() {
        return objectData;
    }
}
