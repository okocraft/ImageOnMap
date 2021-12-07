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

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;


/**
 * This class represents the NBT List tag type.
 * <p>It implements all operations of {@link List}, as well as a few specific operations for NBT data.</p>
 */
public class NBTList implements List<Object> {
    private final Object parent;
    private final Object parentKey;
    List<Object> nmsNbtList;
    private Object nmsNbtTag;
    private NBTType type = NBTType.TAG_END;

    /**
     * Created a new empty NBT list.
     * It is not linked to any item, therefore it is equivalent of using directly
     * a {@link List}&lt;{@link Object}&gt;.
     */
    public NBTList() {
        this(null, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    NBTList(Object nmsListTag) {
        this(null, nmsListTag == null ? new ArrayList<>() : (List<Object>) NBTType.TAG_LIST.getData(nmsListTag));
    }

    private NBTList(Object nmsListTag, List<Object> nmsNbtList) {
        this.nmsNbtTag = nmsListTag;
        this.nmsNbtList = nmsNbtList;
        this.parent = null;
        this.parentKey = null;

        setTypeFromNbtTag();
    }

    NBTList(NBTCompound parent, String parentKey) {
        this.nmsNbtList = null;
        this.nmsNbtTag = null;
        this.parent = parent;
        this.parentKey = parentKey;
    }

    NBTList(NBTList parent, int index) {
        this.nmsNbtList = null;
        this.nmsNbtTag = null;
        this.parent = parent;
        this.parentKey = index;
    }

    static void guessAndWriteTypeToNbtTagList(Object nmsNbtTag) {
        try {
            final NBTType currentType = NBTType.fromId((byte) Reflection.call(nmsNbtTag, "e"));
            //public byte e() {
            //        return this.type;
            //    }


        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException  e) {
            try {
                final NBTType currentType = NBTType.fromId((byte) Reflection.getFieldValue(nmsNbtTag, "type"));

                if (currentType.equals(NBTType.TAG_END)) {
                    // We retrieve the first element of the internal list and use it as
                    // the list type, if the list is not empty.
                    @SuppressWarnings("unchecked") final List<Object> internalNbtList =
                            (List<Object>) Reflection.getFieldValue(nmsNbtTag, "list");

                    if (!internalNbtList.isEmpty()) {
                        Reflection.setFieldValue(nmsNbtTag, "type",
                                (byte) NBTType.fromNmsNbtTag(internalNbtList.get(0)).getId());
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                PluginLogger
                        .error("Unable to set NBTTagList's type."
                                + " Such malformed lists cannot be read by Minecraft most of the time.", e);
            }
        }

    }

    private List<Object> getNbtList() {
        if (nmsNbtList == null) {
            nmsNbtList = new ArrayList<>();
            if (nmsNbtTag != null) {
                NBTType.TAG_LIST.setData(nmsNbtTag, nmsNbtList);
                setTypeFromNbtTag();
            } else {
                nmsNbtTag = NBTType.TAG_LIST.newTag(nmsNbtList);
                NBTType.TAG_LIST.setData(nmsNbtTag, nmsNbtList);

                setTypeFromNbtList();
                writeTypeToNbtTag();

                if (parent != null && parentKey != null) {
                    if (parent instanceof NBTCompound) {
                        ((NBTCompound) parent).put((String) parentKey, this);
                    } else if (parent instanceof NBTList) {
                        ((NBTList) parent).set((Integer) parentKey, this);
                    }
                }
            }
        }

        return nmsNbtList;
    }

    private void setTypeFromNbtTag() {
        if (nmsNbtTag != null) {
            try {
                this.type = NBTType.fromId((byte) Reflection.getFieldValue(nmsNbtTag, "type"));
            } catch (NoSuchFieldException | IllegalAccessException e) {
                ImageOnMap.getPlugin().getLogger().error("Unable to retrieve NBTTagList's type."
                        + " The type will be guessed next time an element is inserted into the list…", e);
            }
        }
    }

    private void setTypeFromNbtList() {
        if (nmsNbtList != null && !nmsNbtList.isEmpty()) {
            setType(NBTType.fromNmsNbtTag(nmsNbtList.get(0)));
        }
    }

    private void writeTypeToNbtTag() {
        if (nmsNbtTag != null) {
            try {
                Reflection.setFieldValue(nmsNbtTag, "type", (byte) type.getId());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                PluginLogger
                        .error("Unable to set NBTTagList's type."
                                + " Such malformed lists cannot be read by Minecraft most of the time.", e);
            }
        }
    }

    public NBTType getType() {
        return type;
    }

    private void setType(NBTType type) {
        this.type = type;
        writeTypeToNbtTag();
    }

    private void checkType(Object o) {
        if (type == null || type.equals(NBTType.TAG_END)) {
            try {
                setType(NBTType.fromClass(o.getClass()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Illegal type class in a NBT list: " + o.getClass(), e);
            }

            return;
        }

        if (!type.isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException("Illegal type class in a NBT list: " + o.getClass());
        }
    }


    @Override
    public int size() {
        return nmsNbtList == null ? 0 : nmsNbtList.size();
    }

    @Override
    public boolean isEmpty() {
        return nmsNbtList == null || nmsNbtList.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nmsNbtList != null && nmsNbtList.contains(NBT.fromNativeValue(o));
    }

    @Override
    public @NotNull Iterator<Object> iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[] {});
    }

    @Override
    public <T> T[] toArray(T[] a) {
        ArrayList<T> list = new ArrayList<>(size());
        if (nmsNbtList != null) {
            list.addAll((List<T>) this);
        }
        return list.toArray(a);
    }

    @Override
    public boolean add(Object e) {
        checkType(e);
        final boolean added = getNbtList().add(NBT.fromNativeValue(e));

        writeTypeToNbtTag();

        return added;
    }

    @Override
    public void add(int index, Object element) {
        checkType(element);
        getNbtList().add(index, NBT.fromNativeValue(element));
        writeTypeToNbtTag();
    }


    @Override
    public boolean remove(Object o) {
        return nmsNbtList != null && nmsNbtList.remove(NBT.fromNativeValue(o));
    }

    @Override
    public Object remove(int index) {
        if (nmsNbtList == null) {
            throw new IndexOutOfBoundsException("NBT list is empty");
        }
        return NBT.toNativeValue(nmsNbtList.remove(index));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        if (nmsNbtList == null) {
            return false;
        }

        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<?> c) {
        boolean changed = false;
        for (Object value : c) {
            checkType(value);
            if (getNbtList().add(NBT.fromNativeValue(value))) {
                changed = true;
            }
        }

        writeTypeToNbtTag();

        return changed;
    }

    @Override
    public boolean addAll(int index, Collection<?> c) {
        int i = 0;
        for (Object o : c) {
            checkType(o);
            add(i + index, o);
            ++i;
        }

        writeTypeToNbtTag();

        return !c.isEmpty();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        if (nmsNbtList == null) {
            return false;
        }

        boolean changed = false;
        for (Object value : c) {
            if (remove(value)) {
                changed = true;
            }
        }

        return changed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        if (nmsNbtList == null) {
            return false;
        }

        boolean changed = false;

        for (Object value : this) {
            if (c.contains(value)) {
                if (nmsNbtList.remove(value)) {
                    changed = true;
                }
            }
        }

        return changed;
    }

    @Override
    public void clear() {
        if (nmsNbtList == null) {
            return;
        }

        nmsNbtList.clear();
    }

    @Override
    public Object get(int index) {
        if (nmsNbtList == null) {
            throw new ArrayIndexOutOfBoundsException("NBT List is empty");
        }
        return NBT.toNativeValue(nmsNbtList.get(index));
    }

    /**
     * Returns the value at the specified position in this list, or the specified default value if this value is null.
     * If a value is present, but could not be coerced to the given type,
     * it is ignored and the default value is returned instead.
     *
     * @param <T>          The type to coerce the indexed value to.
     * @param index        The position
     * @param defaultValue The default value.
     * @return the value at the specified position in this list, or the specified default value if this value is null.
     */
    public <T> T get(int index, T defaultValue) {
        try {
            Object value = get(index);
            if (value == null) {
                return defaultValue;
            }
            return (T) value;
        } catch (ClassCastException | NBTException ex) {
            return defaultValue;
        }
    }

    /**
     * Returns the Compound tag at the specified index.
     * If the value at the specified index is not a compound tag, a new empty tag
     * is returned, and the existing value is overwritten if anything is added
     * to the tag.
     *
     * @param index The index.
     * @return the Compound tag at the specified index.
     */
    public NBTCompound getCompound(int index) {
        return get(index, new NBTCompound(this, index));
    }

    /**
     * Returns the Compound tag at the specified index.
     * If the value at the specified index is not a compound tag, a new empty tag
     * is returned, and the existing value is overwritten if anything is added
     * to the tag.
     *
     * @param index The index.
     * @return the Compound tag at the specified index.
     */
    public NBTList getList(int index) {
        return get(index, new NBTList(this, index));
    }

    @Override
    public Object set(int index, Object element) {
        checkType(element);
        final Object oldValue = NBT.toNativeValue(getNbtList().set(index, NBT.fromNativeValue(element)));

        writeTypeToNbtTag();

        return oldValue;
    }

    @Override
    public int indexOf(Object o) {
        if (nmsNbtList == null) {
            return -1;
        }
        return nmsNbtList.indexOf(NBT.fromNativeValue(o));
    }

    @Override
    public int lastIndexOf(Object o) {
        if (nmsNbtList == null) {
            return -1;
        }
        return nmsNbtList.lastIndexOf(NBT.fromNativeValue(o));
    }

    @Override
    public @NotNull ListIterator<Object> listIterator() {
        return listIterator(0);
    }

    @Override
    public @NotNull ListIterator<Object> listIterator(int index) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Index is out of bounds: " + index);
        }

        if (nmsNbtList == null) {
            return new NbtListIterator(null);
        }

        return new NbtListIterator(nmsNbtList.listIterator(index));
    }

    @Override
    public @NotNull NBTList subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex > size()) {
            throw new IndexOutOfBoundsException("fromIndex is out of bounds: " + fromIndex);
        }

        if (toIndex < 0 || toIndex > size()) {
            throw new IndexOutOfBoundsException("toIndex is out of bounds: " + toIndex);
        }

        if (nmsNbtList == null) {
            return new NBTList(nmsNbtTag, null);
        }

        return new NBTList(nmsNbtTag, nmsNbtList.subList(fromIndex, toIndex));
    }

