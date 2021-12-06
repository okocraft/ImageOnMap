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

package fr.zcraft.quartzlib.tools.text;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.ChatColor;

public class ChatColoredString {
    private final Set<ChatColor> modifiers;
    private final String string;

    public ChatColoredString(Collection<ChatColor> modifiers, String string) {
        this.modifiers = Collections.unmodifiableSet(EnumSet.copyOf(modifiers));
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public Set<ChatColor> getModifiers() {
        return modifiers;
    }

    /**
     * Builds a string of the chat color using the given delimiter.
     */
    public String toString(char delimiter) {
        StringBuilder builder = new StringBuilder();

        for (ChatColor modifier : modifiers) {
            builder.append(delimiter);
            builder.append(modifier.getChar());
        }

        builder.append(string);
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString('§');
    }
}
