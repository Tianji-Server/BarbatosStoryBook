package com.ghostchu.mcplugins.barbatosstorybook;

import cc.carm.lib.easysql.hikari.HikariConfig;
import cc.carm.lib.easysql.hikari.HikariDataSource;
import cc.carm.lib.easysql.manager.SQLManagerImpl;
import com.ghostchu.mcplugins.barbatosstorybook.database.HikariUtil;
import com.ghostchu.mcplugins.barbatosstorybook.database.SimpleDatabaseHelper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public final class BarbatosStoryBook extends JavaPlugin implements Listener {
    private static final Random RANDOM = new Random();
    private final NamespacedKey KEY = new NamespacedKey(this, "storychest");
    private SQLManagerImpl sqlManager;
    private SimpleDatabaseHelper databaseHelper;
    private String serverName;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        this.serverName = getConfig().getString("server");
        if (!setupDatabase()) {
            Bukkit.getPluginManager().disablePlugin(this);
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public SQLManagerImpl getSqlManager() {
        return sqlManager;
    }

    public SimpleDatabaseHelper getDatabaseHelper() {
        return databaseHelper;
    }

    private boolean setupDatabase() {

        try {
            ConfigurationSection dbCfg = getConfig().getConfigurationSection("database");
            HikariConfig config = HikariUtil.createHikariConfig(dbCfg.getConfigurationSection("properties"));
            String dbPrefix = dbCfg.getString("prefix");
            if (dbPrefix == null || "none".equals(dbPrefix)) {
                dbPrefix = "";
            }
            String user = dbCfg.getString("user");
            String pass = dbCfg.getString("password");
            String host = dbCfg.getString("host");
            String port = dbCfg.getString("port");
            String database = dbCfg.getString("database");
            boolean useSSL = dbCfg.getBoolean("usessl");
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
            config.setUsername(user);
            config.setPassword(pass);
            this.sqlManager = new SQLManagerImpl(new HikariDataSource(config), "BarbatosStoryBook-SQLManager");
            //this.sqlManager.setDebugMode(Util.isDevMode());
            // Make the database up to date
            this.databaseHelper = new SimpleDatabaseHelper(this.sqlManager, dbPrefix);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onInventoryClosed(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Container c)) {
            return;
        }
        Boolean b = c.getPersistentDataContainer().get(KEY, PersistentDataType.BOOLEAN);
        if (b != null && b) {
            handleStoryBook(event.getInventory(), event.getPlayer());
        }
    }

    private void handleStoryBook(Inventory inventory, HumanEntity player) {
        if (!(player instanceof Player)) {
            return;
        }
        Map<ItemStack, StoryBook> map = new LinkedHashMap<>();
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack == null) continue;
            if (stack.getType() != Material.WRITTEN_BOOK) continue;
            if (!(stack.getItemMeta() instanceof BookMeta bookMeta)) continue;
            String author = bookMeta.getAuthor();
            String title = bookMeta.getTitle();
            StoryBook storyBook = new StoryBook(author, title, bookMeta.getPages(), stack.clone(), System.currentTimeMillis());
            map.put(stack, storyBook);
        }
        if (map.isEmpty()) return;
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("submitted")));
        BukkitScheduler sc = Bukkit.getScheduler();
        sc.runTaskAsynchronously(this, () -> {
            boolean anyFailed = false;
            for (Map.Entry<ItemStack, StoryBook> book : map.entrySet()) {
                StoryBook storyBook = book.getValue();
                try {
                    if (getConfig().getBoolean("also-write-to-file")) {
                        storyBook.writeTo(getDataFolder());
                    }
                    int changed = databaseHelper.addNewStoryBook(storyBook, serverName).join();
                    if (changed == 0) {
                        getLogger().log(Level.WARNING, "处理故事书时出错: 0 行受到影响");
                        anyFailed = true;
                        continue;
                    }
                    Bukkit.getScheduler().runTask(this, () -> inventory.removeItem(book.getKey()));
                } catch (Throwable th) {
                    getLogger().log(Level.WARNING, "处理故事书时出错", th);
                    anyFailed = true;
                }
            }
            if (anyFailed) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("failure")));
            } else {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("success-write-into-database")));
            }
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("storybook")) {
            if (sender instanceof Player p) {
                if (!p.hasPermission("barabtosstorybook.admin")) {
                    return false;
                }
                Block block = p.getTargetBlockExact(10);
                if (block == null) {
                    p.sendMessage("你必须看向一个有效容器来执行此命令");
                    return true;
                }
                BlockState state = block.getState();
                if (state instanceof Container c) {
                    BlockBreakEvent event = new BlockBreakEvent(c.getBlock(), p);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        p.sendMessage("此容器不能被设置为故事箱：权限检查失败");
                        return true;
                    }
                    c.getPersistentDataContainer().set(KEY, PersistentDataType.BOOLEAN, true);
                    c.update(true);
                    p.sendMessage("容器设置成功");
                } else {
                    p.sendMessage("你所看的方块非有效容器");
                }
                return true;
            } else {
                return false;
            }
        }
        if (command.getName().equals("randomstorybook")) {
            if (sender instanceof Player p) {
                if (!p.hasPermission("barabtosstorybook.open")) {
                    return false;
                }
                databaseHelper.getRandomStoryBook().thenAccept(optional -> {
                    if (optional.isEmpty()) {
                        p.sendMessage("现在好像还没有人写下自己的故事……？");
                        return;
                    }
                    Map.Entry<Long, StoryBook> book = optional.get();
                    openBookForPlayer(p, book.getValue(), book.getKey());
                });
                return true;
            } else {
                return false;
            }
        }
        if (command.getName().equals("openspecificstorybook")) {
            if (sender instanceof Player p) {
                if (!p.hasPermission("barabtosstorybook.open")) {
                    return false;
                }
                long id = Long.parseLong(args[0]);
                databaseHelper.getStoryBook(id).thenAccept(optional -> {
                    if (optional.isEmpty()) {
                        p.sendMessage("现在好像还没有人写下自己的故事……？");
                        return;
                    }
                    StoryBook book = optional.get();
                    openBookForPlayer(p, book, id);
                });
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void openBookForPlayer(Player p, StoryBook book, long id) {
        p.openBook(book.getItemStack());
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        p.sendMessage(ChatColor.translateAlternateColorCodes('&', String.format(getConfig().getString("openbook"),
                book.getAuthor().orElse("匿名"),
                book.getTitle().orElse("无名"),
                format.format(new Date(book.getTime()))
        )));
        TextComponent changeOne = new TextComponent("[再看一本]");
        changeOne.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        changeOne.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("点击换一本继续看")));
        changeOne.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/randomstorybook"));
        TextComponent reopen = new TextComponent("[重新打开]");
        reopen.setColor(net.md_5.bungee.api.ChatColor.GRAY);
        reopen.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("点击重新打开这本书")));
        reopen.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/openspecificstorybook " + id));
        TextComponent finalComponent = new TextComponent(reopen, new TextComponent(" "), changeOne);
        p.spigot().sendMessage(finalComponent);
    }

}