    /**
     * Returns a new filtering iterator for the given type.
     * This special iterator will iterate over the list, but will skip all
     * values that cannot be coerced to the specified type.
     *
     * @param <T>   The type the values need to be coerced to.
     * @return The filtered iterable.
     */
    public <T> Iterable<T> filter() {
        return new NbtListFilterIterator<>(listIterator());
    }

    /**
     * Returns a new filtering iterator for the given type.
     * This special iterator will iterate over the list, but will skip all
     * values that cannot be coerced to the specified type.
     *
     * @param <T>   The type the values need to be coerced to.
     * @param index The index to start the iteration at.
     * @return The filtered iterator.
     */
    public <T> ListIterator<T> filter(int index) {
        return new NbtListFilterIterator<>(listIterator(index));
    }

    @Override
    public String toString() {
        return NBT.toNBTJSONString(this);
    }

    private class NbtListIterator implements ListIterator<Object> {
        private ListIterator<Object> iterator;

        NbtListIterator(ListIterator<Object> iterator) {
            this.iterator = iterator;
        }

        private ListIterator<Object> getIterator() {
            if (iterator == null) {
                iterator = getNbtList().listIterator();
            }

            return iterator;
        }

        @Override
        public boolean hasNext() {
            if (iterator == null) {
                return false;
            }
            return iterator.hasNext();
        }

