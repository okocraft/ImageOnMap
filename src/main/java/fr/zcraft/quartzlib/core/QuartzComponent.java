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

package fr.zcraft.quartzlib.core;

import org.bukkit.plugin.Plugin;

/**
 * This abstract class represents a QuartzLib component that needs to be loaded
 * and/or unloaded.
 */
public abstract class QuartzComponent {
    private boolean enabled = false;

    /**
     * Called when this component is enabled, while the parent plugin is itself
     * enabled.
     */
    protected void onEnable() {
    }

    /**
     * Called when this component is disabled, while the parent plugin is itself
     * disabled.
     */
    protected void onDisable() {
    }

    /**
     * Checks if this component is enabled or not.
     *
     * @return {@code true} if enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables (load) or disable (unload) this QuartzLib component.
     *
     * @param enabled {@code true} to enable the component.
     */
    public void setEnabled(boolean enabled) {
        if (enabled == this.enabled) {
            return; // The state is not changed.
        }

        this.enabled = enabled;

        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }

    /**
     * Gets the plugin owning this component.
     * @return The plugin owning this component.
     */
    protected Plugin getPlugin() {
        return QuartzLib.getPlugin();
    }
}
