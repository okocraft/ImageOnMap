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

package fr.zcraft.quartzlib.tools.players;


import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.NMSNetwork;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;


/**
 * Enables or disables reduced debug infos for a player.
 */
public final class ReducedDebugInfo {
    private static final byte ENABLE_REDUCED_DEBUG_INFO = 22;
    private static final byte DISABLE_REDUCED_DEBUG_INFO = 23;
    private static final Set<UUID> reducedPlayers = new HashSet<>();
    private static boolean enabled;
    private static Class<?> entityClass;
    private static Class<?> packetPlayOutEntityStatusClass;
    private static Listener listener = null;
    private static boolean listenerRegistered = false;

    static {
        try {
            entityClass =
                    Reflection.getMinecraftClassByName("world.entity.Entity");
            packetPlayOutEntityStatusClass =
                    Reflection.getMinecraftClassByName("network.protocol.game.PacketPlayOutEntityStatus");

            enabled = true;
        } catch (ClassNotFoundException e) {
            enabled = false;
        }
    }

    /**
     * Enables or disables reduced debug infos for the given player. This will
     * not be kept if the player re-logs.
     *
     * <p>Note that this will only be guaranteed effective on Notchian clients,
     * as modded ones can choose to ignore the packet.</p>
     *
     * @param player The player.
     * @param reduce {@code true} to reduce debugs infos; {@code false} to
     *               expand them.
     */
    public static void setForPlayer(final Player player, final boolean reduce) {
        setForPlayer(player, reduce, false);
    }

    /**
     * Enables or disables reduced debug info for the given player.
     *
     * <p>Note that this will only be guaranteed effective on Notchian clients,
     * as modded ones can choose to ignore the packet.</p>
     *
     * @param player      The player.
     * @param reduce      {@code true} to reduce debugs infos; {@code false} to
     *                    expand them.
     * @param keepReduced {@code true} to keep the option active when the player
     *                    re-logs. To disable a kept reduction, just call {@link
     *                    #setForPlayer(Player, boolean) setForPlayer(Player,
     *                    false)}. Only kept while the server is running, not
     *                    after a reboot.
     * @return {@code true} if the packet was successfully sent.
     */
    public static boolean setForPlayer(final Player player, final boolean reduce, final boolean keepReduced) {
        if (!enabled) {
            return false;
        }

        if (reduce && keepReduced) {
            reducedPlayers.add(player.getUniqueId());
        } else if (!reduce) {
            reducedPlayers.remove(player.getUniqueId());
        }

        checkListener();

        try {
            final Object handle = NMSNetwork.getPlayerHandle(player);

            final Constructor<?> packetConstructor =
                    packetPlayOutEntityStatusClass.getConstructor(entityClass, byte.class);
            final Object packet = packetConstructor
                    .newInstance(handle, reduce ? ENABLE_REDUCED_DEBUG_INFO : DISABLE_REDUCED_DEBUG_INFO);

            NMSNetwork.sendPacket(NMSNetwork.getPlayerConnection(handle), packet);

            return true;
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException
                | IllegalAccessException | IllegalArgumentException e) {
            PluginLogger.error("Cannot " + (reduce ? "enable" : "disable") + " reduced debug infos for player {0}", e,
                    player.getName());
            return false;
        }
    }

    /**
     * Enables or disables the internal listener to keep the reduced debug
     * enabled, if needed.
     */
    private static void checkListener() {
        if (reducedPlayers.size() > 0) {
            if (listener == null) {
                listener = new ReducedDebugListener();
            }
            if (!listenerRegistered) {
                QuartzLib.registerEvents(listener);
                listenerRegistered = true;
            }
        } else if (listenerRegistered) {
            QuartzLib.unregisterEvents(listener);
            listenerRegistered = false;
        }
    }

    protected static class ReducedDebugListener implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onPlayerJoin(PlayerJoinEvent ev) {
            if (reducedPlayers.contains(ev.getPlayer().getUniqueId())) {
                setForPlayer(ev.getPlayer(), true);
            }
        }
    }
}
