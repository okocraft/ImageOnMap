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

package fr.zcraft.quartzlib.tools;

import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.runners.RunAsyncTask;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import fr.zcraft.quartzlib.tools.text.MessageSender;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.scheduler.BukkitTask;

public final class UpdateChecker implements Listener {
    private static final Set<UUID> notificationSentTo = new HashSet<>();
    private static URI resourceURI;
    private static String newVersion = null;

    private static ConsoleNotificationSender consoleNotificationSender;
    private static PlayerNotificationSender playerNotificationSender;
    private static PlayerNotificationFilter playerNotificationFilter;

    private static BukkitTask checkTask = null;

    private UpdateChecker() {
    }

    /**
     * Boots up the update checker.
     * <p>When this method is called, updates will be checked asynchronously in the
     * background every six hours. To use this class, your plugin MUST be on Spigot,
     * and the version configured on Spigot MUST match the version in the plugin.yml
     * exactly (case-insensitive).</p>
     * <p>To get your Spigot identifier, go to your plugin page, and look at the
     * URL. It will be like {@code https://www.spigotmc.org/resources/resource-name.123456/}.</p>
     * <p>The Spigot identifier is the last part of the URL, here {@code resource-name.123456}.</p>
     * <p>In this version, notifications are sent to all operators, and you are provided a default
     * notification style.</p>
     *
     * @param spigotIdentifier The Spigot identifier of your plugin, as explained above.
     * @see #boot(String, ConsoleNotificationSender, PlayerNotificationSender) to configure the notifications.
     * @see #boot(String, ConsoleNotificationSender, PlayerNotificationSender, PlayerNotificationFilter)
     *      to configure everything.
     */
    public static void boot(final String spigotIdentifier) {
        boot(spigotIdentifier, null, null);
    }

    /**
     * Boots up the update checker.
     * <p>When this method is called, updates will be checked asynchronously in the
     * background every six hours. To use this class, your plugin MUST be on Spigot,
     * and the version configured on Spigot MUST match the version in the plugin.yml
     * exactly (case-insensitive).</p>
     * <p>To get your Spigot identifier, go to your plugin page, and look at the
     * URL. It will be like {@code https://www.spigotmc.org/resources/resource-name.123456/}.</p>
     * <p>The Spigot identifier is the last part of the URL, here {@code resource-name.123456}.</p>
     * <p>In this version, notifications are sent to all operators.</p>
     *
     * @param spigotIdentifier      The Spigot identifier of your plugin, as explained above.
     * @param onUpdateSentToConsole The task to execute when an update notification should
     *                              be sent to the console.
     * @param onUpdateSentToPlayer  The task to execute when an update notification should
     *                              be sent to a player.
     * @see #boot(String, ConsoleNotificationSender, PlayerNotificationSender, PlayerNotificationFilter)
     *      to configure the filter too.
     * @see #boot(String) for a simple version with default notifications that should be OK for everyone.
     */
    public static void boot(final String spigotIdentifier, final ConsoleNotificationSender onUpdateSentToConsole,
                            final PlayerNotificationSender onUpdateSentToPlayer) {
        boot(spigotIdentifier, onUpdateSentToConsole, onUpdateSentToPlayer, null);
    }

    /**
     * Boots up the update checker.
     * <p>When this method is called, updates will be checked asynchronously in the
     * background every six hours. To use this class, your plugin MUST be on Spigot,
     * and the version configured on Spigot MUST match the version in the plugin.yml
     * exactly (case-insensitive).</p>
     * <p>To get your Spigot identifier, go to your plugin page, and look at the
     * URL. It will be like {@code https://www.spigotmc.org/resources/resource-name.123456/}.</p>
     * <p>The Spigot identifier is the last part of the URL, here {@code resource-name.123456}.</p>
     *
     * @param spigotIdentifier      The Spigot identifier of your plugin, as explained above.
     * @param onUpdateSentToConsole The task to execute when an update notification should
     *                              be sent to the console.
     * @param onUpdateSentToPlayer  The task to execute when an update notification should
     *                              be sent to a player.
     * @param filter                A test to check if a player should receive the notification.
     */
    public static void boot(final String spigotIdentifier, final ConsoleNotificationSender onUpdateSentToConsole,
                            final PlayerNotificationSender onUpdateSentToPlayer,
                            final PlayerNotificationFilter filter) {
        consoleNotificationSender =
                onUpdateSentToConsole != null ? consoleNotificationSender : getDefaultConsoleNotificationSender();
        playerNotificationFilter = filter != null ? filter : ServerOperator::isOp;
        playerNotificationSender =
                onUpdateSentToPlayer != null ? onUpdateSentToPlayer : getDefaultPlayerNotificationSender();

        final List<String> identifierParts = Arrays.asList(spigotIdentifier.split("\\."));

        final int resourceID = Integer.parseInt(identifierParts.get(identifierParts.size() - 1));
        final URL checkUri;

        try {
            resourceURI = new URI("https://www.spigotmc.org/resources/" + spigotIdentifier);
        } catch (URISyntaxException e) {
            PluginLogger.error("Unable to boot update checker: invalid resource URI", e);
            return;
        }

        try {
            checkUri = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceID);
        } catch (MalformedURLException e) {
            PluginLogger.error("Unable to boot update checker: invalid resource URI", e);
            return;
        }

