/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
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
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap;

import java.util.Locale;
import java.util.function.Supplier;

import org.bukkit.configuration.file.FileConfiguration;

import fr.moribus.imageonmap.i18n.I18n;

public final class PluginConfiguration {

    private static final ImageOnMap PLUGIN = ImageOnMap.getPlugin();

    public static final Supplier<Locale> LANG = () -> I18n.localeFromString(PLUGIN.getConfig().getString("lang"));

    public static final Supplier<Integer> MAP_GLOBAL_LIMIT = () -> {
        FileConfiguration config = PLUGIN.getConfig();
        if (config.isInt("map-global-limit")) {
            return config.getInt("map-global-limit");
        }
        if (config.isInt("Limit-map-by-server")) {
            return config.getInt("Limit-map-by-server");
        }
        return 0;
    };

    public static final Supplier<Integer> MAP_PLAYER_LIMIT = () -> {
        FileConfiguration config = PLUGIN.getConfig();
        if (config.isInt("map-player-limit")) {
            return config.getInt("map-player-limit");
        }
        if (config.isInt("Limit-map-by-player")) {
            return config.getInt("Limit-map-by-player");
        }
        return 0;
    };

    public static final Supplier<Boolean> SAVE_FULL_IMAGE = () -> PLUGIN.getConfig().getBoolean("save-full-image");

    public static final Supplier<Integer> LIMIT_SIZE_X = () -> PLUGIN.getConfig().getInt("limit-map-size-x");

    public static final Supplier<Integer> LIMIT_SIZE_Y = () -> PLUGIN.getConfig().getInt("limit-map-size-y");

}
