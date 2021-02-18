package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;
import us.myles.ViaVersion.api.type.Type;

public class CommandRewriter1_13_1 extends CommandRewriter {

    public CommandRewriter1_13_1(Protocol protocol) {
        super(protocol);

        this.parserHandlers.put("minecraft:dimension", wrapper -> {
            wrapper.write(Type.VAR_INT, 0); // Single word
        });
    }

    @Override
    @Nullable
    protected String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:column_pos")) {
            return "minecraft:vec2";
        } else if (argumentType.equals("minecraft:dimension")) {
            return "brigadier:string";
        }
        return super.handleArgumentType(argumentType);
    }

}
