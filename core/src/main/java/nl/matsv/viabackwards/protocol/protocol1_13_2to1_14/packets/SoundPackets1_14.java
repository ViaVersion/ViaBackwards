package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.EntityPositionStorage1_14;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;

public class SoundPackets1_14 extends Rewriter<Protocol1_13_2To1_14> {

    public SoundPackets1_14(Protocol1_13_2To1_14 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        SoundRewriter soundRewriter = new SoundRewriter(protocol,
                id -> BackwardsMappings.soundMappings.getNewId(id), stringId -> BackwardsMappings.soundMappings.getNewId(stringId));
        soundRewriter.registerSound(ClientboundPackets1_14.SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_14.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_14.STOP_SOUND);

        // Entity Sound Effect
        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_SOUND, null, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.cancel();

                    int soundId = wrapper.read(Type.VAR_INT);
                    int newId = BackwardsMappings.soundMappings.getNewId(soundId);
                    if (newId == -1) return;

                    int category = wrapper.read(Type.VAR_INT);
                    int entityId = wrapper.read(Type.VAR_INT);

                    EntityTracker.StoredEntity storedEntity = wrapper.user().get(EntityTracker.class).get(protocol).getEntity(entityId);
                    EntityPositionStorage1_14 entityStorage;
                    if (storedEntity == null || (entityStorage = storedEntity.get(EntityPositionStorage1_14.class)) == null) {
                        ViaBackwards.getPlatform().getLogger().warning("Untracked entity with id " + entityId);
                        return;
                    }

                    float volume = wrapper.read(Type.FLOAT);
                    float pitch = wrapper.read(Type.FLOAT);
                    int x = (int) (entityStorage.getX() * 8D);
                    int y = (int) (entityStorage.getY() * 8D);
                    int z = (int) (entityStorage.getZ() * 8D);

                    PacketWrapper soundPacket = wrapper.create(0x4D);
                    soundPacket.write(Type.VAR_INT, newId);
                    soundPacket.write(Type.VAR_INT, category);
                    soundPacket.write(Type.INT, x);
                    soundPacket.write(Type.INT, y);
                    soundPacket.write(Type.INT, z);
                    soundPacket.write(Type.FLOAT, volume);
                    soundPacket.write(Type.FLOAT, pitch);
                    soundPacket.send(Protocol1_13_2To1_14.class);
                });
            }
        });
    }
}
