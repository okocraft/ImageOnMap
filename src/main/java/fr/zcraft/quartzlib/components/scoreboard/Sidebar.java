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

package fr.zcraft.quartzlib.components.scoreboard;

import com.google.common.collect.ImmutableSet;
import fr.zcraft.quartzlib.components.scoreboard.sender.ObjectiveSender;
import fr.zcraft.quartzlib.components.scoreboard.sender.SidebarObjective;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.exceptions.IncompatibleMinecraftVersionException;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.runners.RunAsyncTask;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;


/**
 * This is the base class of a sidebar scoreboard. To create one, create a class extending this one,
 * set the properties of the scoreboard, and complete the methods returning the scoreboard's
 * content.
 * <p>As this utility does uses packets, it does not interfere with Bukkit's scoreboard assigned
 * to the players: you can use both of these utilities at the same time, as long as you don't
 * display a sidebar scoreboard using the Bukkit API.</p>
 *
 * @author Amaury Carrade
 */
public abstract class Sidebar {
    /* ** Global attributes ** */

    private static final Set<Sidebar> sidebars = new CopyOnWriteArraySet<>();

    // Asynchronously available list of the logged in players.
    private static final Map<UUID, Player> loggedInPlayers = new ConcurrentHashMap<>();


    /* ** This instance's attributes ** */

    private final Set<UUID> recipients = new CopyOnWriteArraySet<>();
    // Other cases
    private final Map<UUID, SidebarObjective> objectives = new ConcurrentHashMap<>();
    private int lastLineScore = 1;
    private SidebarMode contentMode = SidebarMode.GLOBAL;
    private SidebarMode titleMode = SidebarMode.GLOBAL;
    private boolean automaticDeduplication = true;
    private boolean async = false;
    private long autoRefreshDelay = 0;
    private BukkitTask refreshTask = null;
    // Only used if both titleMode and contentMode are global.
    private SidebarObjective globalObjective = null;


    public Sidebar() {
        sidebars.add(this);
    }



    /* ** Methods to override ** */

    /**
     * Initializes the scoreboards API. Must be called before this library is used.
     */
    public static void init() {
        updateLoggedInPlayers();
        QuartzLib.registerEvents(new OnlinePlayersListener());
    }

    /**
     * Clears the scoreboard API. Must be called when the plugin is disabled.
     */
    public static void exit() {
        loggedInPlayers.clear();

        for (Sidebar sidebar : sidebars) {
            sidebar.runAutoRefresh(false);
        }

        try {
            ObjectiveSender.clearForAll();
        } catch (IncompatibleMinecraftVersionException e) {
            PluginLogger.warning("Could not properly disable Sidebar", e);
        }
    }

    /**
     * Returns the current sidebars.
     *
     * @return The sidebars.
     */
    public static Set<Sidebar> getSidebars() {
        return ImmutableSet.copyOf(sidebars);
    }

    /**
     * Returns a set containing the currently logged-in players. This method can be used
     * asynchronously.
     *
     * @return The logged-in players.
     */
    public static Set<Player> getOnlinePlayersAsync() {
        return ImmutableSet.copyOf(loggedInPlayers.values());
    }



    /* ** Public API ** */

    /**
     * Get a player from an UUID. This method can be used asynchronously.
     * <p>The returned {@link Player} object must be used read-only for thread safety.</p>
     *
     * @param id The player's UUID.
     * @return The Player object; {@code null} if offline.
     */
    public static Player getPlayerAsync(final UUID id) {
        return loggedInPlayers.get(id);
    }

    /**
     * Updates the asynchronously-available list of logged-in players.
     * <p>This method needs to be called from the Bukkit's main thread!</p>
     */
    static void updateLoggedInPlayers() {
        synchronized (loggedInPlayers) {
            loggedInPlayers.clear();

            for (Player player : Bukkit.getOnlinePlayers()) {
                loggedInPlayers.put(player.getUniqueId(), player);
            }
        }
    }

    /**
     * Returns the content of the scoreboard. Called by the update methods.
     * <p>The first item of the list will be the top line of the sidebar board, and so on. If this
     * returns {@code null}, the scoreboard will not be updated.</p>
     *
     * @param player The receiver of this content. If the content's mode is set to {@link
     *               SidebarMode#GLOBAL}, this will always be {@code null}, and the method will be
     *               called one time per update cycle.
     * @return The content. {@code null} to cancel this update.
     */
    public abstract List<String> getContent(final Player player);

    /**
     * Returns the title of the scoreboard. Called by the update methods.
     *
     * @param player The receiver of this title. If the title's mode is set to {@link
     *               SidebarMode#GLOBAL}, this will always be {@code null}, and the method will be
     *               called one time per update cycle.
     * @return The title. {@code null} to cancel this update.
     */
    public abstract String getTitle(final Player player);

