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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * Utility to send JSON messages.
 *
 * <p>This tool uses the /tellraw command to send the messages. If the JSON is not correctly
 * formatted, the message will not be sent and a Runtime exception containing the exception throw by
 * the vanilla /tellraw command will be thrown.</p>
 */
public final class RawMessage {
    private RawMessage() {
    }

    /**
     * Sends raw text to the given command sender.
     *
     * <p>If the command sender is a Player, the JSON representation is used (using tellraw).
     * Otherwise, the message is converted to formatted text and is sent normally.</p>
     *
     * @param commandSender The receiver of the message.
     * @param text          The JSON message.
     * @throws RuntimeException if the JSON is invalid (or other problem encountered while sending
     *                          the message).
     */
    public static void send(CommandSender commandSender, RawText text) {
        if (commandSender instanceof Player) {
            send((Player) commandSender, text.toJSONString());
        } else {
            commandSender.sendMessage(text.toFormattedText());
        }
    }

    /**
     * Sends a raw JSON message to the given player.
     *
     * @param player The receiver of the message.
     * @param json   The JSON message.
     * @throws RuntimeException if the JSON is invalid (or other problem encountered while sending
     *                          the message).
     */
    public static void send(Player player, String json) {
        send(player.getName(), json);
    }

    /**
     * Sends a raw JSON message to the given selector.
     *
     * @param selector The receiver(s) of the message. Spaces are disallowed. This has to be a valid
     *                 Minecraft selector, like {@code @a}, or {@code @r[m=0]}.
     * @param text     The JSON message.
     * @throws RuntimeException if the JSON is invalid (or other problem encountered while sending
     *                          the message).
     */
    public static void send(String selector, RawText text) {
        send(selector, text.toJSONString());
    }

    /**
     * Sends a raw JSON message to the given selector.
     *
     * @param selector The receiver(s) of the message. Spaces are disallowed. This has to be a valid
     *                 Minecraft selector, like {@code @a}, or {@code @r[m=0]}.
     * @param json     The JSON message.
     * @throws RuntimeException if the JSON is invalid (or other problem encountered while sending
     *                          the message).
     */
    public static void send(String selector, String json) {
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tellraw " + selector + " " + json);
        } catch (CommandException e) {
            throw new RuntimeException("Unable to send raw message to " + selector + ", is the JSON valid? ",
                    e.getCause() != null ? e.getCause() : e);
        }
    }

    /**
     * Broadcasts a raw JSON message to the server.
     *
     * @param json The JSON message.
     * @throws RuntimeException if the JSON is invalid (or other problem encountered while sending
     *                          the message).
     */
    public static void broadcast(String json) {
        send("@a", json);
    }

    /**
     * Broadcasts a raw JSON message to the server.
     *
     * @param text The message.
     * @throws RuntimeException if a problem is encountered while sending the message.
     */
    public static void broadcast(RawText text) {
        send("@a", text.toJSONString());
        Bukkit.getConsoleSender().sendMessage(text.toFormattedText());
    }

}
