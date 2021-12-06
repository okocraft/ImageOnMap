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

import java.util.EnumSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public class ChatColorParser implements Iterator<ChatColoredString>, Iterable<ChatColoredString> {
    private final char delimiter;
    private final String string;
    private final Set<ChatColor> currentModifiers = EnumSet.noneOf(ChatColor.class);
    private int previousPos = -2;
    private int currentPos = -1;
    private boolean done = false;
    private ChatColoredString nextString;

    public ChatColorParser(String string) {
        this('§', string);
    }

    /**
     * Creates a new chat color parser.
     */
    public ChatColorParser(char delimiter, String string) {
        this.string = string;
        this.delimiter = delimiter;

        nextString = fetchNextPos();
    }

    /**
     * Apply a given modifier to the current mutable set of modifiers.
     */
    public static void applyModifier(Set<ChatColor> currentModifiers, ChatColor newModifier) {
        if (newModifier == ChatColor.RESET) {
            currentModifiers.clear();
            return;
        } else if (newModifier.isColor()) {
            for (ChatColor color : ChatColor.values()) {
                if (!color.isColor()) {
                    continue;
                }
                currentModifiers.remove(color);
            }
        }

        currentModifiers.add(newModifier);
    }

    @Override
    public boolean hasNext() {
        return nextString != null;
    }

    @Override
    public ChatColoredString next() {
        if (nextString == null) {
            throw new NoSuchElementException();
        }

        ChatColoredString currentString = nextString;
        nextString = fetchNextPos();
        return currentString;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove");
    }

    @Override
    public @NotNull Iterator<ChatColoredString> iterator() {
        return this;
    }

    private ChatColor getCurrentColor() {
        ChatColor color = ChatColor.getByChar(string.charAt(currentPos + 1));
        if (color == null) {
            throw new IllegalArgumentException("Invalid token : invalid color code :" + string.charAt(currentPos + 1));
        }

        return color;
    }

    private ChatColoredString fetchNextPos() {
        if (done) {
            return null;
        }

        while (true) {
            currentPos = string.indexOf(delimiter, currentPos + 1);
            if (currentPos == -1) {
                done = true;
                break;
            }

            if (currentPos >= string.length() - 1) {
                throw new IllegalArgumentException("Invalid token : found delimiter without color code.");
            }

            //Color code mode
            if (currentPos == previousPos + 2) {
                applyModifier(currentModifiers, getCurrentColor());
            } else {
                ChatColoredString str =
                        new ChatColoredString(currentModifiers, string.substring(previousPos + 2, currentPos));

                applyModifier(currentModifiers, getCurrentColor());
                previousPos = currentPos;
                return str;
            }

            previousPos = currentPos;
        }

        return new ChatColoredString(currentModifiers, string.substring(previousPos + 2));
    }
}
