package dev.smp.heartrender;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HeartrenderCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX = HeartrenderPlugin.PREFIX;
    private final HeartrenderPlugin plugin;

    public HeartrenderCommand(HeartrenderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);

            case "give" -> {
                if (!sender.hasPermission("heartrender.admin")) {
                    return noPerm(sender);
                }
                Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1])
                        : (sender instanceof Player p ? p : null);
                if (target == null) {
                    sender.sendMessage(PREFIX + "\u00a7cUsage: /" + label + " give <player>");
                    return true;
                }
                ItemStack sword = plugin.createSword();
                target.getInventory().addItem(sword).values()
                        .forEach(left -> target.getWorld().dropItemNaturally(target.getLocation(), left));
                sender.sendMessage(PREFIX + "Gave the Heartrender to \u00a7c" + target.getName() + "\u00a77.");
            }

            case "recipe" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(PREFIX + "Only players can open the recipe GUI.");
                    return true;
                }
                if (player.hasPermission("heartrender.reveal") || plugin.isRevealed(player.getUniqueId())) {
                    plugin.openRecipe(player);
                } else {
                    player.sendMessage(PREFIX + "\u00a7cYou haven't been shown the secret of the Heartrender yet.");
                }
            }

            case "reveal" -> {
                if (!sender.hasPermission("heartrender.reveal")) {
                    return noPerm(sender);
                }
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "\u00a7cUsage: /" + label + " reveal <player|all>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        plugin.reveal(p, sender.getName());
                    }
                    sender.sendMessage(PREFIX + "Revealed the recipe to everyone online.");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(PREFIX + "\u00a7cThat player isn't online.");
                    return true;
                }
                plugin.reveal(target, sender.getName());
                sender.sendMessage(PREFIX + "Revealed the recipe to \u00a7c" + target.getName() + "\u00a77.");
            }

            case "hide" -> {
                if (!sender.hasPermission("heartrender.reveal")) {
                    return noPerm(sender);
                }
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "\u00a7cUsage: /" + label + " hide <player|all>");
                    return true;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    Bukkit.getOnlinePlayers().forEach(plugin::hide);
                    sender.sendMessage(PREFIX + "Hid the recipe from everyone online.");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(PREFIX + "\u00a7cThat player isn't online.");
                    return true;
                }
                plugin.hide(target);
                sender.sendMessage(PREFIX + "Hid the recipe from \u00a7c" + target.getName() + "\u00a77.");
            }

            case "info" -> {
                if (!sender.hasPermission("heartrender.admin")) {
                    return noPerm(sender);
                }
                int max = plugin.getMaxCrafts();
                sender.sendMessage(PREFIX + "Forged so far: \u00a7c" + plugin.getCrafted()
                        + "\u00a77 / \u00a7c" + (max <= 0 ? "unlimited" : String.valueOf(max)));
            }

            case "resetcount" -> {
                if (!sender.hasPermission("heartrender.admin")) {
                    return noPerm(sender);
                }
                plugin.setCrafted(0);
                sender.sendMessage(PREFIX + "Craft counter reset. The blade can be forged again.");
            }

            case "reload" -> {
                if (!sender.hasPermission("heartrender.admin")) {
                    return noPerm(sender);
                }
                plugin.reloadConfig();
                sender.sendMessage(PREFIX + "Config reloaded.");
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private boolean noPerm(CommandSender sender) {
        sender.sendMessage(PREFIX + "\u00a7cYou don't have permission to do that.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("\u00a74\u00a7m      \u00a7r \u00a7c\u00a7lHeartrender \u00a74\u00a7m      ");
        sender.sendMessage("\u00a7c/hr recipe \u00a77- View the recipe (if revealed to you)");
        if (sender.hasPermission("heartrender.reveal")) {
            sender.sendMessage("\u00a7c/hr reveal <player|all> \u00a77- Show the recipe to someone");
            sender.sendMessage("\u00a7c/hr hide <player|all> \u00a77- Take the recipe back");
        }
        if (sender.hasPermission("heartrender.admin")) {
            sender.sendMessage("\u00a7c/hr give [player] \u00a77- Give the sword");
            sender.sendMessage("\u00a7c/hr info \u00a77- Craft counter");
            sender.sendMessage("\u00a7c/hr resetcount \u00a77- Reset craft counter");
            sender.sendMessage("\u00a7c/hr reload \u00a77- Reload config");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("help");
            out.add("recipe");
            if (sender.hasPermission("heartrender.reveal")) {
                out.add("reveal");
                out.add("hide");
            }
            if (sender.hasPermission("heartrender.admin")) {
                out.add("give");
                out.add("info");
                out.add("resetcount");
                out.add("reload");
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("give") || sub.equals("reveal") || sub.equals("hide")) {
                if (!sub.equals("give")) {
                    out.add("all");
                }
                out.addAll(plugin.onlineNames());
            }
        }
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(typed));
        return out;
    }
}
