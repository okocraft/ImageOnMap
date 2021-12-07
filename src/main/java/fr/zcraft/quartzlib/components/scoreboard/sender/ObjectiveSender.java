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

package fr.zcraft.quartzlib.components.scoreboard.sender;


import fr.zcraft.quartzlib.components.scoreboard.Sidebar;
import fr.zcraft.quartzlib.exceptions.IncompatibleMinecraftVersionException;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.NMSNetwork;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;


/**
 * This class uses packets to send the {@link SidebarObjective SidebarObjectives} to their
 * receivers.
 *
 *
 * <h3>WARNING</h3>
 * <p>This class is intended for use with the sidebar objectives only. Notably, it will force the
 * display position and unregister the objectives previously sent through it. This API may evolve to
 * a generic way to send scoreboard packets, but it's not currently the case.</p>
 *
 * @author Amaury Carrade
 */
public class ObjectiveSender {
    private static final Map<UUID, Object> playersConnections = new ConcurrentHashMap<>();
    private static final Map<UUID, String> sentObjectives = new HashMap<>();


    // The action field of the scoreboard objective packet.
    private static final int PACKET_SCOREBOARD_OBJECTIVE_ACTION_CREATE = 0;
    private static final int PACKET_SCOREBOARD_OBJECTIVE_ACTION_DELETE = 1;
    private static final int PACKET_SCOREBOARD_OBJECTIVE_ACTION_UPDATE = 2;

    // The location field of the objective display packet.
    // For the curious ones:  0 = list ; 1 = sidebar ; 2 = below name.
    private static final int PACKET_DISPLAY_OBJECTIVE_SIDEBAR_LOCATION = 1;


    // The NMS classes & enum values needed to send the packets.
    private static Class<?> packetPlayOutScoreboardObjectiveClass;
    private static Class<?> packetPlayOutScoreboardDisplayObjectiveClass;
    private static Class<?> packetPlayOutScoreboardScoreClass;
    private static Class<?> chatComponentText;

    private static Object enumScoreboardHealthDisplay_INTEGER = null;
    private static Object enumScoreboardAction_CHANGE = null;
    private static Object enumScoreboardAction_REMOVE = null;


    static {
        try {
            chatComponentText = Reflection.getMinecraftClassByName("network.chat.ChatComponentText");
        } catch (ClassNotFoundException e) {
            chatComponentText = null;
        }

        try {
            packetPlayOutScoreboardObjectiveClass =
                    Reflection.getMinecraftClassByName("network.protocol.game.PacketPlayOutScoreboardObjective");
            packetPlayOutScoreboardDisplayObjectiveClass =
                    Reflection.getMinecraftClassByName("network.protocol.game.PacketPlayOutScoreboardDisplayObjective");
            packetPlayOutScoreboardScoreClass =
                    Reflection.getMinecraftClassByName("network.protocol.game.PacketPlayOutScoreboardScore");


            // IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER value

            Class<?> enumScoreboardHealthDisplay;
            try {
                enumScoreboardHealthDisplay =
                        Reflection.getMinecraftClassByName(
                                "world.scores.criteria.IScoreboardCriteria$EnumScoreboardHealthDisplay");
            } catch (ClassNotFoundException e) {
                enumScoreboardHealthDisplay = Reflection.getMinecraftClassByName("EnumScoreboardHealthDisplay");
            }

            for (Object enumConstant : enumScoreboardHealthDisplay.getEnumConstants()) {
                if (((Enum<?>) enumConstant).name().equals("INTEGER")) {
                    enumScoreboardHealthDisplay_INTEGER = enumConstant;
                    break;
                }
            }

            if (enumScoreboardHealthDisplay_INTEGER == null) {
                throw new ClassNotFoundException("Unable to retrieve the INTEGER value of the"
                        + "IScoreboardCriteria$EnumScoreboardHealthDisplay enum");
            }


            // PacketPlayOutScoreboardScore.EnumScoreboardAction values

            Class<?> enumScoreboardAction;
            try {
                enumScoreboardAction =
                        Reflection.getMinecraftClassByName(
                                "network.protocol.game.PacketPlayOutScoreboardScore$EnumScoreboardAction");
            } catch (ClassNotFoundException e) {
                try {
                    enumScoreboardAction = Reflection.getMinecraftClassByName("EnumScoreboardAction");
                } catch (ClassNotFoundException ex) {
                    enumScoreboardAction = Reflection.getMinecraftClassByName("server.ScoreboardServer$Action");
                }
            }

            for (Object enumConstant : enumScoreboardAction.getEnumConstants()) {
                switch (((Enum<?>) enumConstant).name()) {
                    case "CHANGE" -> enumScoreboardAction_CHANGE = enumConstant;
                    case "REMOVE" -> enumScoreboardAction_REMOVE = enumConstant;
                }
            }

            if (enumScoreboardAction_CHANGE == null) {
                throw new ClassNotFoundException("Unable to retrieve the CHANGE value of the"
                        + "PacketPlayOutScoreboardScore$EnumScoreboardAction enum");
            }
            if (enumScoreboardAction_REMOVE == null) {
                throw new ClassNotFoundException("Unable to retrieve the REMOVE value of the"
                        + "PacketPlayOutScoreboardScore$EnumScoreboardAction enum");
            }
        } catch (ClassNotFoundException e) {
            ImageOnMap.getPlugin().getLogger().warning("Unable to get the required classes to send scoreboard packets", e);
        }
    }



