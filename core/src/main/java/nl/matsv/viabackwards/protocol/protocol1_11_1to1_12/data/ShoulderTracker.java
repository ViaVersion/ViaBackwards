/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.md_5.bungee.api.ChatColor;
import nl.matsv.viabackwards.ViaBackwards;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_12to1_11_1.Protocol1_12To1_11_1;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9TO1_8;

@Getter
@Setter
@ToString
public class ShoulderTracker extends StoredObject {
    private int entityId;
    private String leftShoulder;
    private String rightShoulder;

    public ShoulderTracker(UserConnection user) {
        super(user);
    }

    public void update() {
        PacketWrapper wrapper = new PacketWrapper(0x0F, null, getUser());

        wrapper.write(Type.STRING, Protocol1_9TO1_8.fixJson(generateString()));
        wrapper.write(Type.BYTE, (byte) 2);

        try {
            wrapper.send(Protocol1_12To1_11_1.class);
        } catch (Exception e) {
            ViaBackwards.getPlatform().getLogger().severe("Failed to send the shoulder indication");
            e.printStackTrace();
        }
    }

    // Does actionbar not support json colors? :(
    private String generateString() {
        StringBuilder builder = new StringBuilder();

        // Empty spaces because the non-json formatting is weird
        builder.append("  ");
        if (leftShoulder == null)
            builder.append(ChatColor.RED).append(ChatColor.BOLD).append("Nothing");
        else
            builder.append(ChatColor.DARK_GREEN).append(ChatColor.BOLD).append(getName(leftShoulder));

        builder.append(ChatColor.DARK_GRAY).append(ChatColor.BOLD).append(" <- ")
                .append(ChatColor.GRAY).append(ChatColor.BOLD).append("Shoulders")
                .append(ChatColor.DARK_GRAY).append(ChatColor.BOLD).append(" -> ");

        if (rightShoulder == null)
            builder.append(ChatColor.RED).append(ChatColor.BOLD).append("Nothing");
        else
            builder.append(ChatColor.DARK_GREEN).append(ChatColor.BOLD).append(getName(rightShoulder));

        return builder.toString();
    }

    private String getName(String current) {
        if (current.startsWith("minecraft:"))
            current = current.substring(10);
        String[] array = current.split("_");
        StringBuilder builder = new StringBuilder();

        for (String s : array) {
            builder.append(s.substring(0, 1).toUpperCase())
                    .append(s.substring(1))
                    .append(" ");
        }

        return builder.toString();
    }
}
