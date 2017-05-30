/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nl.matsv.viabackwards.api.v2.MetaStorage;

@RequiredArgsConstructor
@Getter
@ToString
public class EntityData {
    private final int id;
    private final boolean isObject;

    private final int replacementId;
    private final int objectData;
    private MetaCreator defaultMeta;

    public void spawnMetadata(MetaCreator handler) {
        this.defaultMeta = handler;
    }

    public boolean hasBaseMeta() {
        return this.defaultMeta != null;
    }

    public interface MetaCreator {
        void handle(MetaStorage storage);
    }
}
