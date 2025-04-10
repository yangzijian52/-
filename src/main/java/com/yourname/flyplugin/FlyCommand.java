package com.yourname.flyplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public class FlyCommand implements CommandExecutor {
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Set<UUID> pendingResets = new HashSet<>(); // 需要重置状态的玩家
    private final Economy econ = Main.getEconomy();
    private final Main plugin;

    public FlyCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只有玩家可以使用此命令！");
            return true;
        }

        UUID uuid = player.getUniqueId();
        String command = cmd.getName().toLowerCase();

        switch (command) {
            case "flystart" -> handleFlyStart(player, uuid);
            case "flystop" -> handleFlyStop(player, uuid);
        }
        return true;
    }

    // 开启飞行
    private void handleFlyStart(Player player, UUID uuid) {
        if (activeTasks.containsKey(uuid)) {
            player.sendMessage("§e你已经开启飞行了！");
            return;
        }

        double cost = plugin.getCostPerMinute();

        // 立即检查并扣除首次费用
        if (!econ.has(player, cost)) {
            player.sendMessage("§c需要至少 " + cost + " 金币才能开启飞行");
            return;
        }
        econ.withdrawPlayer(player, cost); // 立即扣除

        // 启用飞行
        player.setAllowFlight(true);
        player.setFlying(true);
        player.sendMessage("§a飞行已开启，已扣除 " + cost + " 金币，后续每分钟将再扣除");

        // 记录日志
        if (plugin.isLoggingEnabled()) {
            plugin.getLogger().info(player.getName() + " 开启飞行并扣费 " + cost);
        }

        // 定时任务（延迟60秒后执行，周期60秒）
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                stopFlight(uuid, true);
                return;
            }

            if (econ.has(player, cost)) {
                econ.withdrawPlayer(player, cost);
                if (plugin.isLoggingEnabled()) {
                    plugin.getLogger().info(player.getName() + " 定时扣费 " + cost);
                }
            } else {
                player.sendMessage("§c余额不足，飞行已关闭！");
                stopFlight(uuid, false);
            }
        }, 1200L, 1200L); // 60秒 = 1200 ticks (20 ticks/秒 * 60)

        activeTasks.put(uuid, task);
    }

    // 关闭飞行
    private void handleFlyStop(Player player, UUID uuid) {
        if (stopFlight(uuid, false)) {
            player.sendMessage("§a飞行已关闭");
        } else {
            player.sendMessage("§c你还没有开启飞行！");
        }
    }

    // 停止飞行（isAuto：是否自动触发）
    public boolean stopFlight(UUID uuid, boolean isAuto) {
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
            // 自动触发时不立即关闭飞行（如玩家离线）
            if (!isAuto) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            } else {
                pendingResets.add(uuid); // 标记需要重置
            }
            return true;
        }
        return false;
    }

    // 状态检查
    public boolean needsReset(UUID uuid) {
        return pendingResets.contains(uuid);
    }

    public void clearReset(UUID uuid) {
        pendingResets.remove(uuid);
    }
}