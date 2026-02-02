package io.github.sxnsh1ness.homes;

import io.github.sxnsh1ness.homes.commands.*;
import io.github.sxnsh1ness.homes.config.ConfigManager;
import io.github.sxnsh1ness.homes.config.PluginMessages;
import io.github.sxnsh1ness.homes.database.DatabaseManager;
import io.github.sxnsh1ness.homes.gui.HomeGUI;
import io.github.sxnsh1ness.homes.gui.InviteGUI;
import io.github.sxnsh1ness.homes.listeners.ChatListener;
import io.github.sxnsh1ness.homes.utils.CooldownManager;
import io.github.sxnsh1ness.homes.utils.LuckPermsHelper;
import io.github.sxnsh1ness.homes.utils.TeleportManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class HomesPlugin extends JavaPlugin {

    @Getter
    private static HomesPlugin instance;
    @Getter
    private DatabaseManager databaseManager;
    @Getter
    private CooldownManager cooldownManager;
    @Getter
    private TeleportManager teleportManager;
    @Getter
    private HomeGUI homeGUI;

    @Override
    public void onEnable() {
        instance = this;
        ConfigManager.loadConfig();
        PluginMessages.loadMessages();

        if (!LuckPermsHelper.setupLuckPerms()) {
            getLogger().severe("LuckPerms не найден! Плагин будет отключен.");
            getServer().getPluginManager().disablePlugin(this);
        }

        databaseManager = new DatabaseManager();
        databaseManager.initialize();

        cooldownManager = new CooldownManager();

        teleportManager = new TeleportManager(cooldownManager);

        homeGUI = new HomeGUI(databaseManager, teleportManager, cooldownManager);
        InviteGUI inviteGUI = new InviteGUI(this, databaseManager, homeGUI);
        homeGUI.setInviteGUI(inviteGUI);
        ChatListener chatListener = new ChatListener(databaseManager, homeGUI);
        chatListener.setInviteGUI(inviteGUI);
        getServer().getPluginManager().registerEvents(chatListener, this);

        getCommand("home").setExecutor(new HomeCommand(databaseManager, teleportManager, cooldownManager));
        getCommand("sethome").setExecutor(new SetHomeCommand(databaseManager));
        getCommand("deletehome").setExecutor(new DeleteHomeCommand(databaseManager));
        getCommand("renamehome").setExecutor(new RenameHomeCommand(databaseManager));
        getCommand("homes").setExecutor(new HomesCommand(databaseManager));
        getCommand("homegui").setExecutor(new HomeGUICommand(homeGUI));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
