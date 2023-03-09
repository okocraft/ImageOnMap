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

package fr.moribus.imageonmap.commands;

import fr.moribus.imageonmap.gui.GuiUtils;

import org.bukkit.ChatColor;


public class CommandException extends Exception {
    private final Reason reason;
    private final Command command;
    private final String extra;

    /**
     * A new CommandException.
     * @param command The command that raised the exception.
     * @param reason The reason code.
     * @param extra Any extra message.
     */
    public CommandException(Command command, Reason reason, String extra) {
        this.command = command;
        this.reason = reason;
        this.extra = extra;
    }

    public CommandException(Command command, Reason reason) {
        this(command, reason, "");
    }

    /**
     * Builds the "reason" string for this command exception.
     * @return The reason string.
     */
    public String getReasonString() {
        return switch (reason) {
            case COMMANDSENDER_EXPECTED_PLAYER ->  "You must be a player to use this command.";
            case INVALID_PARAMETERS -> {
                final String prefix = ChatColor.GOLD + Commands.CHAT_PREFIX + " " + ChatColor.RESET;
                yield "\n"
                        + ChatColor.RED + Commands.CHAT_PREFIX + ' ' + ChatColor.BOLD + "Invalid argument" + '\n'
                        + GuiUtils.generatePrefixedFixedLengthString(ChatColor.RED + Commands.CHAT_PREFIX + " ", extra)
                        + '\n'
                        + GuiUtils.generatePrefixedFixedLengthString(prefix, "Usage: " + command.getUsageString())
                        + '\n'
                        + GuiUtils.generatePrefixedFixedLengthString(prefix,
                        "For more information, use /" + command.getCommandGroup().getUsualName()
                                + " help " + command.getName());
            }
            case COMMAND_ERROR -> extra.isEmpty() ? "An unknown error suddenly happened." : extra;
            case SENDER_NOT_AUTHORIZED -> "You do not have the permission to use this command.";
        };
    }

    public enum Reason {
        COMMANDSENDER_EXPECTED_PLAYER,
        INVALID_PARAMETERS,
        COMMAND_ERROR,
        SENDER_NOT_AUTHORIZED
    }
}
