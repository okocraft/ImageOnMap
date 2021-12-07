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

import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


enum NBTType {
    TAG_END((byte) 0, null, Void.class),
    TAG_BYTE((byte) 1, "NBTTagByte", byte.class, Byte.class),
    TAG_SHORT((byte) 2, "NBTTagShort", short.class, Short.class),
    TAG_INT((byte) 3, "NBTTagInt", int.class, Integer.class),
    TAG_LONG((byte) 4, "NBTTagLong", long.class, Long.class),
    TAG_FLOAT((byte) 5, "NBTTagFloat", float.class, Float.class),
    TAG_DOUBLE((byte) 6, "NBTTagDouble", double.class, Double.class),
    TAG_BYTE_ARRAY((byte) 7, "NBTTagByteArray", byte[].class),
    TAG_INT_ARRAY((byte) 11, "NBTTagIntArray", int[].class),
    TAG_STRING((byte) 8, "NBTTagString", String.class),
    TAG_LIST((byte) 9, "NBTTagList", List.class),
    TAG_COMPOUND((byte) 10, "NBTTagCompound", Map.class);

    // Unique NBT type id
    private final byte id;
    private final Class[] types;
    private final String nmsClassName;
    private Class nmsClass;

    NBTType(byte id, String nmsClassName, Class... types) {
        this.id = id;
        this.types = types;
        this.nmsClassName = "nbt." + nmsClassName;
    }

    public static NBTType fromId(byte id) {
        for (NBTType type : NBTType.values()) {
            if (id == type.id) {
                return type;
            }
        }

        throw new IllegalArgumentException("Illegal type id: " + id);
    }

    public static NBTType fromNmsNbtTag(Object nmsNbtTag) {
        try {
            return fromId((byte) Reflection.call(nmsNbtTag, "getTypeId"));
        } catch (Exception ex) {
            throw new NBTException("Unable to retrieve type of nbt tag", ex);
        }
    }

    public static NBTType fromClass(Class klass) {
        for (NBTType type : NBTType.values()) {
            if (type.isAssignableFrom(klass)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Illegal type class: " + klass);
    }

    public String getNmsTagFieldName() {
        return switch (this) {
            case TAG_COMPOUND -> "tags";//Modified in 1.17 from map to tags
            //TODO add a version check here
            case TAG_LIST -> "list";
            default -> "data";
        };
    }

    public Class[] getJavaTypes() {
        return types;
    }

    public boolean isAssignableFrom(Class otherType) {
        for (Class type : types) {
            if (type.isAssignableFrom(otherType)) {
                return true;
            }
        }

        return false;
    }

    public int getId() {
        return id;
    }

    public String getNmsClassName() {
        return nmsClassName;
    }

    public Class getNmsClass() {
        if (nmsClassName == null) {
            return null;
        }

        try {
            if (nmsClass == null) {
                nmsClass = Reflection.getMinecraftClassByName(nmsClassName);
            }
        } catch (Exception ex) {
            throw new NBTException("Unable to retrieve NBT tag class", ex);
        }

        return nmsClass;
    }

    public Object newTag(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Contents of a tag cannot be null");
        }
        if (!isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(
                    "Invalid content type '" + value.getClass() + "' for tag " + nmsClassName);
        }

        try {
            final Object tag;
            switch (this) {
                case TAG_COMPOUND -> {
                    // ImageOnMap.getPlugin().getLogger().info("Tag_compound");
                    tag = Reflection.instantiate(getNmsClass());
                    if (value instanceof NBTCompound) {
                        setData(tag, ((NBTCompound) value).nmsNbtMap);
                    } else {
                        new NBTCompound(tag).putAll((Map) value);
                    }
                }
                case TAG_LIST -> {
                    // ImageOnMap.getPlugin().getLogger().info("Tag_list");
                    tag = Reflection.instantiate(getNmsClass());
                    if (value instanceof NBTList) {
                        setData(tag, ((NBTList) value).nmsNbtList);
                    } else {
                        new NBTList(tag).addAll((List) value);
                    }

                    // If a NBTTagList is built from scratch, the NMS object is created lately
                    // and may not have the list's type registered at this point.
                    NBTList.guessAndWriteTypeToNbtTagList(tag);
                }
                default -> {
                    // ImageOnMap.getPlugin().getLogger().info("default");
                    Constructor cons = Reflection.findConstructor(getNmsClass(), 1);
                    cons.setAccessible(true);
                    tag = cons.newInstance(value);
                }
            }

            return tag;
        } catch (Exception ex) {
            throw new NBTException("Unable to create NBT tag", ex);
        }
    }

    public Object getData(Object nmsNbtTag) {
        if (nmsNbtTag == null) {
            return null;
        }
        try {
            return Reflection.getFieldValue(nmsNbtTag, getNmsTagFieldName());
        } catch (Exception ex) {
            try {
                return Reflection.call(Reflection.getMinecraftClassByName("nbt.NBTTagCompound"), nmsNbtTag, "h");
            } catch (Exception e) {
                throw new NBTException("Unable to retrieve NBT tag data", e);
            }
        }
    }

    public void setData(Object nmsNbtTag, Object value) {
        try {
            Reflection.setFieldValue(nmsNbtTag, getNmsTagFieldName(), value);

        } catch (Exception ex) {
            try {
                // ImageOnMap.getPlugin().getLogger().info(getNmsTagFieldName() + " value: " + value);
                Class nbtBaseClass = Reflection.getMinecraftClassByName(
                        "nbt.NBTBase");
                //Todo Maybe an issue here with older naming that used NBTbase instead of NBTBase
                Class nbtTagCompoundClass = Reflection.getMinecraftClassByName("nbt.NBTTagCompound");
                //Object nbtTagCompound = Reflection.instantiate(nbtTagCompoundClass);
                //Object nbtBase = nbtBaseClass.cast(nbtTagCompound);
                if (value instanceof ArrayList) {
                    ArrayList<Object> valueList = ((ArrayList<Object>) value);
                    for (Object val : valueList) {

                        // ImageOnMap.getPlugin().getLogger().info(value + " " + val.getClass().getName());
                    }
                } else {
                    Map<String, Object> valueMap = ((Map<String, Object>) value);
                    for (Map.Entry<String, Object> entry : valueMap.entrySet()) {
                        // ImageOnMap.getPlugin().getLogger().info("key " + entry.getKey() + " Value: " + entry.getValue());
                        // ImageOnMap.getPlugin().getLogger().info("" + entry.getValue().getClass().getName());
                    }
                }
                //Reflection.call(Reflection.getMinecraftClassByName("nbt.NBTTagCompound"), nmsNbtTag, "set",
                //        getNmsTagFieldName(),
                //        value);
                //creer nouveau ntbtagcompound avec les nouvelles valeurs, faire le set.
                //NBTBase nbtbase = (NBTBase) this.tags.get(s);
            } catch (Exception e) {
                throw new NBTException("Unable to set NBT tag data", e);
            }
        }
    }
}