        @Override
        public Object next() {
            if (iterator == null) {
                throw new NoSuchElementException("NBT List is empty");
            }
            return NBT.toNativeValue(iterator.next());
        }

        @Override
        public boolean hasPrevious() {
            if (iterator == null) {
                return false;
            }
            return iterator.hasPrevious();
        }

        @Override
        public Object previous() {
            if (iterator == null) {
                throw new NoSuchElementException("NBT List is empty");
            }
            return NBT.toNativeValue(iterator.previous());
        }

        @Override
        public int nextIndex() {
            if (iterator == null) {
                throw new NoSuchElementException("NBT List is empty");
            }
            return iterator.nextIndex();
        }

        @Override
        public int previousIndex() {
            if (iterator == null) {
                throw new NoSuchElementException("NBT List is empty");
            }
            return iterator.previousIndex();
        }

        @Override
        public void remove() {
            if (iterator == null) {
                throw new NoSuchElementException("NBT List is empty");
            }
            iterator.remove();
        }

        @Override
        public void set(Object e) {
            checkType(e);
            getIterator().set(NBT.fromNativeValue(e));
            writeTypeToNbtTag();
        }

        @Override
        public void add(Object e) {
            checkType(e);
            getIterator().add(NBT.fromNativeValue(e));
            writeTypeToNbtTag();
        }

    }

    private class NbtListFilterIterator<T> implements ListIterator<T>, Iterable<T> {
        private final ListIterator<Object> baseIterator;
        private T previousItem;
        private T nextItem;

        private int previousIndex;
        private int nextIndex;

        NbtListFilterIterator(ListIterator<Object> baseIterator) {
            this.baseIterator = baseIterator;
            previousItem = null;
            nextItem = fetchNext();
        }

        @Override
        public boolean hasNext() {
            return nextItem != null;
        }

        @Override
        public T next() {
            if (nextItem == null) {
                throw new NoSuchElementException();
            }

            previousItem = nextItem;
            previousIndex = nextIndex;
            nextItem = fetchNext();
            return previousItem;
        }

        private T fetchNext() {
            while (true) {
                if (!baseIterator.hasNext()) {
                    return null;
                }
                try {
                    ++nextIndex;
                    return (T) baseIterator.next();
                } catch (ClassCastException ignored) {
                }
            }
        }

        private T fetchPrevious() {
            while (true) {
                if (!baseIterator.hasPrevious()) {
                    return null;
                }
                try {
                    --previousIndex;
                    return (T) baseIterator.previous();
                } catch (ClassCastException ignored) {
                }
            }
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return this;
        }

        @Override
        public boolean hasPrevious() {
            return previousItem != null;
        }

        @Override
        public T previous() {
            nextItem = previousItem;
            previousItem = fetchPrevious();
            return nextItem;
        }

        @Override
        public int nextIndex() {
            if (!baseIterator.hasNext()) {
                return -1;
            }
            return nextIndex;
        }

        @Override
        public int previousIndex() {
            if (!baseIterator.hasPrevious()) {
                return -1;
            }
            return previousIndex;
        }

        @Override
        public void remove() {
            baseIterator.remove();
        }

        @Override
        public void set(T e) {
            checkType(e);
            baseIterator.set(e);
            writeTypeToNbtTag();
        }

        @Override
        public void add(T e) {
            checkType(e);
            baseIterator.add(e);
            writeTypeToNbtTag();
        }
    }
}
