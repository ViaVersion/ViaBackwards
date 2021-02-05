package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;
import us.myles.ViaVersion.api.type.Type;

public class CommandRewriter1_16_2 extends CommandRewriter {

    public CommandRewriter1_16_2(Protocol protocol) {
        super(protocol);

        this.parserHandlers.put("minecraft:angle", wrapper -> {
            wrapper.write(Type.VAR_INT, 0); // Single word
        });
    }

    @Override
    @Nullable
    protected String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:angle")) {
            return "brigadier:string";
        }
        return super.handleArgumentType(argumentType);
    }

}