    /**
     * This method is called before every update method ({@link #getTitle(Player)} and {@link
     * #getContent(Player)}) by the {@link #refresh()} method.
     */
    public void preRender() {
    }

    /**
     * This method is called after every update method ({@link #getTitle(Player)} and {@link
     * #getContent(Player)}) by the {@link #refresh()} method.
     */
    public void postRender() {
    }

    /**
     * Sets the asynchronous status of this scoreboard.
     * <p> If {@code true}, the scoreboard will be updated asynchronously. Else, the update task will be
     * executed from the Bukkit's main thread.</p>
     * <p>WARNING — The async status is only guaranteed if the auto-refresh is used.</p>
     *
     * @param async {@code true} if async.
     */
    public void setAsync(final boolean async) {
        this.async = async;
    }

    /**
     * Sets the auto-refresh delay, in ticks. To disable, set to 0.
     * <p>This will not launch or update the updater. Use {@link #runAutoRefresh(boolean)} to
     * (re)launch it.</p>
     *
     * @param autoRefreshDelay The delay, in ticks.
     */
    public void setAutoRefreshDelay(final long autoRefreshDelay) {
        this.autoRefreshDelay = autoRefreshDelay;
    }

    /**
     * The score of the last line. The scores of the lines will be automatically calculated from
     * this score to the max score needed.
     * <p>If a score less than 1 is given, 1 is used.</p>
     *
     * @param lastLineScore The score of the bottom line.
     */
    public void setLastLineScore(final int lastLineScore) {
        this.lastLineScore = Math.max(lastLineScore, 1);
    }

    /**
     * Sets wherever, when the objective is built, the lines with the same value are automatically
     * deduplicated by adding a space after.
     * <p>If this is disabled, you'll still be able to insert blank lines with an empty string.</p>
     * <p>Please note that enabling this option may have a cost in performances if your sidebar is
     * updated for a lot of people and/or with high-cost calculations and/or very frequently.</p>
     *
     * @param automaticDeduplication {@code true} to enable.
     */
    public void setAutomaticDeduplication(boolean automaticDeduplication) {
        this.automaticDeduplication = automaticDeduplication;
    }

    /**
     * Sets the scoreboard content's mode of update: either a scoreboard per player or the same
     * scoreboard from everyone.
     *
     * @param contentMode The mode.
     */
    public void setContentMode(final SidebarMode contentMode) {
        this.contentMode = contentMode;
    }

    /**
     * Sets the scoreboard title's mode of update: either a title per player or the same scoreboard
     * from everyone.
     *
     * @param titleMode The mode.
     */
    public void setTitleMode(final SidebarMode titleMode) {
        this.titleMode = titleMode;
    }



    /* **  Per-line update API  ** */

    /**
     * Adds a recipient to this sidebar.
     *
     * @param id The recipient's UUID.
     */
    public void addRecipient(final UUID id) {
        recipients.add(id);
    }

    /**
     * Adds a recipient to this sidebar.
     *
     * @param player The recipient.
     */
    public void addRecipient(final Player player) {
        addRecipient(player.getUniqueId());
    }

    /* **  Private API  ** */

    /**
     * Removes a recipient from this sidebar.
     *
     * @param id The recipient's UUID.
     */
    public void removeRecipient(final UUID id) {
        recipients.remove(id);
        objectives.remove(id);

        if (contentMode == SidebarMode.GLOBAL && titleMode == SidebarMode.GLOBAL && globalObjective != null) {
            globalObjective.removeReceiver(id);
        }

        ObjectiveSender.clear(id);
    }

    /**
     * Removes a recipient from this sidebar.
     *
     * @param player The recipient.
     */
    public void removeRecipient(final Player player) {
        removeRecipient(player.getUniqueId());
    }

    /**
     * Refresh the whole scoreboard.
     * <p>Calls the update methods and updates the scoreboard for each recipient.</p>
     */
    public void refresh() {
        preRender();

        String title = null;
        List<String> content = null;

        if (titleMode == SidebarMode.GLOBAL) {
            title = getTitle(null);
        }

        if (contentMode == SidebarMode.GLOBAL) {
            content = getContent(null);
        }

        if (titleMode == SidebarMode.GLOBAL && contentMode == SidebarMode.GLOBAL) {
            // If nothing was done before (objective never created) and we don't have anything to
            // do, we exit now to avoid errors.
            if (title == null && content == null && globalObjective == null) {
                return;
            }

            // If the content needs to be refreshed, a new objective is created
            if (content != null || globalObjective == null) {
                globalObjective = constructObjective(title, content, recipients);
            }  else if (title != null) { // Else, only the title is updated, or nothing
                globalObjective.setDisplayName(title);
            }

            ObjectiveSender.send(globalObjective);
        } else {
            for (UUID id : recipients) {
                Player recipient = getPlayerAsync(id);
                if (recipient != null && recipient.isOnline()) {
                    refresh(recipient, title, content);
                }
            }
        }

        postRender();
    }

