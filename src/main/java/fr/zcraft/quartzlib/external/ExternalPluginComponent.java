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

package fr.zcraft.quartzlib.external;

import fr.zcraft.quartzlib.core.QuartzComponent;
import fr.zcraft.quartzlib.tools.PluginLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * This class is a component to interface with other plugins.
 * When enabled, the plugin will be looked for using the provided name, and
 * casted to the given class.
 * If any of this fails, the component will not be enabled.
 * Note these components may even fail during instanciation (if the other
 * plugin's classes are not found).
 *
 * @param <T> The class of the plugin to interface with.
 */
public class ExternalPluginComponent<T extends Plugin> extends QuartzComponent {
    private final String pluginName;
    private T plugin;

    /**
     * Instantiates a new external plugin component with the given name.
     *
     * @param pluginName The name of the plugin to interface with.
     */
    protected ExternalPluginComponent(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    protected final void onEnable() {
        Plugin bukkitPlugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
        if (bukkitPlugin == null) {
            setEnabled(false);
            return;
        } else if (!bukkitPlugin.isEnabled()) {
            // We try to load the plugin (required if we're a startup plugin
            // depending on a non-startup one).
            Bukkit.getServer().getPluginManager().enablePlugin(bukkitPlugin);
            if (!bukkitPlugin.isEnabled()) {
                setEnabled(false);
                return;
            }
        }

        try {
            plugin = (T) bukkitPlugin;
        } catch (Exception e) {
            PluginLogger.error("Exception while loading plugin '{0}'. Is it up-to-date ?", e, pluginName);
            this.setEnabled(false);
            return;
        }

        onLoad();
    }

    /**
     * Returns the external plugin.
     * If this component failed to inizialize, an IllegalStateException will be
     * thrown.
     *
     * @return The external plugin.
     */
    protected T get() {
        if (!this.isEnabled()) {
            throw new IllegalStateException("External plugin " + pluginName + " could not be loaded.");
        }
        return plugin;
    }

    /**
     * This method is called after the external plugin has been found.
     * It replaces onEnable().
     */
    protected void onLoad() {
    }

    /**
     * Returns the name of the external plugin.
     *
     * @return The specified name of the external plugin.
     */
    public String getPluginName() {
        return pluginName;
    }
}
