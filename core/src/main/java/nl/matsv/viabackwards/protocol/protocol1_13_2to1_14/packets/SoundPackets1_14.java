package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.EntityPositionStorage;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.SoundMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

import java.util.Optional;

public class SoundPackets1_14 extends Rewriter<Protocol1_13_2To1_14> {
    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
        // Sound Effect
        protocol.registerOutgoing(State.PLAY, 0x51, 0x4D, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound Id
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(Type.VAR_INT, 0, SoundMapping.getOldSound(wrapper.get(Type.VAR_INT, 0)));
                    }
                });
            }
        });

        // Entity Sound Effect
        protocol.registerOutgoing(State.PLAY, 0x50, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();

                        int soundId = wrapper.read(Type.VAR_INT);
                        int category = wrapper.read(Type.VAR_INT);
                        int entityId = wrapper.read(Type.VAR_INT);

                        Optional<EntityTracker.StoredEntity> optEntity = wrapper.user().get(EntityTracker.class).get(protocol).getEntity(entityId);
                        EntityPositionStorage storedEntity;
                        if (!optEntity.isPresent() || (storedEntity = optEntity.get().get(EntityPositionStorage.class)) == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Untracked entity with id " + entityId);
                            return;
                        }

                        float volume = wrapper.read(Type.FLOAT);
                        float pitch = wrapper.read(Type.FLOAT);
                        int x = (int) (storedEntity.getX() * 8D);
                        int y = (int) (storedEntity.getY() * 8D);
                        int z = (int) (storedEntity.getZ() * 8D);

                        PacketWrapper soundPacket = wrapper.create(0x4D);
                        soundPacket.write(Type.VAR_INT, SoundMapping.getOldSound(soundId));
                        soundPacket.write(Type.VAR_INT, category);
                        soundPacket.write(Type.INT, x);
                        soundPacket.write(Type.INT, y);
                        soundPacket.write(Type.INT, z);
                        soundPacket.write(Type.FLOAT, volume);
                        soundPacket.write(Type.FLOAT, pitch);
                        soundPacket.send(Protocol1_13_2To1_14.class);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
    }
}
