package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;

public class SoundRewriter extends us.myles.ViaVersion.api.rewriters.SoundRewriter {

    private final BackwardsProtocol protocol;

    public SoundRewriter(BackwardsProtocol protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    public void registerNamedSound(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound identifier
                handler(getNamedSoundHandler());
            }
        });
    }

    public void registerStopSound(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(getStopSoundHandler());
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            String soundId = wrapper.get(Type.STRING, 0);
            if (soundId.startsWith("minecraft:")) {
                soundId = soundId.substring(10);
            }

            String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId == null) return;
            if (!mappedId.isEmpty()) {
                wrapper.set(Type.STRING, 0, mappedId);
            } else {
                wrapper.cancel();
            }
        };
    }

    public PacketHandler getStopSoundHandler() {
        return wrapper -> {
            byte flags = wrapper.passthrough(Type.BYTE);
            if ((flags & 0x02) == 0) return; // No sound specified

            if ((flags & 0x01) != 0) {
                wrapper.passthrough(Type.VAR_INT); // Source
            }

            String soundId = wrapper.read(Type.STRING);
            if (soundId.startsWith("minecraft:")) {
                soundId = soundId.substring(10);
            }

            String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId == null) {
                // No mapping found
                wrapper.write(Type.STRING, soundId);
                return;
            }

            if (!mappedId.isEmpty()) {
                wrapper.write(Type.STRING, mappedId);
            } else {
                // Cancel if set to empty
                wrapper.cancel();
            }
        };
    }
}
