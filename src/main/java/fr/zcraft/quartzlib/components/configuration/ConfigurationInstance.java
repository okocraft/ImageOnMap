/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fr.zcraft.quartzlib.components.configuration;

import fr.zcraft.quartzlib.core.QuartzComponent;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.Callback;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;


public class ConfigurationInstance extends QuartzComponent {
    private final String fileName;
    private final File file;
    private ConfigurationItem[] items;
    private Callback<ConfigurationItem<?>> updateCallback;
    private FileConfiguration bukkitConfiguration;

    /**
     * Creates a configuration instance from a file.
     *
     * @param file The file containing the YAML configuration.
     */
    public ConfigurationInstance(File file) {
        this.file = file;
        this.fileName = null;
        this.bukkitConfiguration = new YamlConfiguration();
    }

    /**
     * Creates a configuration instance from a file.
     *
     * @param fileName The name, relative to the plugin's data folder, of the
     *                 file containing the YAML configuration.
     */
    public ConfigurationInstance(String fileName) {
        this.file = null;
        this.fileName = fileName;
        this.bukkitConfiguration = new YamlConfiguration();
    }

    /**
     * Creates a configuration instance from a {@link FileConfiguration} directly.
     *
     * @param config The configuration.
     */
    ConfigurationInstance(FileConfiguration config) {
        this.bukkitConfiguration = config;
        this.file = null;
        this.fileName = null;
    }

    /**
     * Initializes and loads the configuration.
     *
     * @throws IOException                   If the configuration is loaded from a file and this file
     *                                       cannot be opened for some reason.
     * @throws InvalidConfigurationException If the configuration is invalid.
     */
    public void load() throws IOException, InvalidConfigurationException {
        init(this.getClass());

        final File underlyingFile = getUnderlyingFile();

        if (underlyingFile != null) {
            bukkitConfiguration.load(underlyingFile);
        }
        // Else the configuration is already “loaded” as the configuration instance was
        // directly constructed with a FileConfiguration instance.

        validateVerbally();
    }

    /**
     * Reloads the configuration from its file.
     *
     * @throws IllegalStateException If the configuration was directly constructed from
     *                               a {@link FileConfiguration}, as there is nothing to reload the configuration from.
     */
    public void reload() throws IllegalStateException {
        reload(false);
    }

    /**
     * Reloads the configuration from its file.
     *
     * @param assumeConfigYml If {@code true}, and the configuration was loaded from a
     *                        {@link FileConfiguration} directly, we assume that we have
     *                        to reload the main plugin configuration from config.yml,
     *                        as this configuration is loaded directly. This option is
     *                        used internally in {@link Configuration} for the reload method.
     * @throws IllegalStateException If the configuration was directly constructed from
     *                               a {@link FileConfiguration}, and {@code assumeConfigYml} is false,
     *                               as there is nothing to reload the configuration from.
     */
    void reload(boolean assumeConfigYml) throws IllegalStateException {
        try {
            final File underlyingFile = getUnderlyingFile();

            if (underlyingFile != null) {
                this.bukkitConfiguration.load(underlyingFile);
            } else if (assumeConfigYml) {
                // We assume that it's config.yml
                QuartzLib.getPlugin().reloadConfig();

                // The instance change when the reloadConfig method is called.
                this.bukkitConfiguration = QuartzLib.getPlugin().getConfig();
            } else {
                throw new IllegalStateException(
                        "Cannot reload configuration if constructed directly from a FileConfiguration instance.");
            }

            validateVerbally();
        } catch (Exception ex) {
            PluginLogger.error("Couldn't read configuration file {0}: {1}", ex, getFilePath(), ex.getMessage());
        }
    }

    /**
     * Returns the config's file path.
     * @return The file path, or {@code null} if no file name is set. This may return a
     *     relative path to the plugin's data folder.
     * @see #getUnderlyingFile() Method to get a {@code File} object directly, without
     *     path problems.
     */
    public String getFilePath() {
        if (file != null) {
            return file.getAbsolutePath();
        }
        return fileName;
    }

