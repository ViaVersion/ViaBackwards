package nl.matsv.viabackwards.protocol.protocol1_14to1_14_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.packets.EntityPackets1_14_1;
import us.myles.ViaVersion.api.data.UserConnection;

public class Protocol1_14To1_14_1 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
        new EntityPackets1_14_1(this).register();
    }

    @Override
    public void init(UserConnection user) {
        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);
    }
}
