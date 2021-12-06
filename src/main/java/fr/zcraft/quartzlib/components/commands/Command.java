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

import fr.zcraft.quartzlib.components.commands.CommandException.Reason;
import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.text.RawMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class Command {
    private static final Pattern FLAG_PATTERN = Pattern.compile("(--?)[a-zA-Z0-9-]+");

    protected CommandGroup commandGroup;
    protected String commandName;
    protected String usageParameters;
    protected String commandDescription;
    protected String[] aliases;

    protected boolean flagsEnabled;
    protected Set<String> acceptedFlags;

    protected CommandSender sender;
    protected String[] args;
    protected Set<String> flags;

    /**
     * Parses arguments to extract flags.
     *
     * <p>This method is made static and with all data as argument to be able to
     * be unit tested.</p>
     *
     * @param args          The raw arguments.
     * @param acceptedFlags A set with lowercase accepted flags.
     * @param realArgs      An initially empty list filled with the real
     *                      arguments, ordered.
     * @param flags         An initially empty set filled with flags found in
     *                      the raw arguments.
     */
    private static void parseArgs(final String[] args, final Set<String> acceptedFlags, List<String> realArgs,
                                  Set<String> flags) {
        for (final String arg : args) {
            if (!FLAG_PATTERN.matcher(arg).matches()) {
                realArgs.add(arg);
                continue;
            }

            final Set<String> flagsInArg;
            if (arg.startsWith("--")) {
                final String flatFlag = arg.replace("--", "").trim().toLowerCase();
                if (isValidFlag(acceptedFlags, flatFlag)) {
                    flagsInArg = Collections.singleton(flatFlag);
                } else {
                    realArgs.add(arg);
                    continue;
                }
            } else {
                final String flatFlags = arg.replace("-", "").trim().toLowerCase();
                flagsInArg = new HashSet<>(flatFlags.length());

                for (char c : flatFlags.toCharArray()) {
                    final String flag = String.valueOf(c);
                    if (isValidFlag(acceptedFlags, flag)) {
                        flagsInArg.add(flag);
                    }
                }

                // If there is no valid flag at all in the argument, we ignore it and
                // add it back to args
                if (flagsInArg.isEmpty()) {
                    realArgs.add(arg);
                    continue;
                }
            }

            flags.addAll(flagsInArg);
        }
    }

    /**
     * Parses arguments to extract flags (if enabled).
     *
     * @param args The raw arguments passed to the command.
     */
    private void parseArgs(String[] args) {
        if (!flagsEnabled) {
            this.args = args;
            this.flags = null;
            return;
        }

        final List<String> argsList = new ArrayList<>(args.length);
        flags = new HashSet<>();

        parseArgs(args, acceptedFlags, argsList, flags);

        this.args = argsList.toArray(new String[0]);
    }

    /**
     * Checks if a flag is accepted.
     *
     * @param acceptedFlags A list of accepted flags. Can be empty or {@code
     *                      null} accepts all flags while empty accept no one.
     * @param flag          The flag to test.
     * @return {@code true} if this flag is valid.
     */
    private static boolean isValidFlag(Set<String> acceptedFlags, String flag) {
        return acceptedFlags != null && (acceptedFlags.size() == 0 || acceptedFlags.contains(flag.toLowerCase()));
    }

    /**
     * Displays a gray informational message.
     *
     * @param sender  The receiver of the message.
     * @param message The message to display.
     */
    protected static void info(CommandSender sender, String message) {
        sender.sendMessage("§7" + message);
    }

    /**
     * Displays a gray informational message to the sender.
     *
     * @param message The message to display.
     */
    protected void info(String message) {
        info(sender, message);
    }


    /**
     * Displays a green success message.
     *
     * @param sender  The receiver of the message.
     * @param message The message to display.
     */
    protected static void success(CommandSender sender, String message) {
        sender.sendMessage("§a" + message);
    }

    /**
     * Displays a green success message to the sender.
     *
     * @param message The message to display.
     */
    protected void success(String message) {
        success(sender, message);
    }

    /**
     * Displays a red warning message.
     *
     * @param sender  The receiver of the message.
     * @param message The message to display.
     */
    protected static void warning(CommandSender sender, String message) {
        sender.sendMessage("§c" + message);
    }

    /**
     * Displays a red warning message to the sender.
     *
     * @param message The message to display.
     */
    protected void warning(String message) {
        warning(sender, message);
    }

    private static String invalidParameterString(int index, final String expected) {
        return "Argument #" + (index + 1) + " invalid: expected " + expected;
    }

    private static String invalidParameterString(int index, final Object[] expected) {
        String[] expectedStrings = new String[expected.length];

        for (int i = expected.length; i-- > 0; ) {
            expectedStrings[i] = expected[i].toString().toLowerCase();
        }

        String expectedString = StringUtils.join(expectedStrings, ',');

        return "Argument #" + (index + 1) + " invalid: expected " + expectedString;
    }

    /**
     * Runs the command.
     *
     * <p>Use protected fields to access data (like {@link #args}).</p>
     *
     * @throws CommandException If something bad happens.
     */
    protected abstract void run() throws CommandException;

    /**
     * Initializes the command. Internal use.
     *
     * @param commandGroup The group this command instance belongs to.
     */
    void init(CommandGroup commandGroup) {
        this.commandGroup = commandGroup;

        CommandInfo commandInfo = this.getClass().getAnnotation(CommandInfo.class);

        if (commandInfo == null) {
            throw new IllegalArgumentException("Command has no CommandInfo annotation");
        }

        commandName = commandInfo.name().toLowerCase();
        usageParameters = commandInfo.usageParameters();
        commandDescription = commandGroup.getDescription(commandName);
        aliases = commandInfo.aliases();

        WithFlags withFlags = this.getClass().getAnnotation(WithFlags.class);
        flagsEnabled = withFlags != null;
        if (flagsEnabled) {
            acceptedFlags = new HashSet<>();
            for (final String flag : withFlags.value()) {
                acceptedFlags.add(flag.toLowerCase());
            }
        } else {
            acceptedFlags = Collections.emptySet();
        }
    }

    /**
     * Checks if a given sender is allowed to execute this command.
     *
     * @param sender The sender.
     * @return {@code true} if the sender can execute the command.
     */
    public boolean canExecute(CommandSender sender) {
        String permissionPrefix = QuartzLib.getPlugin().getName().toLowerCase() + ".";
        String globalPermission = Commands.getGlobalPermission();

        if (globalPermission != null) {
            if (sender.hasPermission(permissionPrefix + globalPermission)) {
                return true;
            }
        }

        return sender.hasPermission(permissionPrefix + commandGroup.getUsualName());
    }

    /**
     * Checks if a given sender is allowed to execute this command.
     *
     * @param sender The sender.
     * @param args   The arguments passed to the command.
     * @return {@code true} if the sender can execute the command.
     */
    public boolean canExecute(CommandSender sender, String[] args) {
        return canExecute(sender);
    }

    /**
     * Tab-completes the command. This command should be overridden.
     *
     * <p>Use protected fields to access data (like {@link #args}).</p>
     *
     * @return A list with suggestions, or {@code null} without suggestions.
     * @throws CommandException If something bad happens.
     */
    protected List<String> complete() throws CommandException {
        return null;
    }

    /**
     * Executes this command.
     *
     * @param sender The sender.
     * @param args   The raw arguments passed to the command.
     */
    public void execute(CommandSender sender, String[] args) {
        this.sender = sender;
        parseArgs(args);

        try {
            if (!canExecute(sender, args)) {
                throw new CommandException(this, Reason.SENDER_NOT_AUTHORIZED);
            }
            run();
        } catch (CommandException ex) {
            warning(ex.getReasonString());
        }

        this.sender = null;
        this.args = null;
        this.flags = null;
    }

    /**
     * Tab-completes this command.
     *
     * @param sender The sender.
     * @param args   The raw arguments passed to the command.
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> result = null;

        this.sender = sender;
        parseArgs(args);

        try {
            if (canExecute(sender, args)) {
                result = complete();
            }
        } catch (CommandException ignored) { }

        this.sender = null;
        this.args = null;
        this.flags = null;

        if (result == null) {
            result = new ArrayList<>();
        }
        return result;
    }

    /**
     * Returns this command's usage parameters.
     * @return This command's usage parameters.
     */
    public String getUsageParameters() {
        return usageParameters;
    }

    /**
     * Returns this command's usage string.
     * @return This command's usage string, formatted like this: {@code
     * /{command} {sub-command} {usage parameters}}.
     */
    public String getUsageString() {
        return "/" + commandGroup.getUsualName() + " " + commandName + " " + usageParameters;
    }

    /**
     * Returns the name of this command.
     * @return The name of this command.
     */
    public String getName() {
        return commandName;
    }


    ///////////// Common methods for commands /////////////

    /**
     * Get the command group.
     * @return The command group this command belongs to.
     */
    CommandGroup getCommandGroup() {
        return commandGroup;
    }

    /**
     * Get the aliases.
     * @return The aliases of this command.
     */
    public String[] getAliases() {
        return aliases;
    }

    /**
     * Checks if the given name matches this command's, or any of its aliases.
     * @param name A command name.
     * @return {@code true} if this command can be called like that,
     *     checking (without case) the command name then aliases.
     */
    public boolean matches(String name) {
        if (commandName.equals(name.toLowerCase())) {
            return true;
        }

        for (String alias : aliases) {
            if (alias.equals(name)) {
                return true;
            }
        }

        return false;
    }


    ///////////// Methods for command execution /////////////

    /**
     * Builds a command usage string.
     * @param args Some arguments.
     * @return A ready-to-be-executed command string with the passed arguments.
     */
    public String build(String... args) {
        StringBuilder command = new StringBuilder("/" + commandGroup.getUsualName() + " " + commandName);

        for (String arg : args) {
            command.append(" ").append(arg);
        }

        return command.toString();
    }

    /**
     * Stops the command execution because an argument is invalid, and displays
     * an error message.
     *
     * @param reason The error.
     * @throws CommandException the thrown exception.
     */
    protected void throwInvalidArgument(String reason) throws CommandException {
        throw new CommandException(this, Reason.INVALID_PARAMETERS, reason);
    }

    /**
     * Stops the command execution because the command usage is disallowed, and
     * displays an error message.
     *
     * @throws CommandException the thrown exception.
     */
    protected void throwNotAuthorized() throws CommandException {
        throw new CommandException(this, Reason.SENDER_NOT_AUTHORIZED);
    }

    /**
     * Retrieves the {@link Player} who executed this command. If the command is
     * not executed by a player, aborts the execution and displays an error
     * message.
     *
     * @return The player executing this command.
     * @throws CommandException If the sender is not a player.
     */
    protected Player playerSender() throws CommandException {
        if (!(sender instanceof Player)) {
            throw new CommandException(this, Reason.COMMANDSENDER_EXPECTED_PLAYER);
        }
        return (Player) sender;
    }

    /**
     * Aborts the execution and displays an error message.
     *
     * @param message The message.
     * @throws CommandException the thrown exception.
     */
    protected void error(String message) throws CommandException {
        throw new CommandException(this, Reason.COMMAND_ERROR, message);
    }

    /**
     * Aborts the execution and displays a generic error message.
     *
     * @throws CommandException the thrown exception.
     */
    protected void error() throws CommandException {
        error("");
    }


    ///////////// Methods for autocompletion /////////////

    /**
     * Sends a JSON-formatted message to the sender.
     *
     * @param rawMessage The JSON message.
     * @throws CommandException if the command sender is not a player.
     */
    protected void tellRaw(String rawMessage) throws CommandException {
        RawMessage.send(playerSender(), rawMessage);
    }

    /**
     * Sends a {@linkplain RawText raw JSON text} to the sender.
     *
     * @param text The JSON message.
     */
    protected void send(RawText text) {
        RawMessage.send(sender, text);
    }

    /**
     * Returns the strings of the list starting with the given prefix.
     *
     * @param prefix The prefix.
     * @param list   The strings.
     * @return A sub-list containing the strings starting with prefix.
     */
    protected List<String> getMatchingSubset(String prefix, String... list) {
        return getMatchingSubset(Arrays.asList(list), prefix);
    }

    /**
     * Returns the strings of the list starting with the given prefix.
     *
     * @param list   The strings.
     * @param prefix The prefix.
     * @return A sub-list containing the strings starting with prefix.
     */
    protected List<String> getMatchingSubset(Iterable<? extends String> list, String prefix) {
        List<String> matches = new ArrayList<>();

        for (String item : list) {
            if (item.startsWith(prefix)) {
                matches.add(item);
            }
        }

        return matches;
    }


    ///////////// Methods for parameters /////////////

    /**
     * Returns a list of player names starting by the given prefix, among all
     * logged in players.
     *
     * @param prefix The prefix.
     * @return A sub-list containing the players names starting with prefix.
     */
    protected List<String> getMatchingPlayerNames(String prefix) {
        return getMatchingPlayerNames(Bukkit.getOnlinePlayers(), prefix);
    }

    /**
     * Returns a list of player names starting by the given prefix, among the
     * given players.
     *
     * @param players A list of players.
     * @param prefix  The prefix.
     * @return A sub-list containing the players names starting with prefix.
     */
    protected List<String> getMatchingPlayerNames(Iterable<? extends Player> players, String prefix) {
        List<String> matches = new ArrayList<String>();

        for (Player player : players) {
            if (player.getName().startsWith(prefix)) {
                matches.add(player.getName());
            }
        }

        return matches;
    }

    /**
     * Retrieves an integer at the given index, or aborts the execution if none
     * can be found.
     *
     * @param index The index.
     * @return The retrieved integer.
     * @throws CommandException If the value is invalid.
     */
    protected int getIntegerParameter(int index) throws CommandException {
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException e) {
            throw new CommandException(this, Reason.INVALID_PARAMETERS, invalidParameterString(index, "integer"));
        }
    }

    /**
     * Retrieves a double at the given index, or aborts the execution if none
     * can be found.
     *
     * @param index The index.
     * @return The retrieved double.
     * @throws CommandException If the value is invalid.
     */
    protected double getDoubleParameter(int index) throws CommandException {
        try {
            return Double.parseDouble(args[index]);
        } catch (NumberFormatException e) {
            throw new CommandException(this, Reason.INVALID_PARAMETERS,
                    invalidParameterString(index, "integer or decimal value"));
        }
    }

    /**
     * Retrieves a float at the given index, or aborts the execution if none can
     * be found.
     *
     * @param index The index.
     * @return The retrieved float.
     * @throws CommandException If the value is invalid.
     */
    protected float getFloatParameter(int index) throws CommandException {
        try {
            return Float.parseFloat(args[index]);
        } catch (NumberFormatException e) {
            throw new CommandException(this, Reason.INVALID_PARAMETERS,
                    invalidParameterString(index, "integer or decimal value"));
        }
    }

    /**
     * Retrieves a long at the given index, or aborts the execution if none can
     * be found.
     *
     * @param index The index.
     * @return The retrieved long.
     * @throws CommandException If the value is invalid.
     */
    protected long getLongParameter(int index) throws CommandException {
        try {
            return Long.parseLong(args[index]);
        } catch (NumberFormatException e) {
            throw new CommandException(this, Reason.INVALID_PARAMETERS, invalidParameterString(index, "integer"));
        }
    }

    /**
     * Retrieves aa boolean at the given index, or aborts the execution if none
     * can be found.
     *
     * <p>Accepts yes, y, on, true, 1, no, n, off, false, and 0.</p>
     *
     * @param index The index.
     * @return The retrieved boolean.
     * @throws CommandException If the value is invalid.
     */
    protected boolean getBooleanParameter(int index) throws CommandException {
        switch (args[index].toLowerCase().trim()) {
            case "yes":
            case "y":
            case "on":
            case "true":
            case "1":
                return true;

            case "no":
            case "n":
            case "off":
            case "false":
            case "0":
                return false;

            default:
                throw new CommandException(this, Reason.INVALID_PARAMETERS,
                        invalidParameterString(index, "boolean (yes/no)"));
        }
    }

    /**
     * Retrieves an enum value at the given index, or aborts the execution if
     * none can be found.
     *
     * <p>Checks against the enum values without case, but does not converts
     * spaces to underscores or things like that.</p>
     *
     * @param index    The index.
     * @param enumType The enum to search into.
     * @return The retrieved enum value.
     * @throws CommandException If the value cannot be found in the enum.
     */
    protected <T extends Enum<?>> T getEnumParameter(int index, Class<T> enumType) throws CommandException {
        Enum<?>[] enumValues = enumType.getEnumConstants();
        String parameter = args[index].toLowerCase();

        for (Enum<?> value : enumValues) {
            if (value.toString().toLowerCase().equals(parameter)) {
                return (T) value;
            }
        }

        throw new CommandException(this, Reason.INVALID_PARAMETERS, invalidParameterString(index, enumValues));
    }

    /**
     * Retrieves a player from its name at the given index, or aborts the
     * execution if none can be found.
     *
     * @param index The index.
     * @return The retrieved player.
     * @throws CommandException If the value is invalid.
     */
    protected Player getPlayerParameter(int index) throws CommandException {
        String parameter = args[index];

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equals(parameter)) {
                return player;
            }
        }

        throw new CommandException(this, Reason.INVALID_PARAMETERS, invalidParameterString(index, "player name"));
    }

    /**
     * Retrieves a player from its name at the given index, or aborts the
     * execution if none can be found.
     *
     * @param parameter The string containing the name.
     * @param callback  A consumer that will use the offline player's UUID
     */
    public void offlinePlayerParameter(final String parameter, final Consumer<UUID> callback) {
        CommandWorkers cw = new CommandWorkers();
        cw.offlineNameFetch(parameter, callback);
    }

    /**
     * Retrieves a player from its name at the given index, or aborts the
     * execution if none can be found.
     *
     * @param index    The index.
     * @param callback A consumer that will use the offline player's UUID
     */
    public void offlinePlayerParameter(int index, final Consumer<UUID> callback) {
        final String parameter = args[index];
        offlinePlayerParameter(parameter, callback);
    }


    ///////////// Methods for flags /////////////

    /**
     * Checks if a flag is set.
     *
     * <p> To use this functionality, your command class must be annotated by
     * {@link WithFlags}. </p>
     *
     * <p>A flag is a value precessed by one or two dashes, and composed of
     * alphanumerical characters, and dashes.<br> Flags are not
     * case-sensitive.</p>
     *
     * <p>One-letter flags are passed using the syntax {@code -f} (for the
     * {@code f} flag). Multiple one-letter flags can be passed at once, like
     * this: {@code -fcrx} (for the {@code f}, {@code c}, {@code r}, and {@code
     * x} flags).</p>
     *
     * <p>Multiple-letter flags are passed using the syntax {@code --flag} (for
     * the {@code flag} flag). To pass multiple multiple-letter flags, you must
     * repeat the {@code --}: {@code --flag --other-flag} (for the flags {@code
     * flag} and {@code other-flag}).</p>
     *
     * <p>With the {@link WithFlags} annotation alone, all flags are caught.
     * You can constrain the flags retrieved by passing an array of flags to the
     * annotation, like this:
     *
     * <pre>
     *     \@WithFlags({"flag", "f"})
     * </pre>
     *
     * <p>If a flag-like argument is passed but not in the flags whitelist, it will
     * be left in the {@link #args} parameters like any other arguments. Else,
     * the retrieved flags are removed from the arguments list.</p>
     *
     * @param flag The flag.
     * @return {@code true} if the flag was passed by the player.
     */
    protected boolean hasFlag(String flag) {
        return flags != null && flags.contains(flag.toLowerCase());
    }
}
