package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;

import java.util.ArrayList;

@Getter
@Setter
public class InventoryTracker extends StoredObject {
    private int lastTransactionWindow = -1;
    private int lastShiftTransaction = -1;
    // Workaround for https://github.com/Matsv/ViaBackwards/issues/48
    // Resent when it is rejected
    private ArrayList<ClickWindow> clicks = new ArrayList<>();

    public InventoryTracker(UserConnection user) {
        super(user);
    }

    @AllArgsConstructor
    public static class ClickWindow {
        public short windowId;
        public short slot;
        public byte button;
        public short actionNumber;
        public int mode;
    }
}
