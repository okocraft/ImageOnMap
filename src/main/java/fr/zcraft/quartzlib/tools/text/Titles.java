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

import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.exceptions.IncompatibleMinecraftVersionException;
import fr.zcraft.quartzlib.tools.reflection.NMSNetwork;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import java.lang.reflect.InvocationTargetException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


/**
 * This utility class sends titles to the players.
 *
 * @author Amaury Carrade
 */
public final class Titles {
    private static final Class<?> packetPlayOutTitleClass;
    private static final Class<?> iChatBaseComponentClass;
    private static final boolean enabled = true;
    private static Class<?> chatSerializerClass;
    private static Class<?> enumTitleActionClass;

    private static Object enumTitleActionTitle;
    private static Object enumTitleActionSubtitle;

    static {
        try {
            packetPlayOutTitleClass = Reflection.getMinecraftClassByName("PacketPlayOutTitle");
            iChatBaseComponentClass = Reflection.getMinecraftClassByName("network.chat.IChatBaseComponent");

            try {
                chatSerializerClass = Reflection.getMinecraftClassByName("ChatSerializer");
            } catch (ClassNotFoundException e) {
                chatSerializerClass =
                        Reflection.getMinecraftClassByName("network.chat.IChatBaseComponent$ChatSerializer");
            }

            try {
                enumTitleActionClass = Reflection.getMinecraftClassByName("PacketPlayOutTitle$EnumTitleAction");
            } catch (ClassNotFoundException e) {
                enumTitleActionClass = Reflection.getMinecraftClassByName("EnumTitleAction");
            }

            for (Object enumConstant : enumTitleActionClass.getEnumConstants()) {
                switch (((Enum) enumConstant).name()) {
                    case "TITLE" -> enumTitleActionTitle = enumConstant;
                    case "SUBTITLE" -> enumTitleActionSubtitle = enumConstant;
                    default -> {
                    }
                }
            }
        } catch (Exception e) {
            enabled = false;
            throw new IncompatibleMinecraftVersionException("Unable to load classes needed to display titles.", e);
        }
    }


    private Titles() {
    }


    /**
     * Displays a title to the given player.
     *
     * @param player   The receiver of the title.
     * @param fadeIn   The fade-in time, in ticks.
     * @param stay     The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                 ticks).
     * @param fadeOut  The fade-out time, in ticks.
     * @param title    The text of the title. {@code null} if you don't want to display a title.
     * @param subtitle The text of the subtitle. {@code null} if you don't want to display a
     *                 subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void displayTitle(Player player, int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        displayRawTitle(
                player, fadeIn, stay, fadeOut,
                "{\"text\": \"" + (title != null ? title.replace("\"", "\\\"") : "") + "\"}",
                "{\"text\": \"" + (subtitle != null ? subtitle.replace("\"", "\\\"") : "") + "\"}"
        );
    }

    /**
     * Displays a title to the given player.
     *
     * @param player   The receiver of the title.
     * @param fadeIn   The fade-in time, in ticks.
     * @param stay     The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                 ticks).
     * @param fadeOut  The fade-out time, in ticks.
     * @param title    The text of the title. {@code null} if you don't want to display a title.
     * @param subtitle The text of the subtitle. {@code null} if you don't want to display a
     *                 subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void displayTitle(Player player, int fadeIn, int stay, int fadeOut, RawText title, RawText subtitle) {
        displayRawTitle(
                player, fadeIn, stay, fadeOut,
                title != null ? title.toJSONString() : "{\"text\": \"\"}",
                subtitle != null ? subtitle.toJSONString() : "{\"text\": \"\"}"
        );
    }

    /**
     * The core method to send a title to a player.
     *
     * @param connection  The player's connection (instance of {@code net.minecraft.server.PlayerConnection})
     * @param fadeIn      The fade-in time, in ticks.
     * @param stay        The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                    ticks).
     * @param fadeOut     The fade-out time, in ticks.
     * @param rawTitle    The JSON representation of the title. {@code null} if you don't want to
     *                    display a title.
     * @param rawSubtitle The JSON representation of the subtitle. {@code null} if you don't want to
     *                    display a subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    private static void displayTitle(Object connection, int fadeIn, int stay, int fadeOut, String rawTitle,
                                     String rawSubtitle) {
        if (!enabled) {
            return;
        }

        sendTimes(connection, fadeIn, stay, fadeOut);

        // Subtitles needs a title to be displayed.
        if ((rawTitle == null || rawTitle.isEmpty()) && rawSubtitle != null && !rawSubtitle.isEmpty()) {
            rawTitle = "{\"text\":\" \"}";
        }

        if (rawTitle != null && !rawTitle.isEmpty()) {
            sendTitleAction(connection, enumTitleActionTitle, rawTitle);
        }

        if (rawSubtitle != null && !rawSubtitle.isEmpty()) {
            sendTitleAction(connection, enumTitleActionSubtitle, rawSubtitle);
        }
    }

    /**
     * Displays a title to the given player.
     *
     * @param player      The receiver of the title.
     * @param fadeIn      The fade-in time, in ticks.
     * @param stay        The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                    ticks).
     * @param fadeOut     The fade-out time, in ticks.
     * @param rawTitle    The JSON representation of the title. {@code null} if you don't want to
     *                    display a title.
     * @param rawSubtitle The JSON representation of the subtitle. {@code null} if you don't want to
     *                    display a subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void displayRawTitle(Player player, int fadeIn, int stay, int fadeOut, String rawTitle,
                                       String rawSubtitle) {
        try {
            displayTitle(NMSNetwork.getPlayerConnection(player), fadeIn, stay, fadeOut, rawTitle, rawSubtitle);
        } catch (InvocationTargetException e) {
            throw new IncompatibleMinecraftVersionException(e);
        }
    }

    /**
     * Displays a title to the whole server.
     *
     * @param fadeIn   The fade-in time, in ticks.
     * @param stay     The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                 ticks).
     * @param fadeOut  The fade-out time, in ticks.
     * @param title    The text of the title. {@code null} if you don't want to display a title.
     * @param subtitle The text of the subtitle. {@code null} if you don't want to display a
     *                 subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void broadcastTitle(int fadeIn, int stay, int fadeOut, String title, String subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            displayTitle(player, fadeIn, stay, fadeOut, title, subtitle);
        }
    }

    /**
     * Displays a title to the whole server.
     *
     * @param fadeIn   The fade-in time, in ticks.
     * @param stay     The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                 ticks).
     * @param fadeOut  The fade-out time, in ticks.
     * @param title    The text of the title. {@code null} if you don't want to display a title.
     * @param subtitle The text of the subtitle. {@code null} if you don't want to display a
     *                 subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void broadcastTitle(int fadeIn, int stay, int fadeOut, RawText title, RawText subtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            displayTitle(player, fadeIn, stay, fadeOut, title, subtitle);
        }
    }

    /**
     * Displays a title to the whole server.
     *
     * @param fadeIn      The fade-in time, in ticks.
     * @param stay        The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                    ticks).
     * @param fadeOut     The fade-out time, in ticks.
     * @param rawTitle    The JSON representation of the title. {@code null} if you don't want to
     *                    display a title.
     * @param rawSubtitle The JSON representation of the subtitle. {@code null} if you don't want to
     *                    display a subtitle.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    public static void broadcastRawTitle(int fadeIn, int stay, int fadeOut, String rawTitle, String rawSubtitle) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            displayRawTitle(player, fadeIn, stay, fadeOut, rawTitle, rawSubtitle);
        }
    }



    /* *** Private API *** */

