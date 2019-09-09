/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards;

import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.fabric.util.LoggerWrapper;
import org.apache.logging.log4j.LogManager;

import java.util.logging.Logger;

public class ViaFabricAddon implements ViaBackwardsPlatform, Runnable {
    @Getter
    private final Logger logger = new LoggerWrapper(LogManager.getLogger("ViaBackwards"));

    @Override
    public void run() {
        this.init(FabricLoader.getInstance().getConfigDirectory().toPath().resolve("ViaBackwards").resolve("config.yml").toFile());
    }

    @Override
    public void disable() {
        // Not possible
    }
}
