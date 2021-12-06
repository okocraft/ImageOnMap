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

import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;


public class CommandGroup implements TabCompleter, CommandExecutor {
    private final CommandGroup shortcutCommandGroup;
    private final String[] names;
    private final Class<? extends Command>[] commandsClasses;
    private final ArrayList<Command> commands = new ArrayList<>();
    private final HashMap<String, String> commandsDescriptions = new HashMap<>();
    private String description = "";

    CommandGroup(CommandGroup shortcutCommandGroup, Class<? extends Command> commandClass, String... names) {
        this.names = names;
        this.commandsClasses = new Class[] {commandClass};
        this.shortcutCommandGroup = shortcutCommandGroup;
        initCommands();
    }

    CommandGroup(String[] names, Class<? extends Command>... commandsClasses) {
        this.names = names;
        this.commandsClasses = commandsClasses;
        this.shortcutCommandGroup = null;
        initDescriptions();
        initCommands();
    }

    /**
     * Gets the command args from the given group args.
     * @param args The group args.
     * @return The command args.
     */
    public static String[] getCommandArgsFromGroupArgs(String[] args) {
        String[] commandArgs = new String[args.length - 1];

        System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

        return commandArgs;
    }

    private void initDescriptions() {
        String fileName = "help/" + getUsualName() + ".txt";
        InputStream stream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (stream == null) {
            PluginLogger.warning("Could not load description file for the " + getUsualName() + " command");
            return;
        }

        Scanner scanner = new Scanner(stream);
        StringBuilder builder = new StringBuilder();

        //Getting the group's description
        //And then each command's description
        int colonIndex;
        int firstSpaceIndex;
        boolean isGroupDescription = true;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            colonIndex = line.indexOf(':');
            if (isGroupDescription) {
                firstSpaceIndex = line.indexOf(' ');
                if (colonIndex > 0 && firstSpaceIndex > colonIndex) {
                    isGroupDescription = false;
                }
            }

            if (isGroupDescription) {
                builder.append(line).append('\n');
            } else {
                commandsDescriptions.put(line.substring(0, colonIndex).trim(),
                        line.substring(colonIndex + 1).trim());
            }
        }

        scanner.close();
        description = builder.toString().trim();

    }

    private void initCommands() {
        for (Class<? extends Command> commandClass : commandsClasses) {
            addCommand(commandClass);
        }

        if (!isShortcutCommand()) {
            addCommand(HelpCommand.class);
        }
    }

    private void addCommand(Class<? extends Command> commandClass) {
        Constructor<? extends Command> constructor;
        Command newCommand;
        try {
            constructor = commandClass.getConstructor();
            newCommand = constructor.newInstance();
            newCommand.init(isShortcutCommand() ? shortcutCommandGroup : this);
            commands.add(newCommand);
        } catch (Exception ex) {
            PluginLogger.warning("Exception while initializing command", ex);
        }
    }

    /**
     * Execute the command matching the args.
     * @param sender The command's sender.
     * @param args The command args.
     * @return true if command ran successfuly.
     */
    public boolean executeMatchingCommand(CommandSender sender, String[] args) {
        if (isShortcutCommand()) {
            commands.get(0).execute(sender, args);
            return true;
        }

        if (args.length <= 0) {
            sender.sendMessage(getUsage());
            return false;
        }

        String commandName = args[0];
        String[] commandArgs = getCommandArgsFromGroupArgs(args);

        return executeMatchingCommand(sender, commandName, commandArgs);
    }

    private boolean executeMatchingCommand(CommandSender sender, String commandName, String[] args) {
        Command command = getMatchingCommand(commandName);
        if (command != null) {
            command.execute(sender, args);
        } else {
            sender.sendMessage(getUsage());
        }
        return command != null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String label,
                                      String[] args) {
        return tabComplete(sender, args);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        return executeMatchingCommand(sender, args);
    }

    /**
     * Computes a list of possible autocomplete suggestions for the given partial arguments.
     * @param sender The sender of the command.
     * @param args The partial arguments.
     * @return A list of suggestions.
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (isShortcutCommand()) {
            return commands.get(0).tabComplete(sender, args);
        }
        if (args.length <= 1) {
            return tabComplete(sender, args.length == 1 ? args[0] : null);
        }
        String commandName = args[0];
        String[] commandArgs = getCommandArgsFromGroupArgs(args);
        return tabCompleteMatching(sender, commandName, commandArgs);
    }

    /**
     * Computes a list of possible autocomplete suggestions for the given command.
     * @param sender The sender of the command.
     * @param commandName The name of the command
     * @return A list of suggestions.
     */
    public List<String> tabComplete(CommandSender sender, String commandName) {
        ArrayList<String> matchingCommands = new ArrayList<String>();
        for (Command command : commands) {
            if (!command.canExecute(sender)) {
                continue;
            }
            if (commandName == null || command.getName().startsWith(commandName.toLowerCase())) {
                matchingCommands.add(command.getName());
            }
        }
        return matchingCommands;
    }

    private List<String> tabCompleteMatching(CommandSender sender, String commandName, String[] args) {
        Command command = getMatchingCommand(commandName);
        if (command != null) {
            return command.tabComplete(sender, args);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Gets the command matching the given name.
     * @param commandName The command name.
     * @return The matching command, or null if none were found.
     */
    public Command getMatchingCommand(String commandName) {
        for (Command command : commands) {
            if (command.matches(commandName)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Gets the command matching the given class.
     * @param commandClass The command class.
     * @return The matching gommand, or null if none were found.
     */
    public Command getCommandInfo(Class<? extends Command> commandClass) {
        for (Command command : commands) {
            if (command.getClass().equals(commandClass)) {
                return command;
            }
        }
        return null;
    }

    /**
     * Return if this command matches the given name.
     * @param name The name of the command to test for.
     * @return if this command matches the given name.
     */
    public boolean matches(String name) {
        name = name.toLowerCase();
        for (String commandName : names) {
            if (commandName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns an array of all subcommands.
     * @return an array of all subcommands.
     */
    public String[] getCommandsNames() {
        String[] commandsNames = new String[commands.size()];

        for (int i = 0; i < commands.size(); i++) {
            commandsNames[i] = commands.get(i).getName();
        }

        return commandsNames;
    }

    void register(JavaPlugin plugin) {
        PluginCommand bukkitCommand = plugin.getCommand(getUsualName());
        if (bukkitCommand == null) {
            throw new IllegalStateException("Command " + getUsualName() + " is not correctly registered in plugin.yml");
        }
        bukkitCommand.setAliases(getAliases());
        bukkitCommand.setExecutor(this);
        bukkitCommand.setTabCompleter(this);
    }

    protected String getUsage() {
        if (isShortcutCommand()) {
            return "§cUsage: " + commands.get(0).getUsageString();
        }
        return "§cUsage: /" + getUsualName()
                + " <" + StringUtils.join(getCommandsNames(), "|") + ">";
    }

    public String getUsualName() {
        return names[0];
    }

    public String[] getNames() {
        return names.clone();
    }

    public List<String> getAliases() {
        return Arrays.asList(names).subList(1, names.length);
    }

    public Command[] getCommands() {
        return commands.toArray(new Command[0]);
    }

    public String getDescription() {
        return description;
    }

    public String getDescription(String commandName) {
        return commandsDescriptions.get(commandName);
    }

    public boolean isShortcutCommand() {
        return shortcutCommandGroup != null;
    }

    public CommandGroup getShortcutCommandGroup() {
        return shortcutCommandGroup;
    }

}
