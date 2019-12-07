package nl.matsv.viabackwards.api.rewriters;

import us.myles.ViaVersion.api.PacketWrapper;

public abstract class RecipeRewriter {

    protected final BlockItemRewriter rewriter;

    protected RecipeRewriter(final BlockItemRewriter rewriter) {
        this.rewriter = rewriter;
    }

    public abstract void handle(PacketWrapper wrapper, String type) throws Exception;
}
