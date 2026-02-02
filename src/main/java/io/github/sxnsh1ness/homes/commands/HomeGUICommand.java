package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.gui.HomeGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeGUICommand implements CommandExecutor {

    private final HomeGUI homeGUI;

    public HomeGUICommand(HomeGUI homeGUI) {
        this.homeGUI = homeGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!", NamedTextColor.RED));
            return true;
        }

        homeGUI.openGUI(player, 0);
        return true;
    }
}
