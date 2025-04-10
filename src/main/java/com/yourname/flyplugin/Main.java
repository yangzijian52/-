package com.yourname.flyplugin;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
    private static Economy econ;
    private static Main instance;
    private FlyCommand flyCommand;
    private FileConfiguration config;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();

        // 初始化经济系统
        if (!setupEconomy()) {
            getLogger().severe("Vault经济系统未连接，插件已禁用！");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        flyCommand = new FlyCommand(this);
        // 注册命令
        getCommand("flystart").setExecutor(flyCommand);
        getCommand("flystop").setExecutor(flyCommand);
        getCommand("flyreload").setExecutor((sender, cmd, label, args) -> {
            reloadConfig();
            config = getConfig();
            sender.sendMessage("§a配置重载成功！");
            return true;
        });
        // 注册事件监听
        getServer().getPluginManager().registerEvents(new PlayerListener(flyCommand), this);

        // 启动日志
        if (config.getBoolean("settings.console-logging")) {
            getLogger().info("§a飞行插件 v1.0 已加载");
        }
    }

    // 经济系统初始化
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    // 配置方法
    public double getCostPerMinute() { return config.getDouble("settings.cost-per-minute"); }
    public long getChargeInterval() { return config.getLong("settings.charge-interval") * 20L; }
    public boolean isLoggingEnabled() { return config.getBoolean("settings.console-logging"); }

    // 静态访问
    public static Economy getEconomy() { return econ; }
    public static Main getInstance() { return instance; }
}