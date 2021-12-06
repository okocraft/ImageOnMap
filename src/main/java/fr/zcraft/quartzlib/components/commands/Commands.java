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

package fr.zcraft.quartzlib.components.commands;

import fr.zcraft.quartzlib.core.QuartzComponent;
import fr.zcraft.quartzlib.core.QuartzLib;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

public class Commands extends QuartzComponent {
    public static final String CHAT_PREFIX = "┃";

    private static final List<CommandGroup> commandGroups = new ArrayList<>();
    private static String globalPermission;

    /**
     * Registers a shortcut command.
     */
    public static void registerShortcut(String commandGroupName, Class<? extends Command> commandClass,
                                        String... shortcutNames) {
        CommandGroup group = getMatchingCommandGroup(commandGroupName);
        if (group == null) {
            throw new IllegalArgumentException("Invalid command group name: " + commandGroupName);
        }
        CommandGroup newCommandGroup = new CommandGroup(group, commandClass, shortcutNames);

        newCommandGroup.register(QuartzLib.getPlugin());
        commandGroups.add(newCommandGroup);
    }

    /**
     * Registers many new commands.
     * @param names The names of the commands
     * @param commandsClasses The matching classes for the commands
     */
    public static void register(String[] names, Class<? extends Command>... commandsClasses) {
        final CommandGroup commandGroup = new CommandGroup(names, commandsClasses);
        commandGroup.register(QuartzLib.getPlugin());

        commandGroups.add(commandGroup);
    }

    public static void register(String name, Class<? extends Command>... commandsClasses) {
        register(new String[] {name}, commandsClasses);
    }

    /**
     * Executes a registered command.
     * @param sender The command sender.
     * @param commandName The name of the command.
     * @param args The command's arguments.
     * @return Whether the command was found.
     */
    public static boolean execute(CommandSender sender, String commandName, String[] args) {
        CommandGroup commandGroup = getMatchingCommandGroup(commandName);
        if (commandGroup == null) {
            return false;
        }
        commandGroup.executeMatchingCommand(sender, args);
        return true;
    }


    /**
     * Computes a list of possible autocomplete suggestions for the given command.
     * @param sender The sender of the command.
     * @param commandName The name of the command.
     * @param args The partial arguments for the command.
     * @return A list of suggestions.
     */
    public static List<String> tabComplete(CommandSender sender, String commandName, String[] args) {
        CommandGroup commandGroup = getMatchingCommandGroup(commandName);
        if (commandGroup == null) {
            return new ArrayList<>();
        }
        return commandGroup.tabComplete(sender, args);
    }

    /**
     * Gets the command matching the given class.
     * @param commandClass The command class.
     * @return The matching gommand, or null if none were found.
     */
    public static Command getCommandInfo(Class<? extends Command> commandClass) {
        Command command = null;
        for (CommandGroup commandGroup : commandGroups) {
            command = commandGroup.getCommandInfo(commandClass);
            if (command != null) {
                break;
            }
        }
        return command;
    }

    private static CommandGroup getMatchingCommandGroup(String commandName) {
        for (CommandGroup commandGroup : commandGroups) {
            if (commandGroup.matches(commandName)) {
                return commandGroup;
            }
        }
        return null;
    }


    public static String getGlobalPermission() {
        return globalPermission;
    }

    public static void setGlobalPermission(String permission) {
        globalPermission = permission;
    }
}
