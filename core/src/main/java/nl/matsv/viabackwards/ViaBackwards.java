package nl.matsv.viabackwards;

import com.google.common.base.Preconditions;
import lombok.Getter;

public class ViaBackwards {
    @Getter
    private static ViaBackwardsPlatform platform;

    public static void init(ViaBackwardsPlatform platform) {
        Preconditions.checkArgument(platform != null, "ViaBackwards is already initialized");

        ViaBackwards.platform = platform;
    }
}
