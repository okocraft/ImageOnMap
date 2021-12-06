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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ConfigurationMap<K, V> extends ConfigurationItem<Map<K, V>> implements Map<K, V>, Iterable<Entry<K, V>> {
    private final Class<K> keyType;
    private final Class<V> valueType;

    /**
     * Creates a new configuration map.
     */
    public ConfigurationMap(String name, Map<K, V> defaultValue, Class<K> keyType, Class<V> valueType,
                            String... deprecatedNames) {
        super(name, defaultValue, deprecatedNames);
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    protected Map<K, V> getValue(Object value) throws ConfigurationParseException {
        if (value == null) {
            return null;
        }

        return ConfigurationValueHandlers.handleMapValue(value, keyType, valueType, this);
    }

    @Override
    boolean validate() {
        boolean isValid = super.validate();

        if (ConfigurationSection.class.isAssignableFrom(valueType)) {
            for (Object value : values()) {
                if (!((ConfigurationSection) value).validate()) {
                    isValid = false;
                }
            }
        }

        return isValid;
    }

    @Override
    public int size() {
        return get().size();
    }

    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return get().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return get().containsValue(value);
    }

    @Override
    public V get(Object key) {
        return get().get(key);
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(get().keySet());
    }

    @Override
    public Collection<V> values() {
        return Collections.unmodifiableCollection(get().values());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(get().entrySet());
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return entrySet().iterator();
    }
}
