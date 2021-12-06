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
import java.util.UUID;

/**
 * This class represents an item attribute.
 */
public class Attribute {
    NBTCompound nbt;

    /**
     * Creates a new empty attribute, not linked to any item.
     */
    public Attribute() {
        this(new NBTCompound());
    }

    Attribute(NBTCompound nbt) {
        this.nbt = nbt;
    }

    /**
     * @return the name of this attribute's modifier. It can be any string.
     */
    public final String getName() {
        return nbt.get("Name", null);
    }

    /**
     * Sets the name of this attribute's modifier.
     * This name can be set to any string value.
     *
     * @param name The name.
     */
    public final void setName(String name) {
        nbt.put("Name", name);
    }

    /**
     * Returns the name of the Attribute this Modifier is to act upon.
     * Example: generic.attackDamage
     * See http://minecraft.gamepedia.com/Attribute#Attributes for more information.
     *
     * @return the name of the Attribute this Modifier is to act upon.
     */
    public final String getAttributeName() {
        return nbt.get("AttributeName", null);
    }

    /**
     * Sets the name of the Attribute this Modifier is to act upon.
     *
     * @param attributeName The attribute name.
     */
    public final void setAttributeName(String attributeName) {
        nbt.put("AttributeName", attributeName);
    }

    /**
     * Returns the most significant bytes of this modifier's UUID.
     *
     * @return the most significant bytes of this modifier's UUID.
     */
    public final Long getUUIDMost() {
        return nbt.get("UUIDMost", null);
    }

    /**
     * Sets the most significant bytes of this modifier's UUID.
     *
     * @param uuidMost the bytes.
     */
    public final void setUUIDMost(long uuidMost) {
        nbt.put("UUIDMost", uuidMost);
    }

    /**
     * Returns the least significant bytes of this modifier's UUID.
     *
     * @return the least significant bytes of this modifier's UUID.
     */
    public final Long getUUIDLeast() {
        return nbt.get("UUIDLeast", null);
    }

    /**
     * Sets the least significant bytes of this modifier's UUID.
     *
     * @param uuidLeast the bytes.
     */
    public final void setUUIDLeast(long uuidLeast) {
        nbt.put("UUIDLeast", uuidLeast);
    }

    /**
     * Returns this modifier's UUID.
     *
     * @return this modifier's UUID.
     */
    public final UUID getUUID() {
        Long uuidMost = getUUIDMost();
        Long uuidLeast = getUUIDLeast();

        if (uuidMost == null || uuidLeast == null) {
            return null;
        }

        return new UUID(uuidMost, uuidLeast);
    }

    /**
     * Sets this modifier's UUID.
     *
     * @param uuid the new modifier's UUID.
     */
    public final void setUUID(UUID uuid) {
        setUUIDMost(uuid.getMostSignificantBits());
        setUUIDLeast(uuid.getLeastSignificantBits());
    }

    /**
     * Returns the Minecraft NBT/JSON string representation of this attribute.
     * See {@link NBT#toNBTJSONString(Object) } for more information.
     *
     * @return the Minecraft NBT/JSON string representation of this attribute.
     */
    @Override
    public String toString() {
        return NBT.toNBTJSONString(nbt);
    }

    /**
     * Returns the underlying NBT compound of this attribute.
     *
     * @return the underlying NBT compound of this attribute.
     */
    public final NBTCompound getNBTCompound() {
        return nbt;
    }

    /**
     * Returns the custom data payload associated to this attribute.
     * This enabled plugins to put custom Attributes (and data) to any item.<br>
     * The data payload is actually stored as the name of the attribute's modifier, which is not used by the game.<br>
     * However, as the custom data shows an empty line (per slot in 1.9+ !) in
     * the item's tooltip in the client, you may want to hide item attributes.
     *
     * @return the custom data payload associated to this attribute.
     */
    public final String getCustomData() {
        return getName();
    }

    /**
     * Sets the custom data payload associated to this attribute.
     * See {@link #getCustomData() } for additional information.
     *
     * @param data the new data.
     */
    public final void setCustomData(String data) {
        setName(data);
    }

    /**
     * Returns this modifier's operation.
     *
     * @return this modifier's operation.
     */
    public final AttributeOperation getOperation() {
        return AttributeOperation.fromCode(nbt.get("Operation", 0));
    }

    /**
     * Sets this modifier's operation.
     *
     * @param operation The new operation value.
     */
    public final void setOperation(AttributeOperation operation) {
        nbt.put("Operation", operation.getCode());
    }

    /**
     * Returns the slot for which this modfier applies.
     *
     * @return the slot for which this modfier applies.
     */
    public final String getSlotName() {
        return nbt.get("Slot", null);
    }

    /**
     * Sets the slot for which this modfier applies.
     *
     * @param slotName The name of the slot.
     */
    public final void setSlotName(String slotName) {
        nbt.put("Slot", slotName);
    }

    /**
     * Returns the amount of the modification.
     *
     * @return the amount of the modification.
     */
    public final double getAmount() {
        return nbt.get("Amount", 0.0);
    }

    /**
     * Sets the amount of the modification.
     *
     * @param amount The amount fo the modification.
     */
    public final void setAmount(double amount) {
        nbt.put("Amount", amount);
    }
}