    /* **  Public API  ** */

    /**
     * Sends the given objective to its receiver(s).
     *
     * @param objective The objective to be displayed.
     */
    public static void send(SidebarObjective objective) {
        Validate.notNull(objective, "The objective cannot be null");

        for (UUID receiver : objective.getReceivers()) {
            try {
                send(receiver, objective);
            } catch (RuntimeException ignored) {
            } // Caught, so the packets are not sent for this player only.
        }
    }

    /**
     * Sends the given objective to the receiver.
     * <p>This method will:</p>
     * <ul>
     *     <li>send the “create objective” packet if needed;</li>
     *     <li>send the “display scoreboard” packet, to show the sidebar at the right place;</li>
     *     <li>send the “update score” packet for each score in the objective;</li>
     *     <li>if previously present, send a “remove objective” packet to remove the previously-sent objective.</li>
     * </ul>
     *
     * @param receiver  The receiver of this objective.
     * @param objective The objective to display.
     */
    private static void send(UUID receiver, SidebarObjective objective) {
        final String oldObjective = sentObjectives.get(receiver);
        final Object connection = getPlayerConnection(receiver);

        try {
            createObjective(connection, objective);
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendScores(connection, objective);

        // The objective is displayed when the scores are sent, so all the lines are displayed
        // instantaneously, even with bad connections.
        setObjectiveDisplay(connection, objective);


        sentObjectives.put(receiver, objective.getName());

        if (oldObjective != null) {
            destroyObjective(connection, oldObjective);
        }
    }

    /**
     * Updates the display name of the given objective.
     * <p>For each receiver, this will send a display name update packet if the objective is already
     * displayed, or send the whole sidebar, if not.</p>
     *
     * @param objective The objective to update.
     */
    public static void updateDisplayName(SidebarObjective objective) {
        Validate.notNull(objective, "The objective cannot be null");

        for (UUID receiver : objective.getReceivers()) {
            try {
                String currentPlayerObjective = sentObjectives.get(receiver);
                if (objective.getName().equals(currentPlayerObjective)) {
                    updateObjectiveDisplayName(getPlayerConnection(receiver), objective);
                } else {
                    send(receiver, objective);
                }
            } catch (RuntimeException ignored) {
            } // Caught, so the packets are not sent for this player only.
        }
    }

    /**
     * Sends, for each receiver of this objective, the packets needed to replace the old line
     * (with the given score) with the new line (with the same score).
     *
     * @param objective The objective the old line (and the new) belongs to.
     * @param oldLine   The replaced line.
     * @param newLine   The new line.
     */
    public static void updateLine(SidebarObjective objective, String oldLine, String newLine, int score) {
        for (UUID receiver : objective.getReceivers()) {
            try {
                String currentPlayerObjective = sentObjectives.get(receiver);

                if (objective.getName().equals(currentPlayerObjective)) {
                    Object connection = getPlayerConnection(receiver);

                    sendScore(connection, objective, newLine, score);
                    deleteScore(connection, objective, oldLine);
                } else {
                    send(receiver, objective);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Tells the given player to destroy the previously sent objective, if it exists.
     *
     * @param player The UUID of the player to clear.
     */
    public static void clear(UUID player) {
        String sentObjectiveName = sentObjectives.get(player);

        if (sentObjectiveName != null) {
            destroyObjective(getPlayerConnection(player), sentObjectiveName);
        }
    }



    /* **  Objective senders private API  ** */

    /**
     * Sends the packet to create the given objective.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective to create.
     */
    private static void createObjective(Object connection, SidebarObjective objective) {
        sendScoreboardObjectivePacket(connection, objective.getName(), objective.getDisplayName(),
                PACKET_SCOREBOARD_OBJECTIVE_ACTION_CREATE);
    }

    /**
     * Sends the packet to update the display name of the given objective.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective to update.
     */
    private static void updateObjectiveDisplayName(Object connection, SidebarObjective objective) {
        sendScoreboardObjectivePacket(connection, objective.getName(), objective.getDisplayName(),
                PACKET_SCOREBOARD_OBJECTIVE_ACTION_UPDATE);
    }

    /**
     * Sends the packet to set the display slot of the given objective.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective to place.
     */
    private static void setObjectiveDisplay(Object connection, SidebarObjective objective) {
        sendScoreboardDisplayObjectivePacket(connection, objective.getName(),
                PACKET_DISPLAY_OBJECTIVE_SIDEBAR_LOCATION);
    }

    /**
     * Sends the score packets of the given objective.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective containing the score packets to send.
     */
    private static void sendScores(Object connection, SidebarObjective objective) {
        for (Map.Entry<String, Integer> score : objective.getScores().entrySet()) {
            sendScore(connection, objective, score.getKey(), score.getValue());
        }
    }

    /**
     * Sends a create score packet.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective the score belongs to.
     * @param score      The score's name to send. If this is longer than 40 characters, the client
     *                   will crash.
     * @param value      The score's value to send.
     */
    private static void sendScore(Object connection, SidebarObjective objective, String score, Integer value) {
        sendScoreboardScorePacket(connection, objective.getName(), score, value, enumScoreboardAction_CHANGE);
    }

    /**
     * Sends a remove score packet.
     *
     * @param connection The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objective  The objective the score belongs to.
     * @param score      The score to delete.
     */
    private static void deleteScore(Object connection, SidebarObjective objective, String score) {
        sendScoreboardScorePacket(connection, objective.getName(), score, 0, enumScoreboardAction_REMOVE);
    }

    /**
     * Sends the packet to destroy an objective.
     *
     * @param connection    The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objectiveName The name of the objective to destroy.
     */
    private static void destroyObjective(Object connection, String objectiveName) {
        sendScoreboardObjectivePacket(connection, objectiveName, "", PACKET_SCOREBOARD_OBJECTIVE_ACTION_DELETE);
    }



    /* **  Low level packet senders  ** */

    /**
     * Sends a Scoreboard Objective packet.
     *
     * @param connection           The receiver's connection: an instance of {@code
     *                             nms.PlayerConnection}.
     * @param objectiveName        The name of the objective.
     * @param objectiveDisplayName The display name of the objective.
     * @param action               The action to execute: 0 (to create), 1 (to delete) or 2 (to
     *                             update).
     * @throws IncompatibleMinecraftVersionException if the packet cannot be constructed or sent.
     * @throws RuntimeException                      if something bad happens.
     */
    private static void sendScoreboardObjectivePacket(Object connection, String objectiveName,
                                                      String objectiveDisplayName, int action) {
        try {
            Object packet = Reflection.instantiate(packetPlayOutScoreboardObjectiveClass);

            Reflection.setFieldValue(packet, "a", objectiveName);                       // Objective name
            Reflection.setFieldValue(packet, "c",
                    enumScoreboardHealthDisplay_INTEGER); // Display mode (integer or hearts)
            Reflection.setFieldValue(packet, "d",
                    action);                              // Action (0 = create; 1 = delete; 2 = update)

            // Display name. The try part is for 1.13+, using chat components for the title; the catch for 1.12-.
            try {
                Reflection.setFieldValue(packet, "b", Reflection.instantiate(chatComponentText, objectiveDisplayName));
            } catch (IllegalArgumentException e) {
                Reflection.setFieldValue(packet, "b", objectiveDisplayName);
            }

            NMSNetwork.sendPacket(connection, packet);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | NoSuchFieldException e) {
            throw new IncompatibleMinecraftVersionException("Cannot send PacketPlayOutScoreboardObjective", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("An exception was caught while sending a PacketPlayOutScoreboardObjective",
                    e.getCause());
        }
    }

    /**
     * Sends a Scoreboard Display Objective packet.
     *
     * @param connection    The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objectiveName The objective's name.
     * @param location      Where the objective will be displayed: 0 (list), 1 (sidebar) or 2 (below
     *                      name).
     * @throws IncompatibleMinecraftVersionException if the packet cannot be constructed or sent.
     * @throws RuntimeException                      if something bad happens.
     */
    private static void sendScoreboardDisplayObjectivePacket(Object connection, String objectiveName, int location) {
        try {
            Object packet = Reflection.instantiate(packetPlayOutScoreboardDisplayObjectiveClass);

            Reflection.setFieldValue(packet, "a",
                    location);      // Objective location (0 = list ; 1 = sidebar ; 2 = below name)
            Reflection.setFieldValue(packet, "b", objectiveName); // Objective name

            NMSNetwork.sendPacket(connection, packet);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            throw new IncompatibleMinecraftVersionException("Cannot send PacketPlayOutScoreboardDisplayObjective", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(
                    "An exception was caught while sending a PacketPlayOutScoreboardDisplayObjective", e.getCause());
        }
    }

    /**
     * Sends a Scoreboard Score packet.
     *
     * @param connection    The receiver's connection: an instance of {@code nms.PlayerConnection}.
     * @param objectiveName The objective's name.
     * @param scoreName     The score's name.
     * @param scoreValue    The score's value.
     * @param action        The action to execute: an enum value of
     *                      {@code PacketPlayOutScoreboardScore$EnumScoreboardAction}
     *                      (CHANGE or REMOVE — use {@link #enumScoreboardAction_CHANGE} or {@link
     *                      #enumScoreboardAction_REMOVE}).
     * @throws IncompatibleMinecraftVersionException if the packet cannot be constructed or sent.
     * @throws RuntimeException                      if something bad happens.
     */
    private static void sendScoreboardScorePacket(Object connection, String objectiveName, String scoreName,
                                                  int scoreValue, Object action) {
        try {
            Object packet = Reflection.instantiate(packetPlayOutScoreboardScoreClass);

            Reflection.setFieldValue(packet, "a", scoreName);     // Score name
            Reflection.setFieldValue(packet, "b", objectiveName); // Objective name this score belongs to
            Reflection.setFieldValue(packet, "c", scoreValue);    // Score value
            Reflection.setFieldValue(packet, "d", action);        // Action (enum member - CHANGE or REMOVE)

            NMSNetwork.sendPacket(connection, packet);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | NoSuchFieldException e) {
            throw new IncompatibleMinecraftVersionException("Cannot send PacketPlayOutScoreboardScore", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("An exception was caught while sending a PacketPlayOutScoreboardScore",
                    e.getCause());
        }
    }



    /* **  Players connections caching  ** */

    /**
     * Retrieves and returns a player's network connection. This method caches the connection.
     *
     * @param id The player's UUID.
     * @return The connection, or {@code null} if the player is not logged in.
     * @throws RuntimeException if the connection cannot be retrieved for some reason.
     */
    private static Object getPlayerConnection(UUID id) {
        if (playersConnections.containsKey(id)) {
            return playersConnections.get(id);
        }

        try {
            final Player player = Sidebar.getPlayerAsync(id);

            if (player == null) {
                return null;
            }

            Object connection = NMSNetwork.getPlayerConnection(player);
            if (connection != null) {
                playersConnections.put(id, connection);
                return connection;
            } else {
                throw new RuntimeException("Unable to retrieve a player's connection (UUID: " + id + ")");
            }
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to retrieve a player's connection (UUID: " + id + ")", e);
        }
    }

    /**
     * Invalidates the stored connection of a player, and removes the last objective sent, because
     * if the used logged out and now log back in, the client removed by itself the previously-sent
     * objective.
     *
     * @param id The UUID of the invalidated connection.
     */
    public static void handleLogin(UUID id) {
        synchronized (playersConnections) {
            playersConnections.remove(id);
        }

        synchronized (sentObjectives) {
            sentObjectives.remove(id);
        }
    }
}