    /**
     * Sends the Titles TIMES packet, used to send the fade-in, stay and fade-out times to the
     * client.
     *
     * @param connection The player's connection (instance of {@code net.minecraft.server.PlayerConnection})
     * @param fadeIn     The fade-in time, in ticks.
     * @param stay       The time the title stays in the screen, fade-in &amp; out times excluded (in
     *                   ticks).
     * @param fadeOut    The fade-out time, in ticks.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the times.
     */
    private static void sendTimes(Object connection, int fadeIn, int stay, int fadeOut) {
        try {
            if (fadeIn >= 0 || stay >= 0 || fadeOut >= 0) {
                NMSNetwork.sendPacket(
                        connection,
                        packetPlayOutTitleClass.getConstructor(int.class, int.class, int.class)
                                .newInstance(fadeIn, stay, fadeOut)
                );
            }
        } catch (NoSuchMethodException | InstantiationException
                | IllegalAccessException | InvocationTargetException e) {
            throw new IncompatibleMinecraftVersionException("Error while sending a TIMES title packet",
                    e instanceof InvocationTargetException ? e.getCause() : e);
        }
    }

    /**
     * Sends a full title packet to the player.
     *
     * @param connection The player's connection (instance of {@code net.minecraft.server.PlayerConnection})
     * @param action     The action, an item of the {@code net.minecraft.server.PacketPlayOutTitle$EnumTitleAction}
     *                   enumeration (only TITLE and SUBTITLE are implemented).
     * @param payload    The content to be sent; MUST be a valid JSON payload.
     * @throws IncompatibleMinecraftVersionException if an error is encountered while sending the title.
     */
    private static void sendTitleAction(Object connection, Object action, String payload) {
        try {
            Object baseComponent = iChatBaseComponentClass
                    .cast(Reflection.call(chatSerializerClass, chatSerializerClass, "a", payload));
            Object titlePacket = packetPlayOutTitleClass.getConstructor(enumTitleActionClass, iChatBaseComponentClass)
                    .newInstance(action, baseComponent);

            NMSNetwork.sendPacket(connection, titlePacket);
        } catch (NoSuchMethodException | InstantiationException
                | IllegalAccessException | InvocationTargetException e) {
            throw new IncompatibleMinecraftVersionException("Error while sending a " + action + " title packet",
                    e instanceof InvocationTargetException ? e.getCause() : e);
        }
    }
}