    /**
     * Returns the underlying file.
     * @return A {@link File} object pointing to the underlying file, or {@code null} if no
     *     file name is set. This assumes that the file name is relative to the plugin's data folder.
     */
    private File getUnderlyingFile() {
        if (file == null && fileName == null) {
            return null;
        }
        return file != null ? file : new File(QuartzLib.getPlugin().getDataFolder().getAbsolutePath() + "/" + fileName);
    }

    /**
     * Validates then prints error messages if it does not actually validates.
     */
    private void validateVerbally() {
        if (!validate()) {
            // The file path is null when the instance was constructed from a FileConfiguration object directly.
            final String filePath = getFilePath();

            if (filePath != null) {
                PluginLogger.warning("Some configuration values are invalid. Please check the ''{0}'' file.", filePath);
            } else {
                PluginLogger.warning("Some configuration values are invalid. Please check the configuration file.");
            }
        }
    }

    @Override
    protected void onEnable() {
        try {
            load();
        } catch (Exception ex) {
            PluginLogger.error("Couldn't read configuration file {0}: {1}", ex, getFilePath(), ex.getMessage());
        }
    }

    final void init(Class<?> configurationClass) {
        if (items != null) {
            return;
        }

        ArrayList<ConfigurationItem<?>> itemsList = new ArrayList<>();

        for (Field field : configurationClass.getFields()) {
            if (ConfigurationItem.class.isAssignableFrom(field.getType())) {
                try {
                    ConfigurationItem<?> item = (ConfigurationItem<?>) field.get(this);
                    item.setInstance(this);
                    itemsList.add(item);
                } catch (Exception ignored) {
                }
            }
        }

        items = itemsList.toArray(new ConfigurationItem[itemsList.size()]);
        initFields();
    }

    /**
     * Saves the configuration to the disk.
     *
     * @throws IllegalStateException If the configuration was directly constructed from
     *                               a {@link FileConfiguration}, as there is nothing to save the configuration to.
     */
    public void save() throws IllegalStateException {
        save(false);
    }

    /**
     * Saves the configuration to the disk.
     *
     * @param assumeConfigYml If {@code true}, and the configuration was loaded from a
     *                        {@link FileConfiguration} directly, we assume that we have
     *                        to save the main plugin configuration to config.yml,
     *                        as this configuration is loaded directly. This option is
     *                        used internally in {@link Configuration} for the save method.
     * @throws IllegalStateException If the configuration was directly constructed from
     *                               a {@link FileConfiguration}, and {@code assumeConfigYml} is false,
     *                               as there is nothing to save the configuration to.
     */
    void save(boolean assumeConfigYml) throws IllegalStateException {
        final File saveTo = getUnderlyingFile();

        if (saveTo != null) {
            try {
                this.getConfig().save(saveTo);
            } catch (final IOException e) {
                PluginLogger.error("Could not save config to {0}", e, saveTo);
            }
        } else if (assumeConfigYml) {
            QuartzLib.getPlugin().saveConfig();
        } else {
            throw new IllegalStateException(
                    "Cannot save configuration if constructed directly from a FileConfiguration instance.");
        }
    }

    /**
     * Gets the underlying configuration object.
     * @return The underlying configuration object.
     */
    public FileConfiguration getConfig() {
        return bukkitConfiguration;
    }

    /**
     * Registers a callback called when the configuration is updated someway.
     *
     * <p>Callbacks are not called when the configuration is reloaded fully.</p>
     *
     * @param callback The callback.
     */
    public void registerConfigurationUpdateCallback(Callback<ConfigurationItem<?>> callback) {
        updateCallback = callback;
    }

    private void initFields() {
        for (ConfigurationItem<?> configField : items) {
            configField.init();
        }
    }

    /**
     * Checks if the configuration is valid.
     *
     * @return validity.
     */
    private boolean validate() {
        boolean isValid = true;

        for (ConfigurationItem<?> configField : items) {
            if (!configField.validate()) {
                isValid = false;
            }
        }

        return isValid;
    }

    /**
     * Calls an update callback for the given configuration item.
     *
     * @param configurationItem The updated configuration item.
     */
    void triggerCallback(ConfigurationItem<?> configurationItem) {
        if (updateCallback != null) {
            updateCallback.call(configurationItem);
        }
    }
}
