package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data;

import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.rewriters.CommandRewriter;

public class CommandRewriter1_16 extends CommandRewriter {

    public CommandRewriter1_16(Protocol protocol) {
        super(protocol);
    }

    @Override
    @Nullable
    protected String handleArgumentType(String argumentType) {
        if (argumentType.equals("minecraft:uuid")) {
            return "minecraft:game_profile";
        }
        return super.handleArgumentType(argumentType);
    }

}
