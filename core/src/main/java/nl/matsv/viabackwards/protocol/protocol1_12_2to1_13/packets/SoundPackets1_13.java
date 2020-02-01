package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.NamedSoundMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets1_13 extends Rewriter<Protocol1_12_2To1_13> {
    private static final String[] SOUND_SOURCES = {"master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"};

    public SoundPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {

        // Named Sound Event
        protocol.out(State.PLAY, 0x1A, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        String newSound = wrapper.get(Type.STRING, 0);
                        String oldSound = NamedSoundMapping.getOldId(newSound);
                        if (oldSound != null) {
                            wrapper.set(Type.STRING, 0, oldSound);
                        } else if (!Via.getConfig().isSuppressConversionWarnings()) {
                            ViaBackwards.getPlatform().getLogger().warning("Unknown named sound in 1.13->1.12 protocol: " + newSound);
                        }
                    }
                });
            }
        });

        // Stop Sound
        protocol.out(State.PLAY, 0x4C, 0x18, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "MC|StopSound");
                        byte flags = wrapper.read(Type.BYTE);
                        String source;
                        if ((flags & 0x01) != 0) {
                            source = SOUND_SOURCES[wrapper.read(Type.VAR_INT)];
                        } else {
                            source = "";
                        }
                        String sound = (flags & 0x02) != 0 ? wrapper.read(Type.STRING) : "";

                        wrapper.write(Type.STRING, source);
                        wrapper.write(Type.STRING, sound);
                    }
                });
            }
        });

        // Sound Effect
        protocol.out(State.PLAY, 0x4D, 0x49, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int newSound = wrapper.get(Type.VAR_INT, 0);
                        int oldSound = BackwardsMappings.soundMappings.getNewId(newSound);
                        if (oldSound == -1) {
                            wrapper.cancel();
                        } else {
                            wrapper.set(Type.VAR_INT, 0, oldSound);
                        }
                    }
                });
            }
        });
    }
}
