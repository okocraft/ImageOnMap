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
import fr.zcraft.quartzlib.core.QuartzLib;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
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
        return sender.hasPermission(permissionPrefix + commandGroup.getUsualName());
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
            if (!canExecute(sender)) {
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
            if (canExecute(sender)) {
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
