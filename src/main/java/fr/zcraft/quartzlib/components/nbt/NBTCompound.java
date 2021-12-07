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

package fr.zcraft.quartzlib.components.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the NBT Compound tag type.
 * <p>It implements all operations of {@link Map} with a String key,
 * as well as a few specific operations for NBT data.</p>
 */
public class NBTCompound implements Map<String, Object> {
    private final Object parent;
    private final Object parentKey;
    Map<String, Object> nmsNbtMap;
    private Object nmsNbtTag;

    /**
     * Created a new empty NBT compound.
     * It is not linked to any item, therefore it is equivalent of using directly
     * a {@link Map}&lt;{@link String},{@link Object}&gt;.
     */
    public NBTCompound() {
        this.nmsNbtTag = null;
        this.nmsNbtMap = new HashMap<>();
        this.parent = null;
        this.parentKey = null;
    }

    NBTCompound(Object nativeNbtTag) {
        this.nmsNbtTag = nativeNbtTag;
        if (nativeNbtTag == null) {
            this.nmsNbtMap = new HashMap<>();
        } else {
            this.nmsNbtMap = (Map<String, Object>) NBTType.TAG_COMPOUND.getData(nmsNbtTag);
        }

        this.parent = null;
        this.parentKey = null;
    }

    NBTCompound(NBTCompound parent, String key) {
        this.parent = parent;
        this.parentKey = key;
        this.nmsNbtMap = null;
        this.nmsNbtTag = null;
    }

    NBTCompound(NBTList parent, int index) {
        this.parent = parent;
        this.parentKey = index;
        this.nmsNbtMap = null;
        this.nmsNbtTag = null;
    }

    private Map<String, Object> getNbtMap() {
        if (nmsNbtMap == null) {
            // ImageOnMap.getPlugin().getLogger().info("nmsNbtMap == null");
            nmsNbtMap = new HashMap<>();
            if (nmsNbtTag != null) {
                // ImageOnMap.getPlugin().getLogger().info("nmsNbtTag != null");
                NBTType.TAG_COMPOUND.setData(nmsNbtTag, nmsNbtMap);
            } else {
                // ImageOnMap.getPlugin().getLogger().info("nmsNbtTag == null");
                nmsNbtTag = NBTType.TAG_COMPOUND.newTag(nmsNbtMap);
                NBTType.TAG_LIST.setData(nmsNbtTag, nmsNbtTag);

                if (parent != null && parentKey != null) {
                    if (parent instanceof NBTCompound) {
                        ((NBTCompound) parent).put((String) parentKey, this);
                    } else if (parent instanceof NBTList) {
                        ((NBTList) parent).set((Integer) parentKey, this);
                    }
                }
            }
        } else {
            // ImageOnMap.getPlugin().getLogger().info("nmsNbtMap != null");
            // ImageOnMap.getPlugin().getLogger().info("nmsNbtMap " + nmsNbtMap.toString());
        }
        return nmsNbtMap;
    }

