package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class WorldPackets1_13_1 {

    public static void register(Protocol protocol) {
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION, Protocol1_13To1_13_1::getNewBlockStateId, Protocol1_13To1_13_1::getNewBlockId);

        // Chunk
        protocol.registerOutgoing(State.PLAY, 0x22, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.passthrough(new Chunk1_13Type(clientWorld));

                        for (ChunkSection section : chunk.getSections()) {
                            if (section != null) {
                                for (int i = 0; i < section.getPaletteSize(); i++) {
                                    section.setPaletteEntry(i, Protocol1_13To1_13_1.getNewBlockStateId(section.getPaletteEntry(i)));
                                }
                            }
                        }
                    }
                });
            }
        });

        // Block Action
        blockRewriter.registerBlockAction(0x0A, 0x0A);

        // Block Change
        blockRewriter.registerBlockChange(0xB, 0xB);

        // Multi Block Change
        blockRewriter.registerMultiBlockChange(0xF, 0xF);

        // Effect packet
        blockRewriter.registerEffect(0x23, 0x23, 1010, 2001, InventoryPackets1_13_1::getOldItemId);

        // Spawn particle
        blockRewriter.registerSpawnParticle(Type.FLOAT, 0x24, 0x24, 3, 20, 27, InventoryPackets1_13_1::toClient, Type.FLAT_ITEM);
    }
}
