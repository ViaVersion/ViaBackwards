package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

import java.util.function.Function;

public class SoundRewriter {

    private final BackwardsProtocol protocol;
    // Can't hold the mappings instance here since it's loaded later
    private final IdRewriteFunction idRewriter;
    private final Function<String, String> stringIdRewriter;

    public SoundRewriter(BackwardsProtocol protocol, IdRewriteFunction idRewriter, Function<String, String> stringIdRewriter) {
        this.protocol = protocol;
        this.idRewriter = idRewriter;
        this.stringIdRewriter = stringIdRewriter;
    }

    public SoundRewriter(BackwardsProtocol protocol, IdRewriteFunction idRewriter) {
        this(protocol, idRewriter, null);
    }

    // The same for entity sound effect
    public void registerSound(int oldId, int newId) {
        protocol.registerOutgoing(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound Id
                handler(wrapper -> {
                    int soundId = wrapper.get(Type.VAR_INT, 0);
                    int mappedId = idRewriter.rewrite(soundId);
                    if (mappedId != -1 && soundId != mappedId) {
                        wrapper.set(Type.VAR_INT, 0, mappedId);
                    }
                });
            }
        });
    }

    public void registerNamedSound(int oldId, int newId) {
        protocol.registerOutgoing(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound identifier
                handler(wrapper -> {
                    String soundId = wrapper.get(Type.STRING, 0);
                    String mappedId = stringIdRewriter.apply(soundId);
                    if (mappedId == null) return;
                    if (!mappedId.isEmpty()) {
                        wrapper.set(Type.STRING, 0, mappedId);
                    } else {
                        wrapper.cancel();
                    }
                });
            }
        });
    }
}