    /**
     * Updates the scoreboard for the given player.
     *
     * @param player The player.
     */
    private void refresh(final Player player, final String globalTitle, final List<String> globalContent) {
        String title = globalTitle;
        List<String> content = globalContent;

        final UUID playerID = player.getUniqueId();
        final boolean objectiveAlreadyExists = objectives.containsKey(playerID);


        if (titleMode == SidebarMode.PER_PLAYER) {
            title = getTitle(player);
        }

        if (contentMode == SidebarMode.PER_PLAYER) {
            content = getContent(player);
        }


        if (title != null || content != null) {
            if (content != null || !objectiveAlreadyExists) {
                final SidebarObjective objective = constructObjective(title, content, Collections.singleton(playerID));

                objectives.put(playerID, objective);
                ObjectiveSender.send(objective);
            } else {
                final SidebarObjective objective = objectives.get(playerID);

                objective.setDisplayName(title);
                ObjectiveSender.updateDisplayName(objective);
            }
        }
    }

    /* **  System-wide methods  ** */

    /**
     * (Re)Launches the auto-refresh task.
     * <p>Warning: if this method is called with {@code true} and the update task is already launched,
     * it will be killed and re-launched.</p>
     *
     * @param run {@code true} to run the task. {@code false} to stop it.
     */
    public void runAutoRefresh(final boolean run) {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }

