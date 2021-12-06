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

import fr.moribus.imageonmap.gui.GuiUtils;
import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.commands.PaginatedTextView;
import fr.zcraft.quartzlib.tools.text.RawMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(name = "help", usageParameters = "<command name>")
public class HelpCommand extends Command {
    @Override
    protected void run() throws CommandException {
        if (args.length < 1) {
            groupHelp(1);
        } else {
            if (args.length == 1 && args[0].startsWith("--page=")) {
                try {
                    groupHelp(Integer.valueOf(args[0].split("=")[1]));
                    return;
                } catch (NumberFormatException ignored) {
                }
            }

            commandHelp();
        }
    }

    private void groupHelp(int page) throws CommandException {
        final List<Command> displayedCommands = new ArrayList<>();

        for (Command subCommands : commandGroup.getCommands()) {
            if (subCommands.canExecute(sender, args)) {
                displayedCommands.add(subCommands);
            }
        }

        if (sender instanceof Player) {
            info("");
        }

        new GroupHelpPagination()
                .setData(displayedCommands.toArray(new Command[displayedCommands.size()]))
                .setCurrentPage(page)
                .display(sender);
    }

    private void commandHelp() throws CommandException {
        Command command = commandGroup.getMatchingCommand(args[0]);
        if (command == null) {
            error("The specified command does not exist.");
            return;
        }

        if (!command.canExecute(sender, args)) {
            warning("You do not have the permission to use this command.");
        }

        String message = "\n";
        message += GuiUtils.generatePrefixedFixedLengthString("§6" + Commands.CHAT_PREFIX + "§l ",
                QuartzLib.getPlugin().getName() + " help for /" + command.getCommandGroup().getUsualName() + " "
                        + command.getName()) + "\n";
        message += GuiUtils.generatePrefixedFixedLengthString("§6" + Commands.CHAT_PREFIX + " ",
                "Usage: §r" + command.getUsageString()) + "\n";

        try {
            String help = getHelpText(command);
            if (help.isEmpty()) {
                message += "§c" + Commands.CHAT_PREFIX + " There is no help message for this command.";
            } else {
                message += help;
            }
        } catch (IOException ex) {
            message += "§c" + Commands.CHAT_PREFIX + " Could not read help for this command.";
            PluginLogger.warning("Could not read help for the command: " + command.getName(), ex);
        }

        sender.sendMessage(message);
    }

    private String getHelpText(Command command) throws IOException {
        String fileName = "help/" + commandGroup.getUsualName()
                + "/" + command.getName() + ".txt";

        StringBuilder result = new StringBuilder();

        InputStream stream = getClass().getClassLoader().getResourceAsStream(fileName);
        if (stream == null) {
            return "";
        }

        Scanner scanner = new Scanner(stream);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            result.append("§l§9" + Commands.CHAT_PREFIX + " §r").append(line).append("\n");
        }

        scanner.close();

        return result.toString().trim();
    }


    @Override
    protected List<String> complete() throws CommandException {
        if (args.length != 1) {
            return null;
        }

        ArrayList<String> matches = new ArrayList<>();

        for (Command command : commandGroup.getCommands()) {
            if (command.getName().startsWith(args[0])) {
                matches.add(command.getName());
            }
        }

        return matches;
    }


    private class GroupHelpPagination extends PaginatedTextView<Command> {
        @Override
        protected void displayHeader(CommandSender receiver) {
            final String header = ChatColor.BOLD + (commandGroup.getDescription().isEmpty()
                    ? QuartzLib.getPlugin().getName() + " help for /" + commandGroup.getUsualName()
                    : commandGroup.getDescription());

            receiver.sendMessage(receiver instanceof Player
                    ? GuiUtils.generatePrefixedFixedLengthString(
                    ChatColor.BLUE + Commands.CHAT_PREFIX + " " + ChatColor.RESET, header)
                    : header
            );
        }

        @Override
        protected void displayItem(CommandSender receiver, Command command) {
            final String commandName = "/" + commandGroup.getUsualName() + " " + command.getName();
            final String description = commandGroup.getDescription(command.getName());

            String helpMessage = ChatColor.GOLD + commandName;
            if (description != null) {
                helpMessage += ChatColor.GOLD + ": " + ChatColor.WHITE + description;
            }

            final String formattedHelpMessage = receiver instanceof Player
                    ?
                    GuiUtils.generatePrefixedFixedLengthString(ChatColor.GOLD + Commands.CHAT_PREFIX + " ", helpMessage)
                    : helpMessage;

            RawText helpLine = RawText.fromFormattedString(
                    formattedHelpMessage,
                    new RawText().suggest(commandName + " ").hover(new RawText(command.getUsageString()))
            );

            RawMessage.send(receiver, helpLine);
        }

        @Override
        protected String getCommandToPage(int page) {
            return build("--page=" + page);
        }
    }
}
