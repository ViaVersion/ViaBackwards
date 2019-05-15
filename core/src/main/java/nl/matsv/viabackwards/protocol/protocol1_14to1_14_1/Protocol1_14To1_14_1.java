package nl.matsv.viabackwards.protocol.protocol1_14to1_14_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_14_1to1_14.packets.EntityPackets;
import us.myles.ViaVersion.protocols.protocol1_14_1to1_14.storage.EntityTracker;

/**
 * Created by Marco Neuhaus on 15.05.2019 for the Project ViaBackwardsFoorcee.
 */
public class Protocol1_14To1_14_1 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
        EntityPackets.register(this);
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.put(new EntityTracker(userConnection));
    }
}
