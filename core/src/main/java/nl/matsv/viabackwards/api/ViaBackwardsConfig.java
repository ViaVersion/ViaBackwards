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
}
