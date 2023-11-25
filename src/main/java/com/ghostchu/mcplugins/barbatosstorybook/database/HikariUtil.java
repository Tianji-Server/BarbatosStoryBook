package com.ghostchu.mcplugins.barbatosstorybook.database;

import org.bukkit.configuration.ConfigurationSection;

public class HikariUtil {
    private HikariUtil() {
    }

    public static cc.carm.lib.easysql.hikari.HikariConfig createHikariConfig(ConfigurationSection properties) {
        cc.carm.lib.easysql.hikari.HikariConfig config = new cc.carm.lib.easysql.hikari.HikariConfig();
        if (properties == null) {
            throw new IllegalArgumentException("properties section in configuration not found");
        }
        for (String key : properties.getKeys(false)) {
            config.addDataSourceProperty(key, properties.getString(key));
        }
        return config;
    }
}