        if (run) {
            if (async) {
                refreshTask = RunAsyncTask.timer(this::refresh, 1, autoRefreshDelay);
            } else {
                refreshTask = RunTask.timer(this::refresh, 1, autoRefreshDelay);
            }
        }
    }

    /**
     * Updates a single line of the scoreboard, replacing the old line by the new one.
     * <p>This method is only useful if the {@link #getContent(Player)} method returns {@code null};
     * else, this change will be overwritten by the newly-generated sidebar.</p>
     * <p>Please note that this can hardly be used with scoreboards with identical lines, due to the
     * auto-deduplication, if you use that feature. To handle this case, you can use the {@link
     * #updateLine(Player, int, String)} method instead.</p>
     * <p>If the old sidebar doesn't contains any line equal to {@code oldLine}, nothing is done.</p>
     *
     * @param player  The updated player; {@code null} if the content's mode is {@link
     *                SidebarMode#GLOBAL global}.
     * @param oldLine The replaced line.
     * @param newLine The new line.
     * @throws IllegalStateException    if there isn't any previously-constructed objective for that
     *                                  player (or at all, if the content's mode is {@link
     *                                  SidebarMode#GLOBAL global}).
     * @throws IllegalArgumentException if {@code player} is {@code null} and the content's mode is
     *                                  not {@link SidebarMode#GLOBAL global}.
     * @see #setAutomaticDeduplication(boolean)
     * @see #updateLine(Player, int, String)
     */
    public void updateLine(Player player, String oldLine, String newLine) {
        if ((contentMode == SidebarMode.GLOBAL && (globalObjective == null || player == null))
                || !objectives.containsKey(player.getUniqueId())) {
            throw new IllegalArgumentException(
                    "Cannot change the sidebar for that player because either the objective is not constructed"
                            + "for this player or the player is null out of the GLOBAL content mode.");
        }

        // We need the old score.
        SidebarObjective objective =
                (contentMode == SidebarMode.GLOBAL) ? globalObjective : objectives.get(player.getUniqueId());
        Integer score = objective.getScores().get(oldLine);

        if (score == null) {
            return;  // Nothing to update.
        }

        // Then we updates the line.
        updateLine(objective, oldLine, newLine, score);
    }

    /**
     * Updates a single line of the scoreboard, replacing the line at the given index in the list
     * returned by {@link #getContent(Player)} with the new one.
     * <p>This method is only useful if the {@link #getContent(Player)} method returns {@code null};
     * else, this change will be overwritten by the newly-generated sidebar.</p>
     * <p>If the old sidebar doesn't contains any line at the given index, nothing is done.</p>
     *
     * @param player    The updated player; {@code null} if the content's mode is {@link
     *                  SidebarMode#GLOBAL global}.
     * @param lineIndex The index of the replaced line in the list previously returned by the {@link
     *                  #getContent(Player)} method.
     * @param newLine   The new line.
     * @throws IllegalStateException    if there isn't any previously-constructed objective for that
     *                                  player (or at all, if the content's mode is {@link
     *                                  SidebarMode#GLOBAL global}).
     * @throws IllegalArgumentException if {@code player} is {@code null} and the content's mode is
     *                                  not {@link SidebarMode#GLOBAL global}.
     */
    public void updateLine(Player player, int lineIndex, String newLine) {
        if ((contentMode == SidebarMode.GLOBAL && (globalObjective == null || player == null))
                || !objectives.containsKey(player.getUniqueId())) {
            throw new IllegalArgumentException(
                    "Cannot change the sidebar for that player because either the objective is not constructed"
                            + "for this player or the player is null out of the GLOBAL content mode.");
        }
        // We need the old score.
        SidebarObjective objective =
                (contentMode == SidebarMode.GLOBAL) ? globalObjective : objectives.get(player.getUniqueId());
        Integer biggestScore = -1;

        for (Integer score : objective.getScores().values()) {
            if (score > biggestScore) {
                biggestScore = score;
            }
        }

        if (lineIndex >= biggestScore) {
            return;
        }

        Integer score = biggestScore - lineIndex;

        // Then we need the old line at this score.
        String oldLine = null;
        for (Map.Entry<String, Integer> scoreEntry : new HashMap<>(objective.getScores()).entrySet()) {
            if (scoreEntry.getValue().equals(score)) {
                oldLine = scoreEntry.getKey();
                break;
            }
        }

        if (oldLine == null) {
            return;
        }

        // Then we updates the line.
        updateLine(objective, oldLine, newLine, score);
    }

    /**
     * Sends the packets and updates the objective object to replace the old line at the given score
     * with the new line.
     *
     * @param objective The updated objective.
     * @param oldLine   The old line.
     * @param newLine   The new line.
     * @param score     The score.
     */
    private void updateLine(SidebarObjective objective, String oldLine, String newLine, int score) {
        // First: we check if there is something to do.
        // This may seems strange, but without this check, if a line is replaced by itself (no change),
        // the line will disappears (because the new line is sent before the destroy packet of the
        // old one), and even without that, this avoids useless packets to be sent.
        if (oldLine.equals(newLine)) {
            return;
        }

        // We send the line change packets
        ObjectiveSender.updateLine(objective, oldLine, newLine, score);

        // ...and we update the objective.
        objective.removeScore(oldLine);
        objective.setScore(newLine, score);
    }

    /**
     * Constructs an objective ready to be sent, from the raw data.
     *
     * @param title     The sidebar's title.
     * @param content   The sidebar's content.
     * @param receivers The receivers of this objective.
     * @return The objective.
     */
    private SidebarObjective constructObjective(final String title, final List<String> content, Set<UUID> receivers) {
        SidebarObjective objective = new SidebarObjective(title);

        // The score of the first line
        int score = lastLineScore + content.size() - 1;

        // The deduplication stuff
        Set<String> usedLines = new HashSet<>();

        // The current number of spaces used to create blank lines
        int spacesInBlankLines = 0;

        for (String line : content) {
            // The blank lines are always deduplicated
            if (line.isEmpty()) {
                for (int i = 0; i < spacesInBlankLines; i++) {
                    line += " ";
                }

                spacesInBlankLines++;
            } else if (automaticDeduplication) {
                // If the deduplication is enabled, we add spaces until the line is unique.
                String rawLine = line;

                // The deduplication string used.
                // We try to use the Minecraft formatting codes. If we can't find an unique
                // string with the first one, we try with the next, and so one.
                // This is not used for empty line because a sidebar with 40 empty lines is
                // not a well-designed sidebar, as the sidebar can't display this amount of
                // lines. If this is a problem for you, fill a bug report.
                Character deduplicationChar = '0';

                while (usedLines.contains(line)) {
                    if (line.length() + 2 > SidebarObjective.MAX_LENGTH_SCORE_NAME && deduplicationChar != null) {
                        deduplicationChar++;

                        if (deduplicationChar > 'f') {
                            deduplicationChar = null;
                        } else if (deduplicationChar > '9') {
                            deduplicationChar = 'a';
                        }

                        line = rawLine;
                    }

                    line += deduplicationChar != null ? ChatColor.COLOR_CHAR + "" + deduplicationChar : " ";
                }

                usedLines.add(line);
            }

            objective.setScore(line, score);
            score--;
        }

        for (UUID receiver : receivers) {
            objective.addReceiver(receiver);
        }

        return objective;
    }
}
