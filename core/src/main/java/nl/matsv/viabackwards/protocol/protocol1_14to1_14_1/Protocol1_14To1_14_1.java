package nl.matsv.viabackwards.protocol.protocol1_14to1_14_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.packets.EntityPackets;
import nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.storage.EntityTracker;
import us.myles.ViaVersion.api.data.UserConnection;

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
