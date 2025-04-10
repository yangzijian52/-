package com.yourname.flyplugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.UUID;

public class PlayerListener implements Listener {
    private final FlyCommand flyCommand;

    public PlayerListener(FlyCommand flyCommand) {
        this.flyCommand = flyCommand;
    }

    // 玩家退出时处理
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 如果正在飞行，标记需要重置
        if (flyCommand.stopFlight(uuid, true) && Main.getInstance().isLoggingEnabled()) {
            Main.getInstance().getLogger().info(player.getName() + " 退出时处于飞行状态");
        }
    }

    // 玩家加入时处理
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 检查是否需要重置飞行状态
        if (flyCommand.needsReset(uuid)) {
            player.setAllowFlight(false);
            player.setFlying(false);
            flyCommand.clearReset(uuid);
            if (Main.getInstance().isLoggingEnabled()) {
                Main.getInstance().getLogger().info(player.getName() + " 的飞行状态已重置");
            }
        }
    }
}