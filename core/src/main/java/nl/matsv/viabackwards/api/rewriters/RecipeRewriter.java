package nl.matsv.viabackwards.api.rewriters;

import us.myles.ViaVersion.api.PacketWrapper;

public abstract class RecipeRewriter {

    protected final ItemRewriterBase rewriter;

    protected RecipeRewriter(final ItemRewriterBase rewriter) {
        this.rewriter = rewriter;
    }

    public abstract void handle(PacketWrapper wrapper, String type) throws Exception;
}
