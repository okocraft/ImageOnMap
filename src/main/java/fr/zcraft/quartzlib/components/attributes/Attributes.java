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

package fr.zcraft.quartzlib.components.attributes;

import fr.zcraft.quartzlib.components.nbt.NBT;
import fr.zcraft.quartzlib.components.nbt.NBTCompound;
import fr.zcraft.quartzlib.components.nbt.NBTException;
import fr.zcraft.quartzlib.components.nbt.NBTList;
import fr.zcraft.quartzlib.tools.reflection.NMSException;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

/**
 * This class represents an item's attribute list, and provides a few static
 * utilities to manipulate the attribute list of an item..
 * It implements all operations of {@link List} for
 * {@link Attribute},
 * as well as a few specific operations for item attributes.
 */
public class Attributes implements List<Attribute> {
    private final NBTList rawAttributes;

    private Attributes(NBTList rawAttributes) {
        this.rawAttributes = rawAttributes;
    }

    /**
     * Returns the attribute list for the given item.
     *
     * @param item The item to retreive the attribute list from.
     * @return the attribute list for the given item.
     * @throws NMSException if anything failed while accessing NMS classes.
     *     See {@link NMSException} for more information.
     * @throws NBTException if anything failed while accessing NBT data.
     *     See {@link NBTException} for more information.
     */
    public static Attributes get(ItemStack item) throws NMSException, NBTException {
        if (item == null) {
            throw new IllegalArgumentException("The given item cannot be null");
        }

        return new Attributes(NBT.fromItemStack(item).getList("AttributeModifiers"));
    }

    /**
     * Finds and returns the first attribute  of the specified item with
     * the specified UUID, or null if such an attribute was not found.
     *
     * @param item The item to retrieve the attribute from.
     * @param uuid Thee UUID to search for.
     * @return the first attribute, or null if none was found.
     * @throws NMSException if anything failed while accessing NMS classes.
     *     See {@link NMSException} for more information.
     * @throws NBTException if anything failed while accessing NBT data.
     *     See {@link NBTException} for more information.
     */
    public static Attribute get(ItemStack item, UUID uuid) throws NMSException, NBTException {
        return get(item).find(uuid);
    }

    /**
     * Finds and returns the first attribute  of the specified item with
     * the specified UUID, or null if such an attribute was not found.
     * The attribute is returned as a new instance of the specified class.
     * The class must be publicly accessible, and have an accessible default constructor.
     *
     * @param <T>           The type of the attribute to return.
     * @param item          The item to retrieve the attribute from.
     * @param uuid          Thee UUID to search for.
     * @param attributeType The class of the attribute to return.
     * @return the first attribute, or null if none was found.
     * @throws NMSException if anything failed while accessing NMS classes.
     *     See {@link NMSException} for more information.
     * @throws NBTException if anything failed while accessing NBT data.
     *     See {@link NBTException} for more information.
     */
    public static <T extends Attribute> T get(ItemStack item, UUID uuid, Class<T> attributeType)
            throws NMSException, NBTException {
        return get(item).find(uuid, attributeType);
    }

    /**
     * Returns the attribute at the specified position in this list.
     * The attribute is returned as a new instance of the specified class.
     * The class must be publicly accessible, and have an accessible default constructor.
     *
     * @param <T>           The type of the attribute to return.
     * @param index         {@inheritDoc}
     * @param attributeType The class of the attribute to return.
     * @return The attribute.
     */
    public <T extends Attribute> T get(int index, Class<T> attributeType) {
        T attribute;
        try {
            attribute = Reflection.instantiate(attributeType);
        } catch (Exception ex) {
            throw new NBTException("Unable to instanciate attribute type", ex);
        }
        attribute.nbt = rawAttributes.getCompound(index);
        return attribute;
    }

    @Override
    public Attribute get(int index) {
        return new Attribute(rawAttributes.getCompound(index));
    }

    /**
     * Replaces the first attribute of the specified item having the specified attribute's UUID
     * with the specified attribute.
     * If such an attribute is not found in this list, the specified element is appended to the list.
     *
     * @param item      The item to replace the attribute of.
     * @param attribute The attribute to replace and to get the UUID from.
     * @throws NMSException if anything failed while accessing NMS classes.
     *     See {@link NMSException} for more information.
     * @throws NBTException if anything failed while accessing NBT data.
     *     See {@link NBTException} for more information.
     */
    public static void set(ItemStack item, Attribute attribute) throws NMSException, NBTException {
        get(item).set(attribute.getUUID(), attribute);
    }

    /**
     * Replaces the first attribute having the specified UUID with the specified element.
     * If such an attribute is not found in this list, the specified element is appended to the list.
     *
     * @param uuid    The UUID of the attribute to replace
     * @param element attribute to be stored.
     * @return the element previously stored having the specified UUID, or {@code null} if it was not found.
     */
    public Attribute set(UUID uuid, Attribute element) {
        int index = indexOf(uuid);
        if (index == -1) {
            add(element);
            return null;
        }

        return set(index, element);
    }

    @Override
    public Attribute set(int index, Attribute element) {
        Attribute currentAttribute = get(index);
        rawAttributes.set(index, element.getNBTCompound());
        return currentAttribute;
    }

    @Override
    public int size() {
        return rawAttributes.size();
    }

