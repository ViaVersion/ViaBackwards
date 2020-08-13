package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ImmediateRespawn;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets.BlockItemPackets1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets.EntityPackets1_15;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.StatisticsRewriter;
import us.myles.ViaVersion.api.rewriters.TagRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.Protocol1_15To1_14_4;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData;

public class Protocol1_14_4To1_15 extends BackwardsProtocol<ClientboundPackets1_15, ClientboundPackets1_14, ServerboundPackets1_14, ServerboundPackets1_14> {

    private BlockItemPackets1_15 blockItemPackets;

    public Protocol1_14_4To1_15() {
        super(ClientboundPackets1_15.class, ClientboundPackets1_14.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_15To1_14_4.class, BackwardsMappings::init);

        TranslatableRewriter translatableRewriter = new TranslatableRewriter(this);
        translatableRewriter.registerBossBar(ClientboundPackets1_15.BOSSBAR);
        translatableRewriter.registerChatMessage(ClientboundPackets1_15.CHAT_MESSAGE);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_15.COMBAT_EVENT);
        translatableRewriter.registerDisconnect(ClientboundPackets1_15.DISCONNECT);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_15.OPEN_WINDOW);
        translatableRewriter.registerTabList(ClientboundPackets1_15.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_15.TITLE);
        translatableRewriter.registerPing();

        (blockItemPackets = new BlockItemPackets1_15(this, translatableRewriter)).register();
        new EntityPackets1_15(this).register();

        SoundRewriter soundRewriter = new SoundRewriter(this,
                id -> BackwardsMappings.soundMappings.getNewId(id), stringId -> BackwardsMappings.soundMappings.getNewId(stringId));
        soundRewriter.registerSound(ClientboundPackets1_15.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_15.ENTITY_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_15.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_15.STOP_SOUND);

        // Explosion - manually send an explosion sound
        registerOutgoing(ClientboundPackets1_15.EXPLOSION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLOAT); // x
                map(Type.FLOAT); // y
                map(Type.FLOAT); // z
                handler(wrapper -> {
                    PacketWrapper soundPacket = wrapper.create(0x51);
                    soundPacket.write(Type.VAR_INT, 243); // entity.generic.explode
                    soundPacket.write(Type.VAR_INT, 4); // blocks category
                    soundPacket.write(Type.INT, toEffectCoordinate(wrapper.get(Type.FLOAT, 0))); // x
                    soundPacket.write(Type.INT, toEffectCoordinate(wrapper.get(Type.FLOAT, 1))); // y
                    soundPacket.write(Type.INT, toEffectCoordinate(wrapper.get(Type.FLOAT, 2))); // z
                    soundPacket.write(Type.FLOAT, 4F); // volume
                    soundPacket.write(Type.FLOAT, 1F); // pitch - usually semi randomized by the server, but we don't really have to care about that
                    soundPacket.send(Protocol1_14_4To1_15.class);
                });
            }

            private int toEffectCoordinate(float coordinate) {
                return (int) (coordinate * 8);
            }
        });

        new TagRewriter(this, id -> BackwardsMappings.blockMappings.getNewId(id),
                id -> MappingData.oldToNewItems.inverse().get(id), EntityTypeMapping::getOldEntityId).register(ClientboundPackets1_15.TAGS);

        new StatisticsRewriter(this, id -> BackwardsMappings.blockMappings.getNewId(id), id -> MappingData.oldToNewItems.inverse().get(id),
                EntityTypeMapping::getOldEntityId, id -> BackwardsMappings.statisticsMappings.getNewId(id)).register(ClientboundPackets1_15.STATISTICS);
    }

    public static int getNewBlockStateId(int id) {
        int newId = BackwardsMappings.blockStateMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 blockstate id for 1.14.4 block " + id);
            return 0;
        }
        return newId;
    }

    public static int getNewBlockId(int id) {
        int newId = BackwardsMappings.blockMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 block id for 1.14.4 block " + id);
            return id;
        }
        return newId;
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ImmediateRespawn.class)) {
            user.put(new ImmediateRespawn(user));
        }
        if (!user.has(EntityTracker.class)) {
            user.put(new EntityTracker(user));
        }
        user.get(EntityTracker.class).initProtocol(this);
    }

    public BlockItemPackets1_15 getBlockItemPackets() {
        return blockItemPackets;
    }
}
