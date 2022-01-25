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

package fr.moribus.imageonmap.gui;

import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.util.HashMap;
import org.bukkit.entity.Player;

public final class Gui {
    /**
     * A map of all the currently open GUIs, associated to the HumanEntity
     * that requested it.
     */
    private static final HashMap<Player, GuiBase> openGuis = new HashMap<>();

    /**
     * Opens a GUI for a player.
     *
     * @param <T>    A GUI type.
     * @param owner  The player the GUI will be shown to.
     * @param gui    The GUI.
     * @param parent The parent of the newly created GUI. Can be null.
     * @return The opened GUI.
     */
    public static <T extends GuiBase> T open(final Player owner, final T gui, final GuiBase parent) {
        GuiBase openGui = openGuis.get(owner);
        if (openGui != null) {
            openGui.registerClose();
        }
        if (parent != null) {
            gui.setParent(parent);
        }

        RunTask.later(() -> gui.open(owner), 0);
        return gui;
    }

    /**
     * Opens a GUI for a player.
     *
     * @param <T>   A GUI type.
     * @param owner The player the GUI will be shown to.
     * @param gui   The GUI.
     * @return The opened GUI.
     */
    public static <T extends GuiBase> T open(Player owner, T gui) {
        return open(owner, gui, null);
    }

    /**
     * Closes any open GUI for a given player.
     *
     * @param owner The player.
     */
    public static void close(Player owner) {
        GuiBase openGui = openGuis.get(owner);
        if (openGui != null) {
            openGui.close();
        }
    }

    /**
     * Closes any GUI of this type (or subclass of it).
     *
     * @param guiClass The GUI class.
     */
    public static void close(Class<? extends GuiBase> guiClass) {
        for (GuiBase openGui : openGuis.values()) {
            if (guiClass.isAssignableFrom(openGui.getClass())) {
                openGui.close();
            }
        }
    }

    /**
     * Updates any GUI of this type (or subclass of it).
     *
     * @param guiClass The GUI class.
     */
    public static void update(Class<? extends GuiBase> guiClass) {
        for (GuiBase openGui : openGuis.values()) {
            if (guiClass.isAssignableFrom(openGui.getClass())) {
                openGui.update();
            }
        }
    }

    /**
     * Registers a GUI as open for the given player.
     */
    static void registerGuiOpen(Player player, GuiBase gui) {
        openGuis.put(player, gui);
    }

    /**
     * Registers a GUI as closed for the given player.
     */
    static void registerGuiClose(GuiBase gui) {
        openGuis.remove(gui.getPlayer());
    }

    /**
     * Clears opened GUIs. Invoked on plugin enabled and disabled.
     */
    public static void clearOpenGuis() {
        openGuis.clear();
    }
}