    @Override
    public boolean isEmpty() {
        return rawAttributes.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (!(o instanceof Attribute)) {
            return false;
        }
        return rawAttributes.contains(((Attribute) o).getNBTCompound());
    }

    @Override
    public Iterator<Attribute> iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[] {});
    }

    @Override
    public <T> T[] toArray(T[] a) {
        ArrayList<T> list = new ArrayList<T>(size());
        list.addAll((List<T>) this);
        return list.toArray(a);
    }

    @Override
    public boolean add(Attribute e) {
        return rawAttributes.add(e.getNBTCompound());
    }

    @Override
    public void add(int index, Attribute element) {
        rawAttributes.add(index, element.getNBTCompound());
    }

    @Override
    public boolean remove(Object o) {
        if (!(o instanceof Attribute)) {
            return false;
        }
        return rawAttributes.remove(((Attribute) o).getNBTCompound());
    }

    @Override
    public Attribute remove(int index) {
        Attribute currentAttribute = get(index);
        rawAttributes.remove(index);
        return currentAttribute;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends Attribute> c) {
        boolean changed = false;
        for (Attribute value : c) {
            if (add(value)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<? extends Attribute> c) {
        int i = 0;
        for (Attribute o : c) {
            add(i + index, o);
            ++i;
        }

        return !c.isEmpty();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean changed = false;
        for (Object o : c) {
            if (remove(o)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean changed = false;

        for (Attribute value : this) {
            if (c.contains(value)) {
                if (remove(value)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    @Override
    public void clear() {
        rawAttributes.clear();
    }

    @Override
    public int indexOf(Object o) {
        if (!(o instanceof Attribute)) {
            return -1;
        }
        return rawAttributes.indexOf(((Attribute) o).getNBTCompound());
    }

    /**
     * Returns the index of the first occurrence of an attribute with the
     * specified UUID in this list,
     * or -1 if this list does not contain the element.
     *
     * @param uuid The UUID to search for.
     * @return the index of the first occurrence of an attribute with the specified UUID in this list,
     *     or -1 if this list does not contain the element.
     */
    public int indexOf(UUID uuid) {
        for (int i = 0; i < size(); ++i) {
            NBTCompound compound = rawAttributes.getCompound(i);
            Long uuidMost = compound.get("UUIDMost", null, Long.class);
            Long uuidLeast = compound.get("UUIDLeast", null, Long.class);
            if (uuidMost == null || uuidLeast == null) {
                continue;
            }

            UUID attributeUuid = new UUID(uuidMost, uuidLeast);
            if (attributeUuid.equals(uuid)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Finds and returns the first attribute with the specified UUID in this
     * list, or null if such an attribute was not found.
     *
     * @param uuid Thee UUID to search for.
     * @return the first attribute, or null if none was found.
     */
    public Attribute find(UUID uuid) {
        int index = indexOf(uuid);
        if (index == -1) {
            return null;
        }
        return get(index);
    }

    /**
     * Finds and returns the first attribute with the specified UUID in this
     * list, or null if such an attribute was not found.
     * The attribute is returned as a new instance of the specified class.
     * The class must be publicly accessible, and have an accessible default constructor.
     *
     * @param <T>           The type of the attribute to return.
     * @param uuid          Thee UUID to search for.
     * @param attributeType The class of the attribute to return.
     * @return the first attribute, or null if none was found.
     */
    public <T extends Attribute> T find(UUID uuid, Class<T> attributeType) {
        int index = indexOf(uuid);
        if (index == -1) {
            return null;
        }
        return get(index, attributeType);
    }

    @Override
    public int lastIndexOf(Object o) {
        if (!(o instanceof Attribute)) {
            return -1;
        }
        return rawAttributes.lastIndexOf(((Attribute) o).getNBTCompound());
    }

    @Override
    public ListIterator<Attribute> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<Attribute> listIterator(int index) {
        return new ItemAttributesIterator(rawAttributes.filter(NBTCompound.class, index));
    }

    @Override
    public Attributes subList(int fromIndex, int toIndex) {
        return new Attributes(rawAttributes.subList(fromIndex, toIndex));
    }

    /**
     * Returns the Minecraft NBT/JSON string representation of this attribute list.
     * See {@link NBT#toNBTJSONString(Object) } for more information.
     *
     * @return the Minecraft NBT/JSON string representation of this attribute list.
     */
    @Override
    public String toString() {
        return NBT.toNBTJSONString(rawAttributes);
    }

    private final class ItemAttributesIterator implements ListIterator<Attribute> {
        private final ListIterator<NBTCompound> iterator;

        public ItemAttributesIterator(ListIterator<NBTCompound> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Attribute next() {
            return new Attribute(iterator.next());
        }

        @Override
        public boolean hasPrevious() {
            return iterator.hasPrevious();
        }

        @Override
        public Attribute previous() {
            return new Attribute(iterator.previous());
        }

        @Override
        public int nextIndex() {
            return iterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            return iterator.previousIndex();
        }

        @Override
        public void remove() {
            iterator.remove();
        }

        @Override
        public void set(Attribute e) {
            iterator.set(e.getNBTCompound());
        }

        @Override
        public void add(Attribute e) {
            iterator.add(e.getNBTCompound());
        }

    }
}
