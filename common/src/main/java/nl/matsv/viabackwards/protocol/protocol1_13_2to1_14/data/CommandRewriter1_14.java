package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;
import us.myles.ViaVersion.api.type.Type;

public class CommandRewriter1_14 extends CommandRewriter {

    public CommandRewriter1_14(Protocol protocol) {
        super(protocol);

        this.parserHandlers.put("minecraft:nbt_tag", wrapper -> {
            wrapper.write(Type.VAR_INT, 2); // Greedy phrase
        });
        this.parserHandlers.put("minecraft:time", wrapper -> {
            wrapper.write(Type.BYTE, (byte) (0x01)); // Flags
            wrapper.write(Type.INT, 0); // Min value
        });
    }

    @Override
    @Nullable
    protected String handleArgumentType(String argumentType) {
        switch (argumentType) {
            case "minecraft:nbt_compound_tag":
                return "minecraft:nbt";
            case "minecraft:nbt_tag":
                return "brigadier:string";
            case "minecraft:time":
                return "brigadier:integer";
        }
        return super.handleArgumentType(argumentType);
    }

}
