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

/**
 * This enum represents the possible values for an item attribute operation.
 */
public enum AttributeOperation {
    /**
     * Operation 0: Additive. Adds all of the modifiers' amounts to the current value of the attribute.
     */
    ADDITIVE(0),
    /**
     * Operation 1: Multiplicative. Multiplies the current value of the attribute by (1 + x),
     * where x is the sum of the modifiers' amounts.
     */
    MULTIPLY(1),
    /**
     * Operation 2: Multiplicative. For every modifier, multiplies the current value of the attribute by (1 + x),
     * where x is the amount of the particular modifier.
     * Functions the same as Operation 1 if there is only a single modifier with operation 1 or 2.
     * However, for multiple modifiers it will multiply the modifiers rather than adding them.
     */
    MULTIPLY_ALL(2);

    private final int code;

    AttributeOperation(int code) {
        this.code = code;
    }

    /**
     * Gets an AttributeOperation from its code.
     *
     * @param code The attribute code.
     * @return the corresponding attribute operation.
     * @throws IllegalArgumentException if the code is invalid.
     */
    public static AttributeOperation fromCode(int code) throws IllegalArgumentException {
        for (AttributeOperation operation : values()) {
            if (operation.code == code) {
                return operation;
            }
        }

        throw new IllegalArgumentException("Illegal Attribute operation code : " + code);
    }

    /**
     * Gets the operation's code.
     * @return the operation's code.
     */
    public int getCode() {
        return code;
    }

    @Override
    public String toString() {
        return String.valueOf(code);
    }
}
