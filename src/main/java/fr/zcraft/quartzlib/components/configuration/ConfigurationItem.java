/*
 * Copyright or © or Copr. QuartzLib contributors (2015 - 2020)
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.zcraft.quartzlib.components.configuration;

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import org.bukkit.configuration.file.FileConfiguration;


/**
 * Represents a configuration item stored in the config.yml file.
 *
 * @param <T> The type of the stored value.
 */
public class ConfigurationItem<T> {
    private final Class<T> valueType;
    String fieldName;
    T defaultValue;
    String[] deprecatedNames;
    private ConfigurationItem<?> parent;
    private ConfigurationInstance instance;


    protected ConfigurationItem(String fieldName, T defaultValue, String... deprecatedNames) {
        this(fieldName, defaultValue, Reflection.getDeclaringClass(defaultValue), deprecatedNames);
    }

    /**
     * @param fieldName       The path of the field in the {@code config.yml} file.
     * @param defaultValue    The default value if this is not defined.
     * @param valueType       The type of the value of this configurationItem, if it
     *                        can't be deduced from the defaultValue.
     * @param deprecatedNames A list of deprecated names to migrate the old
     *                        values automatically.
     */
    public ConfigurationItem(String fieldName, T defaultValue, Class<T> valueType, String... deprecatedNames) {
        this.fieldName = fieldName;
        this.defaultValue = defaultValue;
        this.deprecatedNames = deprecatedNames;
        this.valueType = valueType;
    }

    @Deprecated
    public static <T> ConfigurationItem<T> item(String fieldName) {
        return item(fieldName, null);
    }

    /**
     * Utility method to construct a configuration item.
     *
     * @param fieldName       The path of the field in the {@code config.yml} file.
     * @param defaultValue    The default value if this is not defined.
     * @param deprecatedNames A list of deprecated names to migrate the old values automatically.
     * @param <T>             The type of the stored value.
     * @return A ready-to-use configuration item.
     */
    public static <T> ConfigurationItem<T> item(String fieldName, T defaultValue, String... deprecatedNames) {
        return new ConfigurationItem<>(fieldName, defaultValue, deprecatedNames);
    }

    /**
     * Utility method to construct a configuration item.
     *
     * @param fieldName       The path of the field in the {@code config.yml} file.
     * @param type            The default value if this is not defined.
     * @param deprecatedNames A list of deprecated names to migrate the old values automatically.
     * @param <T>             The type of the stored value.
     * @return A ready-to-use configuration item.
     */
    public static <T> ConfigurationItem<T> item(String fieldName, Class<T> type, String... deprecatedNames) {
        return new ConfigurationItem<>(fieldName, null, type, deprecatedNames);
    }

    public static <T> ConfigurationList<T> list(String fieldName, Class<T> type, String... deprecatedNames) {
        return new ConfigurationList<>(fieldName, new ArrayList<T>(), type, deprecatedNames);
    }

    public static <K, V> ConfigurationMap<K, V> map(String field, Class<K> keyType, Class<V> valueType,
                                                    String... deprecatedNames) {
        return new ConfigurationMap<>(field, new HashMap<K, V>(), keyType, valueType, deprecatedNames);
    }

    /**
     * Gets the defined value for this configuration item.
     * @return the defined value for this configuration item, or the default value if missing.
     */
    public T get() {
        try {
            T value = getValue(getRawValue());
            if (value == null) {
                return defaultValue;
            }
            return value;
        } catch (ConfigurationParseException ex) {
            return defaultValue;
        }
    }

    /**
     * Gets the field name.
     * @return the path in the configuration file.
     */
    public String getFieldName() {
        if (parent != null) {
            return parent.getFieldName() + '.' + fieldName;
        }
        return fieldName;
    }

    /**
     * Returns a list of deprecated field names.
     */
    public String[] getDeprecatedFieldNames() {
        ArrayList<String> allNames = new ArrayList<>();

        allNames.add(getFieldName());

        if (parent == null) {
            allNames.addAll(Arrays.asList(deprecatedNames));
        } else {
            for (String parentName : parent.getDeprecatedFieldNames()) {
                allNames.add(parentName + "." + fieldName);
                for (String deprecatedName : deprecatedNames) {
                    allNames.add(parentName + "." + deprecatedName);
                }
            }
        }

        return allNames.toArray(new String[0]);
    }

    /**
     * Returns {@code true} if a value is explicitly set in the configuration file.
     */
    public boolean isDefined() {
        return getConfig().contains(getFieldName());
    }

    /**
     * Updates the value of this configuration item. Saves the change in the configuration file.
     * <p> If you don't want to save the update, use {@link #set(Object, boolean) set(value, false)}.</p>
     *
     * @param value the new value.
     * @return The previously stored value.
     * @see #set(Object, boolean)
     */
    public T set(T value) {
        return set(value, true);
    }

    /**
     * Updates the value of this configuration item.
     *
     * @param value the new value.
     * @param save  {@code true} to save this change in the config file. Be aware that it will save all unsaved changes,
     *              including previous values changed with this argument set to {@code false}.
     * @return The previously stored value.
     */
    public T set(T value, boolean save) {
        T oldValue = get();
        getConfig().set(getFieldName(), value);

        if (getInstance() != null) {
            instance.triggerCallback(this);
            if (save) {
                instance.save();
            }
        }

        return oldValue;
    }

    @Override
    public String toString() {
        return get().toString();
    }

    boolean validate() {
        try {
            getValue(getRawValue());
            return true;
        } catch (ConfigurationParseException ex) {
            PluginLogger
                    .warning("Invalid value for configuration field ''{0}'' : ''{1}''.", getFieldName(), ex.getValue());
            PluginLogger.warning("\tReason : {0}", ex.getMessage());
            return false;
        } catch (Exception ex) {
            PluginLogger.error("Exception caught while validating the configuration field ''{0}''", ex, getFieldName());
            return false;
        }
    }

    protected T getValue(Object object) throws ConfigurationParseException {
        return ConfigurationValueHandlers.handleValue(getRawValue(), valueType, null, null);
    }

    void init() {
    }

    void setParent(ConfigurationItem parent) {
        this.parent = parent;
        if (this.instance == null) {
            this.instance = parent.instance;
        }
    }

    private ConfigurationInstance getInstance() {
        if (this.instance == null && this.parent != null) {
            this.instance = this.parent.getInstance();
        }
        if (this.instance == null) {
            throw new IllegalStateException("Configuration is not loaded.");
        }
        return this.instance;
    }

    void setInstance(ConfigurationInstance instance) {
        this.instance = instance;
    }

    protected FileConfiguration getConfig() {
        return getInstance().getConfig();
    }

    protected Object getRawValue() {
        Object value = getConfig().get(getFieldName());
        if (value != null) {
            return value;
        }

        for (String deprecatedName : getDeprecatedFieldNames()) {
            value = getConfig().get(deprecatedName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
