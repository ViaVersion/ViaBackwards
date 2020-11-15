package nl.matsv.viabackwards.listener;

import nl.matsv.viabackwards.BukkitPlugin;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import us.myles.ViaVersion.bukkit.listeners.ViaBukkitListener;

public class FireDamageListener extends ViaBukkitListener {

    public FireDamageListener(BukkitPlugin plugin) {
        super(plugin, Protocol1_11_1To1_12.class);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFireDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.FIRE
                && cause != EntityDamageEvent.DamageCause.FIRE_TICK
                && cause != EntityDamageEvent.DamageCause.LAVA
                && cause != EntityDamageEvent.DamageCause.DROWNING) {
            return;
        }

        Player player = (Player) event.getEntity();
        if (isOnPipe(player)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1, 1);
        }
    }
}
