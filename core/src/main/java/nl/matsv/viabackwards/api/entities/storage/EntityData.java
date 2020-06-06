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

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;

public class EntityData {
    private final int id;
    private final int replacementId;
    private String mobName;
    private MetaCreator defaultMeta;

    public EntityData(int id, int replacementId) {
        this.id = id;
        this.replacementId = replacementId;
    }

    public EntityData jsonName(String name) {
        this.mobName = ChatRewriter.legacyTextToJson(name);
        return this;
    }

    public EntityData mobName(String name) {
        this.mobName = name;
        return this;
    }

    public EntityData spawnMetadata(MetaCreator handler) {
        this.defaultMeta = handler;
        return this;
    }

    public boolean hasBaseMeta() {
        return this.defaultMeta != null;
    }

    public int getId() {
        return id;
    }

    @Nullable
    public String getMobName() {
        return mobName;
    }

    public int getReplacementId() {
        return replacementId;
    }

    @Nullable
    public MetaCreator getDefaultMeta() {
        return defaultMeta;
    }

    public boolean isObject() {
        return false;
    }

    public int getObjectData() {
        return -1;
    }

    @Override
    public String toString() {
        return "EntityData{" +
                "id=" + id +
                ", mobName='" + mobName + '\'' +
                ", replacementId=" + replacementId +
                ", defaultMeta=" + defaultMeta +
                '}';
    }

    @FunctionalInterface
    public interface MetaCreator {

        void createMeta(MetaStorage storage);
    }
}
