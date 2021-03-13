package nl.matsv.viabackwards.api;

import nl.matsv.viabackwards.api.data.BackwardsMappings;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.protocol.ClientboundPacketType;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.protocol.ServerboundPacketType;

public abstract class BackwardsProtocol<C1 extends ClientboundPacketType, C2 extends ClientboundPacketType, S1 extends ServerboundPacketType, S2 extends ServerboundPacketType>
        extends Protocol<C1, C2, S1, S2> {

    protected BackwardsProtocol() {
    }

    protected BackwardsProtocol(@Nullable Class<C1> oldClientboundPacketEnum, @Nullable Class<C2> clientboundPacketEnum,
                                @Nullable Class<S1> oldServerboundPacketEnum, @Nullable Class<S2> serverboundPacketEnum) {
        super(oldClientboundPacketEnum, clientboundPacketEnum, oldServerboundPacketEnum, serverboundPacketEnum);
    }

    /**
     * Waits for the given protocol to be loaded to then asynchronously execute the runnable for this protocol.
     */
    protected void executeAsyncAfterLoaded(Class<? extends Protocol> protocolClass, Runnable runnable) {
        ProtocolRegistry.addMappingLoaderFuture(getClass(), protocolClass, runnable);
    }

    protected void initEntityTracker(UserConnection user) {
        EntityTracker entityTracker = user.get(EntityTracker.class);
        if (entityTracker == null) {
            user.put(entityTracker = new EntityTracker(user));
        }

        entityTracker.initProtocol(this);
    }

    @Override
    public boolean hasMappingDataToLoad() {
        // Manually load them later, since they depend on VV's mappings
        return false;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return null;
    }
}