    /**
     * Returns the NMS NBTTagCompound instance.
     *
     * @return The NMS NBTTagCompound instance.
     */
    Object getNbtTagCompound() {
        return nmsNbtTag;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or the specified default value if this map contains no mapping for the key.
     * If a value is present, but could not be coerced to the given type,
     * it is ignored and the default value is returned instead.
     *
     * @param <T>          The type to coerce the mapped value to.
     * @param key          The key
     * @param defaultValue The default value.
     * @return the value to which the specified key is mapped,
     *         or the specified default value if this map contains no mapping for the key.
     */
    public <T> T get(String key, T defaultValue) {
        return get(key, defaultValue, defaultValue == null ? null : (Class<T>) defaultValue.getClass());
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or the specified default value if this map contains no mapping for the key.
     * If a value is present, but could not be coerced to the given type,
     * it is ignored and the default value is returned instead.
     * This version of the method is recommended if the defaultValue parameter is null,
     * so it can have enough type information to protect against wrong NBT types.
     *
     * @param <T>          The type to coerce the mapped value to.
     * @param key          The key
     * @param defaultValue The default value.
     * @param valueType    The type of the expected value.
     * @return the value to which the specified key is mapped,
     *         or the specified default value if this map contains no mapping for the key.
     */
    public <T> T get(String key, T defaultValue, Class<T> valueType) {
        try {
            Object value = get(key);
            if (value == null) {
                return defaultValue;
            }
            if (valueType != null) {
                if (!valueType.isAssignableFrom(value.getClass())) {
                    return defaultValue;
                }
            }
            return (T) value;
        } catch (ClassCastException | NBTException ex) {
            return defaultValue;
        }
    }

    @Override
    public Object get(Object key) {
        return nmsNbtMap == null ? null : NBT.toNativeValue(nmsNbtMap.get(key));
    }

    /**
     * Returns the Compound tag to which the specified key is mapped.
     * If the value mapped to the key is not a compound tag, a new empty tag
     * is returned, and the existing value is overwritten if anything is added
     * to the tag.
     *
     * @param key The key.
     * @return the Compound tag to which the specified key is mapped.
     */
    public NBTCompound getCompound(String key) {
        return get(key, new NBTCompound(this, key));
    }

    /**
     * Returns the List tag to which the specified key is mapped.
     * If the value mapped to the key is not a list tag, a new empty tag
     * is returned, and the existing value is overwritten if anything is added
     * to the tag.
     *
     * @param key The key.
     * @return the List tag to which the specified key is mapped.
     */
    public NBTList getList(String key) {
        return get(key, new NBTList(this, key));
    }

    @Override
    public int size() {
        return nmsNbtMap == null ? 0 : nmsNbtMap.size();
    }

    @Override
    public boolean isEmpty() {
        return nmsNbtMap == null || nmsNbtMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return nmsNbtMap != null && nmsNbtMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return nmsNbtMap != null && nmsNbtMap.containsValue(NBT.fromNativeValue(value));
    }

    @Override
    public Object put(String key, Object value) {
        // ImageOnMap.getPlugin().getLogger().info("put nbt value");
        // ImageOnMap.getPlugin().getLogger().info(getNbtMap().toString());
        // ImageOnMap.getPlugin().getLogger().info("KEY " + key);
        // ImageOnMap.getPlugin().getLogger().info("VALUE " + NBT.fromNativeValue(value));
        if (!(nmsNbtMap instanceof HashMap)) {
            nmsNbtMap = new HashMap<>(getNbtMap());
        }
        return NBT.toNativeValue(nmsNbtMap.put(key, NBT.fromNativeValue(value)));
    }

    @Override
    public Object remove(Object key) {
        return nmsNbtMap == null ? null : NBT.toNativeValue(nmsNbtMap.remove(key));
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        for (Entry<? extends String, ?> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        if (nmsNbtMap == null) {
            return;
        }
        nmsNbtMap.clear();
    }

    @Override
    public Set<String> keySet() {
        return nmsNbtMap == null ? new HashSet<String>() : nmsNbtMap.keySet();
    }

    @Override
    public Collection<Object> values() {
        final ArrayList<Object> list = new ArrayList<>(size());
        if (nmsNbtMap == null) {
            return list;
        }

        for (Object value : nmsNbtMap.values()) {
            list.add(NBT.toNativeValue(value));
        }
        return list;
    }

    /**
     * Returns this NBT component as a JSON string usable by Minecraft (in things like tellraws).
     *
     * @return A JSON representation of this component.
     * @see NBT#toNBTJSONString(Object) The underlying export method used.
     */
    @Override
    public String toString() {
        return NBT.toNBTJSONString(this);
    }

    /**
     * Returns a new HashMap containing the keys of this NBT compound. The returned
     * HashMap is independent from the compound and can be used for export and save.
     *
     * @return A new HashMap containing the data stored inside this compound.
     * @see NBT#addToItemStack(ItemStack, Map, boolean) The method used to add these exported tags back inside an item.
     */
    public Map<String, Object> toHashMap() {
        return new HashMap<>(this);
    }

    @Override
    public @NotNull Set<Entry<String, Object>> entrySet() {
        HashSet<Entry<String, Object>> set = new HashSet<>(size());
        if (nmsNbtMap == null) {
            return set;
        }

        for (Entry<String, Object> entry : nmsNbtMap.entrySet()) {
            set.add(new NbtCompoundEntry(entry.getKey()));
        }

        return set;
    }

    private class NbtCompoundEntry implements Entry<String, Object> {
        private final String key;

        public NbtCompoundEntry(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return get(key);
        }

        @Override
        public Object setValue(Object value) {
            return put(key, value);
        }
    }
}
