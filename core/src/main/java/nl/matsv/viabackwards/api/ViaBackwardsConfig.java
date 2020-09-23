package nl.matsv.viabackwards.api;

public interface ViaBackwardsConfig {

    /**
     * Mimics name and level of a custom enchant through the item's lore.
     *
     * @return true if enabled
     */
    boolean addCustomEnchantsToLore();

    /**
     * Writes the color of the scoreboard team after the prefix.
     *
     * @return true if enabled
     */
    boolean addTeamColorTo1_13Prefix();

    /**
     * Converts the new 1.13 face player packets to look packets.
     *
     * @return true if enabled
     */
    boolean isFix1_13FacePlayer();

    /**
     * Always shows the original mob's name instead of only when hovering over them with the cursor.
     *
     * @return true if enabled
     */
    boolean alwaysShowOriginalMobName();
}
