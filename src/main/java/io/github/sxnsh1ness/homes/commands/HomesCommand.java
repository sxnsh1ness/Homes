package io.github.sxnsh1ness.homes.commands;

import io.github.sxnsh1ness.homes.gui.HomeGUI;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class HomesCommand implements CommandExecutor {

    private final HomeGUI homeGUI;

    public HomesCommand(HomeGUI homeGUI) {
        this.homeGUI = homeGUI;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Эта команда доступна только игрокам!"));
            return true;
        }

        homeGUI.openGUI(player, 0);
        return true;
    }
}
