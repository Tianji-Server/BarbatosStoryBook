package com.ghostchu.mcplugins.barbatosstorybook.database;

import cc.carm.lib.easysql.api.SQLManager;
import com.ghostchu.mcplugins.barbatosstorybook.StoryBook;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A Util to execute all SQLs.
 */
public class SimpleDatabaseHelper {
    @NotNull
    private final SQLManager manager;

    @NotNull
    private final String prefix;


    public SimpleDatabaseHelper(@NotNull SQLManager manager, @NotNull String prefix) throws Exception {
        this.manager = manager;
        this.prefix = prefix;
        checkTables();
    }


    public void checkTables() throws SQLException {
        DataTables.initializeTables(manager, prefix);
    }

    public @NotNull SQLManager getManager() {
        return manager;
    }

    public @NotNull String getPrefix() {
        return prefix;
    }

    /**
     * Returns true if the given table has the given column
     *
     * @param table  The table
     * @param column The column
     * @return True if the given table has the given column
     * @throws SQLException If the database isn't connected
     */
    public boolean hasColumn(@NotNull String table, @NotNull String column) throws SQLException {
        if (!hasTable(table)) {
            return false;
        }
        String query = "SELECT * FROM " + table + " LIMIT 1";
        boolean match = false;
        try (Connection connection = manager.getConnection(); PreparedStatement ps = connection.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                if (metaData.getColumnLabel(i).equals(column)) {
                    match = true;
                    break;
                }
            }
        } catch (SQLException e) {
            return match;
        }
        return match; // Uh, wtf.
    }


    /**
     * Returns true if the table exists
     *
     * @param table The table to check for
     * @return True if the table is found
     * @throws SQLException Throw exception when failed execute somethins on SQL
     */
    public boolean hasTable(@NotNull String table) throws SQLException {
        Connection connection = manager.getConnection();
        boolean match = false;
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "%", null)) {
            while (rs.next()) {
                if (table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
                    match = true;
                    break;
                }
            }
        } finally {
            connection.close();
        }
        return match;
    }


    public CompletableFuture<Integer> addNewStoryBook(StoryBook storyBook, String server) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("item", storyBook.getItemStack());
        return DataTables.DATA.createInsert()
                .setColumnNames("author", "item", "title", "content", "server")
                .setParams(storyBook.getAuthor().orElse("无名"),
                        configuration.saveToString(),
                        storyBook.getTitle().orElse("匿名"),
                        new Gson().toJson(storyBook.getPages()),
                        server)
                .returnGeneratedKey().executeFuture(i -> i);
    }

    public CompletableFuture<Map<Long, StoryBook>> getAllStoryBook() {
        return DataTables.DATA.createQuery()
                .build().executeFuture(query -> {
                    Map<Long, StoryBook> books = new LinkedHashMap<>();
                    ResultSet set = query.getResultSet();
                    try (set; query) {
                        while (set.next()) {
                            books.put(set.getLong("id"), toStoryBook(set));
                        }
                        return books;
                    } catch (InvalidConfigurationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public CompletableFuture<Optional<Map.Entry<Long, StoryBook>>> getRandomStoryBook() {
        return DataTables.DATA.createQuery()
                .setLimit(1)
                .addCondition("1=1 order by rand()")
                .build().executeFuture(query -> {
                    ResultSet set = query.getResultSet();
                    if (set.next()) {
                        try (set; query) {
                            return Optional.of(new AbstractMap.SimpleEntry<>(set.getLong("id"), toStoryBook(set)));
                        } catch (InvalidConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return Optional.empty();
                    }
                });
    }

    public CompletableFuture<Optional<StoryBook>> getStoryBook(long id) {
        return DataTables.DATA.createQuery().addCondition("id", id)
                .build().executeFuture(query -> {
                    ResultSet set = query.getResultSet();
                    if (set.next()) {
                        try (set; query) {
                            return Optional.of(toStoryBook(set));
                        } catch (InvalidConfigurationException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        return Optional.empty();
                    }
                });
    }

    public StoryBook toStoryBook(ResultSet set) throws SQLException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(set.getString("item"));
        ItemStack stack = configuration.getItemStack("item");
        List<String> pages = new Gson().fromJson(set.getString("content"), new TypeToken<>() {

        });
        return new StoryBook(set.getString("author"), set.getString("title"), pages, stack, set.getTimestamp("time").getTime());
    }
}
