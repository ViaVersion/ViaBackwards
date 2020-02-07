package nl.matsv.viabackwards.api.rewriters;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public abstract class RecipeRewriter {

    protected final ItemRewriterBase rewriter;

    protected RecipeRewriter(final ItemRewriterBase rewriter) {
        this.rewriter = rewriter;
    }

    public abstract void handle(PacketWrapper wrapper, String type) throws Exception;

    public void registerDefaultHandler(int oldId, int newId) {
        rewriter.getProtocol().registerOutgoing(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < size; i++) {
                        String type = wrapper.passthrough(Type.STRING).replace("minecraft:", "");
                        String id = wrapper.passthrough(Type.STRING); // Recipe Identifier
                        handle(wrapper, type);
                    }
                });
            }
        });
    }
}
