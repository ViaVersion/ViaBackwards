/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.storage;

public class EntityData {
    private final int id;
    private final boolean isObject;
    private String mobName;

    private final int replacementId;
    private final int objectData;
    private MetaCreator defaultMeta;

    public EntityData(int id, boolean isObject, int replacementId, int objectData) {
        this.id = id;
        this.isObject = isObject;
        this.replacementId = replacementId;
        this.objectData = objectData;
    }

    public EntityData mobName(String name) {
        this.mobName = name;
        return this;
    }

    public void spawnMetadata(MetaCreator handler) {
        this.defaultMeta = handler;
    }

    public boolean hasBaseMeta() {
        return this.defaultMeta != null;
    }

    public int getId() {
        return id;
    }

    public boolean isObject() {
        return isObject;
    }

    public String getMobName() {
        return mobName;
    }

    public int getReplacementId() {
        return replacementId;
    }

    public int getObjectData() {
        return objectData;
    }

    public MetaCreator getDefaultMeta() {
        return defaultMeta;
    }

    @Override
    public String toString() {
        return "EntityData{" +
                "id=" + id +
                ", isObject=" + isObject +
                ", mobName='" + mobName + '\'' +
                ", replacementId=" + replacementId +
                ", objectData=" + objectData +
                ", defaultMeta=" + defaultMeta +
                '}';
    }

    public interface MetaCreator {

        void handle(MetaStorage storage);
    }
}
