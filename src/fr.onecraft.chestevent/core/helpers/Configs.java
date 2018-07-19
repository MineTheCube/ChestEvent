package fr.onecraft.chestevent.core.helpers;

import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Configs {
    public static Configuration get(JavaPlugin plugin, String folder, String filename) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder, filename + ".yml");
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return configuration;
        } catch (InvalidConfigurationException | IOException e) {
            return null;
        }
    }

    public static Configuration get(JavaPlugin plugin, String folder, int filename) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            File file = new File(plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder, String.valueOf(filename) + ".yml");
            FileInputStream fileinputstream = new FileInputStream(file);
            configuration.load(new InputStreamReader(fileinputstream, Charset.forName("UTF-8")));
            return configuration;
        } catch (InvalidConfigurationException | IOException e) {
            return null;
        }
    }

    public static boolean save(JavaPlugin plugin, Configuration config, String folder, String configName) {
        try {
            YamlConfiguration configuration = (YamlConfiguration) config;
            configuration.save(new File(plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder, configName + ".yml"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean save(JavaPlugin plugin, Configuration config, String folder, int configName) {
        try {
            YamlConfiguration configuration = (YamlConfiguration) config;
            configuration.save(new File(plugin.getDataFolder() + (folder.isEmpty() ? "" : "/") + folder, configName + ".yml"));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}