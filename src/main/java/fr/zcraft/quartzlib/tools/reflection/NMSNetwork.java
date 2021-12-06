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

package fr.zcraft.quartzlib.tools.reflection;

import fr.zcraft.quartzlib.exceptions.IncompatibleMinecraftVersionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.bukkit.entity.Player;


/**
 * An utility used to manipulate packets and players connections with ease.
 *
 * @author Amaury Carrade
 */
public final class NMSNetwork {
    private static final Class<?> craftPlayerClass;
    private static final Class<?> entityPlayerClass;
    private static final Class<?> packetClass;
    private static final Method sendPacketMethod;

    static {
        try {
            craftPlayerClass = Reflection.getBukkitClassByName("entity.CraftPlayer");
            entityPlayerClass = Reflection.getMinecraftClassByName("server.level.EntityPlayer");

            packetClass = Reflection.getMinecraftClassByName("network.protocol.Packet");
            sendPacketMethod = ((Class<?>) Reflection.getMinecraftClassByName("server.network.PlayerConnection"))
                    .getDeclaredMethod("sendPacket", packetClass);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new IncompatibleMinecraftVersionException("Cannot load classes needed to send network packets", e);
        }
    }

    private NMSNetwork() {
    }

    /**
     * Returns the player's handle (i.e. the NMS EntityPlayer object).
     *
     * @param player The player.
     * @return The player's handle (reflection-retrieved object, instance of the
     *     net.minecraft.server.EntityPlayer class).
     * @throws InvocationTargetException             if an exception is thrown while the connection
     *                                               is retrieved.
     * @throws IncompatibleMinecraftVersionException if an error occurs while loading the classes,
     *                                               methods and fields needed to get the player's
     *                                               connection.
     */
    public static Object getPlayerHandle(Player player) throws InvocationTargetException {
        try {
            Object craftPlayer = craftPlayerClass.cast(player);
            return Reflection.call(craftPlayer, "getHandle");
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IncompatibleMinecraftVersionException(
                    "Cannot retrieve standard Bukkit or NBS object while getting a player's handle, "
                            + "is the current Bukkit/Minecraft version supported by this API?", e);
        }
    }

    /**
     * Returns the player's connection, frequently used to send packets.
     *
     * @param playerHandle The player's handle, as returned by {@link #getPlayerHandle(Player)}.
     * @return The player's connection (reflection-retrieved object, instance of the
     *     net.minecraft.server.PlayerConnection class).
     */
    public static Object getPlayerConnection(Object playerHandle) {
        try {
            if (!entityPlayerClass.isAssignableFrom(playerHandle.getClass())) {
                throw new ClassCastException("Cannot retrieve a player connection from another class that "
                    + "net.minecraft.server.<version>.EntityPlayer (got " + playerHandle.getClass().getName() + ").");
            }

            return Reflection.getFieldValue(playerHandle, "playerConnection");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IncompatibleMinecraftVersionException(
                    "Cannot retrieve standard Bukkit or NBS object while getting a player's connection, "
                            + "is the current Bukkit/Minecraft version supported by this API?", e);
        }
    }

    /**
     * Returns the player's connection, frequently used to send packets.
     *
     * @param player The player.
     * @return The player's connection (reflection-retrieved object, instance of the
     *     net.minecraft.server.PlayerConnection class).
     * @throws InvocationTargetException             if an exception is thrown while the connection
     *                                               is retrieved.
     * @throws IncompatibleMinecraftVersionException if an error occurs while loading the classes,
     *                                               methods and fields needed to get the player's
     *                                               connection.
     */
    public static Object getPlayerConnection(Player player) throws InvocationTargetException {
        return getPlayerConnection(getPlayerHandle(player));
    }

    /**
     * Sends a packet.
     *
     * @param playerConnection A player connection, as returned by the {@link #getPlayerConnection(Player)}
     *                         method.
     * @param packet           The packet to be sent, an instance of a subclass of the
     *                         net.minecraft.server.Packet class.
     * @throws InvocationTargetException             if an exception is thrown while the packet is
     *                                               sent.
     * @throws ClassCastException                    if the {@code packet} object is not an instance
     *                                               of a subclass of the net.minecraft.server.Packet
     *                                               class.
     * @throws IncompatibleMinecraftVersionException if an error occurs while loading the classes,
     *                                               methods and fields needed to send the packet.
     */
    public static void sendPacket(Object playerConnection, Object packet) throws InvocationTargetException {
        try {
            if (!packetClass.isAssignableFrom(packet.getClass())) {
                throw new ClassCastException(
                        "Cannot send a packet object if the object is not a subclass of "
                                + "net.minecraft.server.<version>.Packet (got "
                                + packet.getClass().getName() + ").");
            }

            sendPacketMethod.invoke(playerConnection, packet);
        } catch (IllegalAccessException e) {
            throw new IncompatibleMinecraftVersionException(
                    "Cannot retrieve standard Bukkit or NBS object while sending a packet to a player, "
                            + "is the current Bukkit/Minecraft version supported by this API?", e);
        }
    }

    /**
     * Sends a packet.
     * <p>If you use this method, the player connection is not cached. If you have multiple packets to
     * send, store the player's connection returned by {@link #getPlayerConnection(Player)} and then
     * use the {@link #sendPacket(Object, Object)} method.</p>
     *
     * @param player The player this packet will be sent to.
     * @param packet The packet to be sent, an instance of a subclass of the
     *               net.minecraft.server.Packet class.
     * @throws InvocationTargetException             if an exception is thrown while the packet is
     *                                               sent.
     * @throws ClassCastException                    if the {@code packet} object is not an instance
     *                                               of a subclass of the net.minecraft.server.Packet
     *                                               class.
     * @throws IncompatibleMinecraftVersionException if an error occurs while loading the classes,
     *                                               methods and fields needed to send the packet.
     */
    public static void sendPacket(Player player, Object packet) throws InvocationTargetException {
        sendPacket(getPlayerConnection(player), packet);
    }
}
