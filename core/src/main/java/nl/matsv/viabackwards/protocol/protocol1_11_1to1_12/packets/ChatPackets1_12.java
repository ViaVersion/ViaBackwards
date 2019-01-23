/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.AdvancementTranslations;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonParser;

import java.util.Map;

public class ChatPackets1_12 extends Rewriter<Protocol1_11_1To1_12> {
    @Override
    protected void registerPackets(Protocol1_11_1To1_12 protocol) {
        // Chat Message (ClientBound)
        protocol.registerOutgoing(State.PLAY, 0x0F, 0x0F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Json Data
                map(Type.BYTE); // 1 - Position

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        JsonParser parser = new JsonParser();
                        try {
                            JsonObject object = parser.parse(wrapper.get(Type.STRING, 0)).getAsJsonObject();

                            // Skip if the root doesn't contain translate
                            if (object.has("translate"))
                                handleTranslations(object);

                            wrapper.set(Type.STRING, 0, object.toString());
                        } catch (Exception e) {
                            // Only print if ViaVer debug is enabled
                            if (Via.getManager().isDebug()) {
                                ViaBackwards.getPlatform().getLogger().severe("Failed to handle translations");
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });

    }

    // TODO improve this, not copying will cause ConcurrentModificationException
    public void handleTranslations(JsonObject object) {
        JsonObject copiedObj = copy(object);

        if (object.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : copiedObj.entrySet()) {
                // Get the text that doesn't exist for 1.11 <
                if (entry.getKey().equalsIgnoreCase("translate") && AdvancementTranslations.has(entry.getValue().getAsString())) {
                    String trans = entry.getValue().getAsString();
                    object.remove("translate");
                    object.addProperty("translate", AdvancementTranslations.get(trans));
                }
                // Handle arrays
                if (entry.getValue().isJsonArray())
                    for (JsonElement element : object.get(entry.getKey()).getAsJsonArray())
                        if (element.isJsonObject())
                            handleTranslations(element.getAsJsonObject());

                // Handle objects
                if (entry.getValue().isJsonObject())
                    handleTranslations(object.get(entry.getKey()).getAsJsonObject());
            }
        }
    }

    public JsonObject copy(JsonObject object) {
        JsonObject result = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonObject())
                result.add(entry.getKey(), copy(entry.getValue().getAsJsonObject()));
            else
                result.add(entry.getKey(), entry.getValue());
        }
        return result;
    }

    @Override
    protected void registerRewrites() {

    }

}
