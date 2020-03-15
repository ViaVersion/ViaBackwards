package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.VBSoundMappings;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundRewriter {

    private final BackwardsProtocol protocol;
    private final VBSoundMappings soundMappings;

    public SoundRewriter(BackwardsProtocol protocol, VBSoundMappings soundMappings) {
        this.protocol = protocol;
        this.soundMappings = soundMappings;
    }

    // The same for entity sound effect
    public void registerSound(int oldId, int newId) {
        protocol.registerOutgoing(State.PLAY, oldId, newId, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound Id
                handler(wrapper -> {
                    int soundId = wrapper.get(Type.VAR_INT, 0);
                    int mappedId = soundMappings.getNewId(soundId);
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
                    String mappedId = soundMappings.getNewId(soundId);
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
