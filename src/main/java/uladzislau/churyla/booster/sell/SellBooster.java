package uladzislau.churyla.booster.sell;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.commands.Commandsell;
import net.ess3.api.events.UserBalanceUpdateEvent;
import net.ess3.api.events.UserBalanceUpdateEvent.Cause;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SellBooster extends JavaPlugin implements Listener {

    private final Pattern permissionPattern = Pattern.compile("^boost.money.[1-9][0-9]{0,4}$");

    private Essentials essentials;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Utils.cfg = this.getConfig();
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void handleUserUpdateBalance(UserBalanceUpdateEvent e) {
        if (e.getCause() != Cause.COMMAND_SELL) {
            return;
        }
        final var player = e.getPlayer();
        final var permissions = player.getEffectivePermissions().stream()
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permissionPattern.asMatchPredicate())
                .collect(Collectors.toSet());

        if (permissions.isEmpty()) {
            return;
        }

        final var price = e.getNewBalance().subtract(e.getOldBalance());
        if (price.equals(BigDecimal.ZERO)) {
            return;
        }

        final var cashback = permissions.stream()
                .map(perm -> perm.split("\\.")[2])
                .mapToLong(Long::parseLong)
                .mapToObj(percent -> price.multiply(BigDecimal.valueOf(percent)).divide(BigDecimal.valueOf(100L)))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_EVEN);

        e.setNewBalance(e.getNewBalance().add(cashback));
        player.sendMessage(Utils.formatMSG("cashback-success", cashback.doubleValue()));
    }

    @EventHandler
    public void onPlayerPickUpItemEventHandler(EntityPickupItemEvent e) {
        Commandsell commandsell = new Commandsell();
        final var entity = e.getEntity();
        if (!(entity instanceof Player)) return;

        var player = (Player) entity;
        try {
            commandsell.run(Bukkit.getServer(), essentials.getUser(player), "sell", new String[]{"all"});
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

}
