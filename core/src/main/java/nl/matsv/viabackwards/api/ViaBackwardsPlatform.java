/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9_4To1_10;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.logging.Logger;

public interface ViaBackwardsPlatform {
    /**
     * Initialize ViaBackwards
     */
    default void init() {
        ViaBackwards.init(this);

        if (!isOutdated()) {
            ProtocolRegistry.registerProtocol(new Protocol1_9_4To1_10(), Collections.singletonList(ProtocolVersion.v1_9_3.getId()), ProtocolVersion.v1_10.getId());
            ProtocolRegistry.registerProtocol(new Protocol1_10To1_11(), Collections.singletonList(ProtocolVersion.v1_10.getId()), ProtocolVersion.v1_11.getId());
            ProtocolRegistry.registerProtocol(new Protocol1_11To1_11_1(), Collections.singletonList(ProtocolVersion.v1_11.getId()), ProtocolVersion.v1_11_1.getId());
        }
    }

    /**
     * Logger provided by the platform
     *
     * @return logger instance
     */
    Logger getLogger();

    // TODO remove or better implement on full release
    default boolean isOutdated() {
        Method m = null;
        try {
            m = ChunkSection.class.getMethod("getBlock", int.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) {
        }

        if (m == null) {
            getLogger().severe("================================");
            getLogger().severe("YOUR VIAVERSION IS OUTDATED");
            getLogger().severe("PLEASE USE THE LATEST DEVBUILD");
            getLogger().severe("LINK: https://ci.viaversion.com/job/ViaVersion-DEV/");
            getLogger().severe("VIABACKWARDS WILL NOW DISABLE");
            getLogger().severe("================================");

            disable();
            return true;
        }

        return false;
    }

    /**
     * Disable the plugin
     */
    void disable();
}