        checkTask = RunAsyncTask.timer(() -> {
            try (final InputStream inputStream = checkUri.openStream(); Scanner scanner = new Scanner(inputStream)) {
                if (scanner.hasNext()) {
                    final String version = scanner.next().trim();

                    // If there is an update, we notify the console and the online allowed players.
                    // Then, we register the events in this class to notify other allowed players when they
                    // log in.
                    // Finally, we stop the check task, as there is no point in checking again and again.
                    if (!version.equalsIgnoreCase(QuartzLib.getPlugin().getDescription().getVersion().trim())) {
                        newVersion = version;

                        // To send the notifications, we go back to the main thread.
                        RunTask.nextTick(() -> {
                            consoleNotificationSender.send(version, resourceURI);

                            for (final Player player : Bukkit.getOnlinePlayers()) {
                                if (playerNotificationFilter.check(player)) {
                                    playerNotificationSender.send(version, resourceURI, player);
                                    notificationSentTo.add(player.getUniqueId());
                                }
                            }
                        });

                        QuartzLib.registerEvents(new UpdateChecker());

                        checkTask.cancel();
                        checkTask = null;
                    }
                }
            } catch (IOException ignored) {
            }
        }, 40L, 20 * 3600 * 2L);
    }

    private static ConsoleNotificationSender getDefaultConsoleNotificationSender() {
        return (version, link) -> {
            PluginLogger.warning("A new version of " + QuartzLib.getPlugin().getDescription().getName()
                    + " is available! Latest version is " + version + ", and you're running "
                    + QuartzLib.getPlugin().getDescription().getVersion());
            PluginLogger.warning("Download the new version here: " + link);
        };
    }

    private static PlayerNotificationSender getDefaultPlayerNotificationSender() {
        return (version, link, player) -> {
            final RawText hover = new RawText()
                    .then(I.t("Click here to open"))
                    .then("\n")
                    .then(link.toString().replaceFirst("https://", "").replaceFirst("www\\.", "")).color(ChatColor.GRAY)
                    .build();

            MessageSender.sendSystemMessage(player, "");
            MessageSender.sendSystemMessage(player, new RawText()
                    .uri(link)
                    .hover(hover)
                    .then("✦ ")
                    .color(ChatColor.GOLD)
                    .then(I.t("{0} {1} is available!", QuartzLib.getPlugin().getDescription().getName(), version))
                    .color(ChatColor.GOLD)
                    .style(ChatColor.BOLD)
                    .build()
            );
            MessageSender.sendSystemMessage(player, new RawText()
                    .then(I.t("You're still running {0}. Click here to update.",
                            QuartzLib.getPlugin().getDescription().getVersion()))
                    .color(ChatColor.YELLOW)
                    .uri(link)
                    .hover(hover)
                    .build()
            );
        };
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerJoin(final PlayerJoinEvent ev) {
        final Player player = ev.getPlayer();

        if (newVersion != null && !notificationSentTo.contains(player.getUniqueId())
                && playerNotificationFilter.check(player)) {
            RunTask.later(() -> {
                if (player.isOnline()) {
                    playerNotificationSender.send(newVersion, resourceURI, player);
                    notificationSentTo.add(player.getUniqueId());
                }
            }, 160L);
        }
    }

    public interface ConsoleNotificationSender {
        /**
         * Called when an update notification is sent to the console, to send this update notification.
         *
         * @param version The new version available on Spigot.
         * @param link    The link to the plugin's resource page.
         */
        void send(final String version, final URI link);
    }

    public interface PlayerNotificationSender {
        /**
         * Called when an update notification is sent to a player, to send this update notification.
         * <p>Permissions and such has already be checked, you only have to send the message.</p>
         *
         * @param version The new version available on Spigot.
         * @param link    The link to the plugin's resource page.
         * @param player  The player this notification is sent to.
         */
        void send(final String version, final URI link, final Player player);
    }

    public interface PlayerNotificationFilter {
        /**
         * Checks if a player should get the update notification.
         *
         * @param player The player.
         * @return {@code true} if the player should be notified about the update.
         */
        boolean check(final Player player);
    }
}
