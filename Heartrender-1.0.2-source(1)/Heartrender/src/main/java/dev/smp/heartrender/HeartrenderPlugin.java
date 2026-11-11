package dev.smp.heartrender;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class HeartrenderPlugin extends JavaPlugin implements Listener {

    public static final String PREFIX = "\u00a74[\u00a7cHeartrender\u00a74] \u00a77";
    private static final String GUI_TITLE = "\u00a74\u00a7lHeartrender \u00a78- \u00a77Recipe";

    /*
     * RECIPE (3x3)
     *   Netherite Ingot | Echo Shard   | Netherite Ingot
     *   Diamond Block   | Nether Star  | Diamond Block
     *   (empty)         | Netherite Sword | (empty)
     */
    private static final Material[] GRID = {
            Material.NETHERITE_INGOT, Material.ECHO_SHARD, Material.NETHERITE_INGOT,
            Material.DIAMOND_BLOCK, Material.NETHER_STAR, Material.DIAMOND_BLOCK,
            null, Material.NETHERITE_SWORD, null
    };

    private NamespacedKey swordKey;
    private NamespacedKey recipeKey;

    private File dataFile;
    private YamlConfiguration data;

    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> noFallUntil = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();

        swordKey = new NamespacedKey(this, "heartrender_sword");
        recipeKey = new NamespacedKey(this, "heartrender_recipe");

        registerRecipe();
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand cmd = getCommand("heartrender");
        if (cmd != null) {
            HeartrenderCommand handler = new HeartrenderCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        } else {
            getLogger().severe("Command 'heartrender' missing from plugin.yml - commands will not work!");
        }
        getLogger().info("Heartrender enabled on " + Bukkit.getVersion()
                + " (Java " + System.getProperty("java.version") + ").");
    }

    @Override
    public void onDisable() {
        Bukkit.removeRecipe(recipeKey);
    }

    // ------------------------------------------------------------------
    // Data (craft counter + who has been revealed the recipe)
    // ------------------------------------------------------------------

    private void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void saveData() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }

    public int getCrafted() {
        return data.getInt("crafted", 0);
    }

    public void setCrafted(int amount) {
        data.set("crafted", amount);
        saveData();
    }

    public int getMaxCrafts() {
        return getConfig().getInt("max-crafts", 1);
    }

    public boolean isRevealed(UUID uuid) {
        return data.getStringList("revealed").contains(uuid.toString());
    }

    // ------------------------------------------------------------------
    // The sword
    // ------------------------------------------------------------------

    public ItemStack createSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta == null) {
            return sword;
        }

        meta.setDisplayName("\u00a74\u00a7l\u2726 \u00a7c\u00a7lHeartrender \u00a74\u00a7l\u2726");

        List<String> lore = new ArrayList<>();
        lore.add("\u00a74\u00a7l\u2726 MYTHIC \u2726");
        lore.add("\u00a77Forged from a thousand stolen hearts.");
        lore.add("");
        lore.add("\u00a7c\u25b8 Heart Siphon \u00a77(Passive)");
        lore.add("\u00a78  Chance on hit to drain your foe's life,");
        lore.add("\u00a78  dealing bonus damage and healing you.");
        lore.add("\u00a7c\u25b8 Blood Rush \u00a77(Right-Click)");
        lore.add("\u00a78  Surge forward in a crimson dash.");
        lore.add("");
        lore.add("\u00a74\u00a7oOnly one blade can drink this deep.");
        meta.setLore(lore);

        meta.addEnchant(Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
        meta.addEnchant(Enchantment.LOOTING, 4, true);
        meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
        meta.addEnchant(Enchantment.UNBREAKING, 5, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);

        meta.getPersistentDataContainer().set(swordKey, PersistentDataType.BYTE, (byte) 1);
        sword.setItemMeta(meta);
        return sword;
    }

    public boolean isHeartrender(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_SWORD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(swordKey, PersistentDataType.BYTE);
    }

    // ------------------------------------------------------------------
    // Recipe + reveal system
    // ------------------------------------------------------------------

    private void registerRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, createSword());
        recipe.shape("NEN", "DSD", " W ");
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('D', Material.DIAMOND_BLOCK);
        recipe.setIngredient('S', Material.NETHER_STAR);
        recipe.setIngredient('W', Material.NETHERITE_SWORD);
        Bukkit.removeRecipe(recipeKey);
        boolean registered = Bukkit.addRecipe(recipe);
        getLogger().info("Recipe registered: " + registered);
    }

    public void openRecipe(Player player) {
        player.openInventory(new RecipeGui(GUI_TITLE, GRID, createSword()).getInventory());
    }

    /** Reveals the recipe to a player: unlocks it in the recipe book and opens the GUI. */
    public void reveal(Player target, String revealerName) {
        List<String> list = data.getStringList("revealed");
        if (!list.contains(target.getUniqueId().toString())) {
            list.add(target.getUniqueId().toString());
            data.set("revealed", list);
            saveData();
        }
        target.discoverRecipe(recipeKey);
        target.playSound(target.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.6f);
        target.sendMessage(PREFIX + "\u00a7c" + revealerName + "\u00a77 has revealed the secret recipe of the \u00a7cHeartrender\u00a77 to you!");
        openRecipe(target);
    }

    public void hide(Player target) {
        List<String> list = data.getStringList("revealed");
        list.remove(target.getUniqueId().toString());
        data.set("revealed", list);
        saveData();
        target.undiscoverRecipe(recipeKey);
        target.sendMessage(PREFIX + "The secret of the Heartrender has been taken from you.");
    }

    /** Returns null if the player may craft, otherwise the reason they may not. */
    private String craftDenyReason(HumanEntity human) {
        if (!human.hasPermission("heartrender.craft")) {
            return "You are not allowed to craft this item.";
        }
        boolean needsReveal = getConfig().getBoolean("require-reveal-to-craft", false);
        if (needsReveal && !human.hasPermission("heartrender.admin") && !isRevealed(human.getUniqueId())) {
            return "You haven't been shown the secret of this blade.";
        }
        int max = getMaxCrafts();
        if (max > 0 && getCrafted() >= max) {
            return "The Heartrender has already been forged.";
        }
        return null;
    }

    private boolean isOurRecipe(Recipe recipe) {
        return recipe instanceof Keyed keyed && keyed.getKey().equals(recipeKey);
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!isOurRecipe(event.getRecipe()) || event.getViewers().isEmpty()) {
            return;
        }
        HumanEntity human = event.getViewers().get(0);
        String reason = craftDenyReason(human);
        if (reason != null) {
            event.getInventory().setResult(null);
            human.sendMessage(PREFIX + "\u00a7c" + reason);
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (!isOurRecipe(event.getRecipe())) {
            return;
        }
        HumanEntity human = event.getWhoClicked();
        String reason = craftDenyReason(human);
        if (reason != null) {
            event.setCancelled(true);
            human.sendMessage(PREFIX + "\u00a7c" + reason);
            return;
        }
        // Prevent shift-click multi-crafting so the counter can't be bypassed.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            human.sendMessage(PREFIX + "\u00a7cClick the result normally to forge this blade.");
            return;
        }

        setCrafted(getCrafted() + 1);

        if (human instanceof Player player) {
            player.getWorld().strikeLightningEffect(player.getLocation());
            if (getConfig().getBoolean("announce-craft", true)) {
                Bukkit.broadcastMessage(PREFIX + "\u00a7c" + player.getName()
                        + "\u00a77 has forged the mythic blade \u00a74\u00a7lHeartrender\u00a77!");
            }
        }
    }

    // ------------------------------------------------------------------
    // Abilities
    // ------------------------------------------------------------------

    /** Heart Siphon: passive lifesteal chance on hit. */
    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (!isHeartrender(player.getInventory().getItemInMainHand())) {
            return;
        }
        double chance = getConfig().getDouble("heart-siphon-chance", 0.15);
        if (ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }

        event.setDamage(event.getDamage() + getConfig().getDouble("heart-siphon-bonus-damage", 6.0));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

        // Lifesteal: heal the wielder (never above their max health, so it works with
        // lifesteal heart changes too).
        double heal = getConfig().getDouble("heart-siphon-heal", 4.0);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth != null ? maxHealth.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + heal));

        victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 6, 0.4, 0.3, 0.4, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_HEARTBEAT, 1f, 1.2f);
    }

    /** Blood Rush: right-click to dash forward. */
    @EventHandler
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isHeartrender(event.getItem())) {
            return;
        }

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        long cooldownMs = getConfig().getLong("dash-cooldown-seconds", 15) * 1000L;
        long readyAt = dashCooldowns.getOrDefault(player.getUniqueId(), 0L);

        if (now < readyAt) {
            long secondsLeft = (readyAt - now + 999) / 1000;
            player.sendMessage(PREFIX + "\u00a7cBlood Rush is recharging (" + secondsLeft + "s).");
            return;
        }
        dashCooldowns.put(player.getUniqueId(), now + cooldownMs);

        Vector direction = player.getLocation().getDirection().normalize();
        direction.multiply(2.2);
        direction.setY(direction.getY() * 0.5 + 0.25);
        player.setVelocity(direction);
        player.setFallDistance(0f);
        noFallUntil.put(player.getUniqueId(), now + 4000L);

        player.getWorld().spawnParticle(Particle.CRIMSON_SPORE, player.getLocation().add(0, 1, 0), 80, 0.5, 0.9, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 15, 0.4, 0.6, 0.4, 0.1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
    }

    /** Cancel fall damage shortly after a dash. */
    @EventHandler
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL || !(event.getEntity() instanceof Player player)) {
            return;
        }
        Long until = noFallUntil.get(player.getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------------
    // Recipe GUI protection
    // ------------------------------------------------------------------

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RecipeGui) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onGuiDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof RecipeGui) {
            event.setCancelled(true);
        }
    }

    public Set<String> onlineNames() {
        Set<String> names = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            names.add(p.getName());
        }
        return names;
    }

    public FileConfiguration cfg() {
        return getConfig();
    }
}
