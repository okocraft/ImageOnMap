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

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ConfigurationSection
        extends ConfigurationItem<Map>
        implements Map<String, ConfigurationItem>,
        Iterable<ConfigurationItem> {
    private final HashMap<String, ConfigurationItem> items = new HashMap<String, ConfigurationItem>();

    protected ConfigurationSection() {
        this(null);
    }

    private ConfigurationSection(String fieldName, String... deprecatedNames) {
        super(fieldName, null, Map.class, deprecatedNames);
    }

    @Override
    void init() {
        super.init();
        for (Field field : this.getClass().getFields()) {
            if (ConfigurationItem.class.isAssignableFrom(field.getType())) {
                try {
                    ConfigurationItem<?> item = (ConfigurationItem<?>) field.get(this);
                    item.setParent(this);
                    item.init();
                    items.put(field.getName().toUpperCase(), item);
                } catch (Exception ignored) { }
            }
        }
    }

    @Override
    boolean validate() {
        boolean isValid = true;

        for (ConfigurationItem item : items.values()) {
            if (!item.validate()) {
                isValid = false;
            }
        }

        return isValid;
    }


    @Override
    public Map<String, Object> get() {
        return getConfig().getConfigurationSection(getFieldName()).getValues(true);
    }

    @Override
    public ConfigurationItem get(Object key) {
        return items.get(key.toString().toUpperCase());
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return items.containsKey(key.toString().toUpperCase());
    }

    @Override
    public boolean containsValue(Object value) {
        return items.containsValue(value);
    }

    @Override
    public ConfigurationItem put(String key, ConfigurationItem value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConfigurationItem remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends ConfigurationItem> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(items.keySet());
    }

    @Override
    public Collection<ConfigurationItem> values() {
        return Collections.unmodifiableCollection(items.values());
    }

    @Override
    public Set<Entry<String, ConfigurationItem>> entrySet() {
        return Collections.unmodifiableSet(items.entrySet());
    }

    @Override
    public Iterator<ConfigurationItem> iterator() {
        return items.values().iterator();
    }
}
