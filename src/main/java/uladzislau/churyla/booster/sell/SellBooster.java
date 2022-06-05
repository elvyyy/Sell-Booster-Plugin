package uladzislau.churyla.booster.sell;

import net.ess3.api.events.UserBalanceUpdateEvent;
import net.ess3.api.events.UserBalanceUpdateEvent.Cause;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SellBooster extends JavaPlugin implements Listener {

    private final Pattern permissionPattern = Pattern.compile("^boost.money.[1-9][0-9]{0,4}$");

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        Utils.cfg = this.getConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler(ignoreCancelled = true)
    public void handleUserUpdateBalance(UserBalanceUpdateEvent e) {
        if (e.getCause() != Cause.COMMAND_SELL) {
            return;
        }
        final var player = e.getPlayer();
//        final var user = luckPerms.getPlayerAdapter(Player.class).getUser(player);
//        final var permissions = user.getNodes(NodeType.PERMISSION).stream()
//                .map(PermissionNode::getPermission)
//                .filter(permissionPattern.asMatchPredicate())
//                .collect(Collectors.toSet());
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

}
