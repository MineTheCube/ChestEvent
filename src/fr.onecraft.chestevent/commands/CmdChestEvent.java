package fr.onecraft.chestevent.commands;

import fr.onecraft.chestevent.ChestEvent;
import fr.onecraft.chestevent.core.objects.Chest;
import fr.onecraft.chestevent.core.objects.Model;
import fr.onecraft.chestevent.core.objects.Pager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CmdChestEvent implements CommandExecutor {
    private ChestEvent plugin;

    public CmdChestEvent(ChestEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();
        if (!sender.hasPermission("chestevent." + action)) {
            sender.sendMessage(ChestEvent.ERROR + "Tu n'as pas la permission.");
        } else if (action.startsWith(":") && (sender instanceof Player)) {
            showPage(sender, action);
        } else if (action.equalsIgnoreCase("list")) {
            showEventList(sender);
        } else if (action.equalsIgnoreCase("reload")) {
            reloadCache(sender);
        } else if (args.length < 2) {
            showHelp(sender);
        } else {
            String event = args[1];
            if (!Model.eventExists(event, plugin)) {
                sender.sendMessage(ChestEvent.ERROR + "Cet événement n'existe pas.");
            } else if (action.equalsIgnoreCase("viewcontent")) {
                viewContent(sender, event);
            } else if (action.equalsIgnoreCase("info")) {
                info(sender, event);
            } else if (action.equalsIgnoreCase("give")) {
                give(sender, event, args);
            } else {
                showHelp(sender);
            }
        }
        return true;
    }

    /*
     * METHODS
     */

    private void reloadCache(CommandSender sender) {
        Model.loadEventList(plugin);
        sender.sendMessage(ChestEvent.PREFIX + "Les modèles ont été chargés.");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChestEvent.PREFIX + "Gère les récompenses d'événements"
                + (sender.hasPermission("chestevent.info") ? "\n§b/chestevent info <event>§7 informations sur un événement" : "")
                + (sender.hasPermission("chestevent.viewcontent") ? "\n§b/chestevent viewcontent <event>§7 contenu d'un événement" : "")
                + (sender.hasPermission("chestevent.list") ? "\n§b/chestevent list §7 liste des événements" : "")
                + (sender.hasPermission("chestevent.give") ? "\n§b/chestevent give <event> [pseudo]§7 give un coffre d'événement" : ""));
    }

    private void showEventList(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Model> models = Model.getEventList();
            if (models.isEmpty()) {
                sender.sendMessage(ChestEvent.PREFIX + "Il n'y a aucun événement.");
                return;
            }

            sender.sendMessage(ChestEvent.PREFIX + "Liste des événements");
            models.stream().map(model -> " §7– §b" + model.getName()).forEach(sender::sendMessage);
        });
    }

    private void info(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Model model = Model.fromName(plugin, event);
            if (model == null) {
                sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
                return;
            }

            sender.sendMessage(ChestEvent.PREFIX + "Informations sur l'événement §a" + event + "\n"
                    + " §8- §7Description: §b" + model.getDescription() + "\n"
                    + " §8- §7Permission: §b" + model.getPermission() + "\n"
            );
        });
    }

    private void give(CommandSender sender, String event, String[] args) {
        if (!Model.isValidName(event)) {
            sender.sendMessage(ChestEvent.PREFIX + "Le nom du modèle n'est pas valide, il ne peut comporter que des lettres, chiffres et tirets.");
            return;
        }

        Model model = Model.fromName(plugin, event);
        if (model == null) {
            sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
            return;
        }

        Chest chest = model.createChest();

        if (chest == null) {
            sender.sendMessage(ChestEvent.PREFIX + "Erreur lors de la création du coffre.");
            return;
        }

        if (args.length > 2) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target != null) {
                if (target.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChestEvent.ERROR + "Impossible de donner le coffre, l'inventaire de §a" + target.getName() + " §7est plein.");
                    return;
                }

                sender.sendMessage(ChestEvent.PREFIX + "§a" + sender.getName() + " §7a reçu le coffre de l'événement §a" + event + "§7.");
                target.getInventory().addItem(chest.getChestItem());
            } else
                sender.sendMessage(ChestEvent.ERROR + "§a" + args[2] + " §7est introuvable.");
        } else {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (player.getInventory().firstEmpty() == -1) {
                    sender.sendMessage(ChestEvent.ERROR + "Impossible de vous donner le coffre, votre inventaire est plein.");
                    return;
                }

                sender.sendMessage(ChestEvent.PREFIX + "Vous avez reçu le coffre de l'événement §a" + event + "§7.");
                player.getInventory().addItem(chest.getChestItem());
            } else {
                sender.sendMessage(ChestEvent.PREFIX + "Vous devez etre un joueur pour effectuer cette action.");
            }
        }
    }

    private void showPage(CommandSender sender, String arg) {
        String number = arg.substring(1);
        try {
            int page = Integer.parseInt(number);
            Pager pager = plugin.getPagers().get(sender);
            if (pager.getPages() < page) {
                sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
                return;
            }

            pager.setCurrentPage(page);
            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + pager.getEvent());
            message.setColor(ChatColor.GRAY);
            message.addExtra(": ");
            TextComponent previewPage = new TextComponent("[<--]");
            previewPage.setColor(pager.getCurrentPage() >= 2 ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
            previewPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    "§7Aller à la page précédente"
            ).create()));
            if (pager.getCurrentPage() >= 2) {
                previewPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() - 1)));
            }
            message.addExtra(previewPage);

            TextComponent pages = new TextComponent(" " + pager.getCurrentPage());
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);
            pages = new TextComponent("/");
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);
            pages = new TextComponent(pager.getPages() + " ");
            pages.setColor(ChatColor.GRAY);
            message.addExtra(pages);

            TextComponent nextPage = new TextComponent("[-->]");
            nextPage.setColor(pager.getCurrentPage() < pager.getPages() ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
            nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                    "§7Aller à la page suivante"
            ).create()));
            if (pager.getCurrentPage() < pager.getPages()) {
                nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() + 1)));
            }
            message.addExtra(nextPage);

            sender.sendMessage("\n");
            ((Player) sender).spigot().sendMessage(message);

            pager.getPage(pager.getCurrentPage()).forEach(msg -> ((Player) sender).spigot().sendMessage(msg));
        } catch (NumberFormatException e) {
            sender.sendMessage(ChestEvent.ERROR + "Cette page n'existe pas.");
        }
    }

    private void viewContent(CommandSender sender, String event) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Model model = Model.fromName(plugin, event);
            if (model == null) {
                sender.sendMessage(ChestEvent.PREFIX + "La configuration du modèle n'est pas valide.");
                return;
            }

            List<ItemStack> items = model.getContent();
            List<TextComponent> messages = new ArrayList<>();
            int num = 1;
            for (ItemStack itemStack : items) {
                ItemMeta meta = itemStack.getItemMeta();
                int count = 0;
                TextComponent component = new TextComponent("§7" + num + " §8- §b" + itemStack.getType() + " x" + itemStack.getAmount() + "§8, §7" + itemStack.getItemMeta().getDisplayName());
                String lore = "";
                if (itemStack.getItemMeta().getLore() != null)
                    lore = itemStack.getItemMeta().getLore().stream().map(desc -> "\n" + desc).collect(Collectors.joining());
                StringBuilder enchants = new StringBuilder();
                if (!itemStack.getItemMeta().getEnchants().isEmpty()) {
                    for (Enchantment enchantment : meta.getEnchants().keySet()) {
                        enchants.append("\n").append(" §8– §b").append(enchantment.getName()).append(" niv.").append(meta.getEnchants().values().toArray()[count]);
                        count++;
                    }
                }

                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        (!lore.isEmpty() ? "§7§lDescription" : "")
                                + lore
                                + ((!lore.isEmpty() && !meta.getEnchants().isEmpty()) ? "\n\n" : "")
                                + (!meta.getEnchants().isEmpty() ? "§7§lEnchantements" : "")
                                + enchants.toString()
                ).create()));
                messages.add(component);
                num++;
            }

            Pager pager = new Pager(event, messages);
            if (sender instanceof Player)
                plugin.getPagers().put(((Player) sender).getUniqueId(), pager);

            TextComponent message = new TextComponent(ChestEvent.PREFIX + "Contenu de l'événement §a" + event);
            message.setColor(ChatColor.GRAY);
            message.addExtra("§7: ");

            if (items.size() > 15 && sender instanceof Player) {
                TextComponent previewPage = new TextComponent("[<--]");
                previewPage.setColor(pager.getCurrentPage() > 1 ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
                previewPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        "§7Aller à la page précédente"
                ).create()));
                if (pager.getCurrentPage() > 1)
                    previewPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() - 1)));
                message.addExtra(previewPage);

                TextComponent pages = new TextComponent(" " + pager.getCurrentPage());
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);
                pages = new TextComponent("/");
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);
                pages = new TextComponent(pager.getPages() + " ");
                pages.setColor(ChatColor.GRAY);
                message.addExtra(pages);

                TextComponent nextPage = new TextComponent("[-->]");
                nextPage.setColor(pager.getCurrentPage() < pager.getPages() ? ChatColor.YELLOW : ChatColor.DARK_GRAY);
                nextPage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(
                        "§7Aller à la page suivante"
                ).create()));
                if (pager.getCurrentPage() < pager.getPages())
                    nextPage.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/chestevent :" + (pager.getCurrentPage() + 1)));
                message.addExtra(nextPage);
            }

            sender.sendMessage("\n");

            if (sender instanceof Player) {
                Player player = (Player) sender;
                player.spigot().sendMessage(message);
                pager.getPage(1).forEach(msg -> player.spigot().sendMessage(msg));
            } else {
                sender.sendMessage(BaseComponent.toLegacyText(message));
                messages.stream().map(b -> b.toLegacyText()).forEach(sender::sendMessage);
            }
        });
    }
}