package nl.matsv.viabackwards.protocol.protocol1_12to1_12_1;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

@Getter
@Setter
@ToString
public class KeepAliveTracker extends StoredObject {
    private long keepAlive;

    public KeepAliveTracker(UserConnection user) {
        super(user);
    }
}
