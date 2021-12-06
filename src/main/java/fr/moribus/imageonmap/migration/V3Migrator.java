/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
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
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap.migration;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.mojang.UUIDFetcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * This class represents and executes the ImageOnMap v3.x migration process
 */
public class V3Migrator implements Runnable {

    /**
     * The name of the former file that contained all the maps definitions (including posters)
     */
    private static final String OLD_MAPS_FILE_NAME = "map.yml";

    /**
     * The name of the former file that contained all the posters definitions
     */
    private static final String OLD_POSTERS_FILE_NAME = "poster.yml";

    /**
     * The plugin that is running the migration
     */
    private final ImageOnMap plugin;
    /**
     * The backup directory that will contain the pre-v3 files that
     * were present before the migration started
     */
    private final Path backupsPrev3Directory;
    /**
     * The backup directory that will contain the post-v3 files that
     * were present before the migration started
     */
    private final Path backupsPostv3Directory;
    /**
     * The list of all the posters to migrate
     */
    private final ArrayDeque<OldSavedPoster> postersToMigrate;
    /**
     * The list of all the single maps to migrate
     */
    private final ArrayDeque<OldSavedMap> mapsToMigrate;
    /**
     * The set of all the user names to retreive the UUID from Mojang
     */
    private final HashSet<String> userNamesToFetch;
    /**
     * The former file that contained all the posters definitions
     */
    private Path oldPostersFile;
    /**
     * The former file that contained all the maps definitions (including posters)
     */
    private Path oldMapsFile;
    /**
     * The map of all the usernames and their corresponding UUIDs
     */
    private Map<String, UUID> usersUUIDs;

    public V3Migrator(ImageOnMap plugin) {
        this.plugin = plugin;

        Path dataFolder = plugin.getDataFolder().toPath();

        oldPostersFile = dataFolder.resolve("poster.yml");
        oldMapsFile = dataFolder.resolve("map.yml");

        backupsPrev3Directory = dataFolder.resolve("backups_pre-v3");
        backupsPostv3Directory = dataFolder.resolve("backups_post-v3");

        postersToMigrate = new ArrayDeque<>();
        mapsToMigrate = new ArrayDeque<>();
        userNamesToFetch = new HashSet<>();
    }

    /**
     * Makes a standard file copy, and checks the integrity of the destination
     * file after the copy
     *
     * @param sourceFile      The file to copy
     * @param destinationFile The destination file
     * @throws IOException If the copy failed, if the integrity check failed, or if the destination file already exists
     */
    private static void verifiedBackupCopy(Path sourceFile, Path destinationFile) throws IOException {
        if (Files.isRegularFile(destinationFile)) {
            throw new IOException(
                    "Backup copy failed : destination file (" + destinationFile + ") already exists.");
        }

        Files.copy(sourceFile, destinationFile, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Executes the full migration
     */
    private void migrate() {
        try {
            if (!spotFilesToMigrate()) {
                return;
            }
            if (checkForExistingBackups()) {
                return;
            }
            if (!loadOldFiles()) {
                return;
            }
            backupMapData();
            fetchUUIDs();
            if (!fetchMissingUUIDs()) {
                return;
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("Error while preparing migration"));
            plugin.getLogger().log(Level.SEVERE, I.t("Aborting migration. No change has been made."), ex);
            return;
        }

        try {
            mergeMapData();
            saveChanges();
            cleanup();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("Error while migrating"), ex);
            plugin.getLogger().log(Level.SEVERE, I.t("Aborting migration. Some changes may already have been made."));
            plugin.getLogger().log(Level.SEVERE, I.t(
                    "Before trying to migrate again, you must recover player files from the backups,"
                            + " and then move the backups away from the plugin directory to avoid overwriting them."));
        }
    }

    /**
     * Checks if there is any of the former files to be migrated
     *
     * @return true if any former map or poster file exists, false otherwise
     */
    private boolean spotFilesToMigrate() {
        plugin.getLogger().info(I.t("Looking for configuration files to migrate..."));

        if (!Files.isRegularFile(oldPostersFile)) {
            oldPostersFile = null;
        } else {
            plugin.getLogger().info(I.t("Detected former posters file {0}", OLD_POSTERS_FILE_NAME));
        }

        if (!Files.isRegularFile(oldMapsFile)) {
            oldMapsFile = null;
        } else {
            plugin.getLogger().info(I.t("Detected former maps file {0}", OLD_MAPS_FILE_NAME));
        }

        if (oldPostersFile == null && oldMapsFile == null) {
            plugin.getLogger().info(I.t("There is nothing to migrate. Stopping."));
            return false;
        } else {
            plugin.getLogger().info(I.t("Done."));
            return true;
        }
    }

    /**
     * Checks if any existing backup directories exists
     *
     * @return true if a non-empty backup directory exists, false otherwise
     */
    @SuppressWarnings("ConstantConditions")
    private boolean checkForExistingBackups() {
        if ((Files.isDirectory(backupsPrev3Directory) && backupsPrev3Directory.toFile().list().length == 0)
                || (Files.isDirectory(backupsPostv3Directory) && backupsPrev3Directory.toFile().list().length == 0)) {
            plugin.getLogger().log(Level.SEVERE, I.t("Backup directories already exists."));
            plugin.getLogger().log(Level.SEVERE, I.t("This means that a migration has already been done,"
                    + " or may not have ended well."));
            plugin.getLogger().log(Level.SEVERE, I.t(
                    "To start a new migration,"
                            + " you must move away the backup directories so they are not overwritten."));

            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates backups of the former map files, and of the existing map stores
     *
     * @throws IOException
     **/
    private void backupMapData() throws IOException {
        plugin.getLogger().info(I.t("Backing up map data before migrating..."));

        if (!Files.isDirectory(backupsPrev3Directory)) {
            Files.createDirectories(backupsPrev3Directory);
        }

        if (!Files.isDirectory(backupsPostv3Directory)) {
            Files.createDirectories(backupsPostv3Directory);
        }

        if (oldMapsFile != null && Files.isRegularFile(oldMapsFile)) {
            Path oldMapsFileBackup = backupsPrev3Directory.resolve(oldMapsFile);
            verifiedBackupCopy(oldMapsFile, oldMapsFileBackup);
        }

        if (oldPostersFile != null && Files.isRegularFile(oldPostersFile)) {
            Path oldPostersFileBackup = backupsPrev3Directory.resolve(oldPostersFile);
            verifiedBackupCopy(oldPostersFile, oldPostersFileBackup);
        }

        Files.list(plugin.getMapsDirectory()).forEach(mapFile -> {
            var backupFile = backupsPostv3Directory.resolve(mapFile);
            try {
                verifiedBackupCopy(mapFile, backupFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        plugin.getLogger().info(I.t("Backup complete."));
    }

    /**
     * An utility function to check if a map is actually part of a loaded poster
     *
     * @param map The single map.
     * @return true if the map is part of a poster, false otherwise
     */
    private boolean posterContains(OldSavedMap map) {
        for (OldSavedPoster poster : postersToMigrate) {
            if (poster.contains(map)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Loads the former files into the corresponding arrays
     * Also fetches the names of all the users that have maps
     *
     * @return true if any of the files contained readable map data, false otherwise
     */
    private boolean loadOldFiles() {
        if (oldPostersFile != null) {
            FileConfiguration oldPosters = YamlConfiguration.loadConfiguration(oldPostersFile.toFile());

            OldSavedPoster oldPoster;
            for (String key : oldPosters.getKeys(false)) {
                if ("IdCount".equals(key)) {
                    continue;
                }
                try {
                    oldPoster = new OldSavedPoster(oldPosters.get(key), key);
                    postersToMigrate.add(oldPoster);
                    userNamesToFetch.add(oldPoster.getUserName());
                } catch (InvalidConfigurationException ex) {
                    plugin.getLogger().log(Level.WARNING, "Could not read poster data for key " + key, ex);
                }
            }
        }

        if (oldMapsFile != null) {
            FileConfiguration oldMaps = YamlConfiguration.loadConfiguration(oldMapsFile.toFile());
            OldSavedMap oldMap;

            for (String key : oldMaps.getKeys(false)) {
                try {
                    if ("IdCount".equals(key)) {
                        continue;
                    }
                    oldMap = new OldSavedMap(oldMaps.get(key));

                    if (!posterContains(oldMap)) {
                        mapsToMigrate.add(oldMap);
                    }

                    userNamesToFetch.add(oldMap.getUserName());
                } catch (InvalidConfigurationException ex) {
                    plugin.getLogger().log(Level.WARNING, "Could not read poster data for key '" + key + "'", ex);
                }
            }
        }

        return (postersToMigrate.size() > 0) || (mapsToMigrate.size() > 0);
    }

    /**
     * Fetches all the needed UUIDs from Mojang's UUID conversion service
     *
     * @throws IOException          if the fetcher could not connect to Mojang's servers
     * @throws InterruptedException if the thread was interrupted while fetching UUIDs
     */
    private void fetchUUIDs() throws IOException, InterruptedException {
        plugin.getLogger().info(I.t("Fetching UUIDs from Mojang..."));
        try {
            usersUUIDs = UUIDFetcher.fetch(new ArrayList<>(userNamesToFetch));
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("An error occurred while fetching the UUIDs from Mojang"), ex);
            throw ex;
        } catch (InterruptedException ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("The migration worker has been interrupted"), ex);
            throw ex;
        }
        plugin.getLogger().info(I.tn("Fetching done. {0} UUID have been retrieved.",
                "Fetching done. {0} UUIDs have been retrieved.", usersUUIDs.size()));
    }

    /**
     * Fetches the UUIDs that could not be retrieved via Mojang's standard API
     *
     * @return true if at least one UUID has been retrieved, false otherwise
     */
    private boolean fetchMissingUUIDs() throws IOException, InterruptedException {
        if (usersUUIDs.size() == userNamesToFetch.size()) {
            return true;
        }
        int remainingUsersCount = userNamesToFetch.size() - usersUUIDs.size();
        plugin.getLogger().info(I.tn("Mojang did not find UUIDs for {0} player at the current time.",
                "Mojang did not find UUIDs for {0} players at the current time.", remainingUsersCount));
        plugin.getLogger().info(I.t("The Mojang servers limit requests rate at one per second, this may take some time..."));

        try {
            UUIDFetcher.fetchRemaining(userNamesToFetch, usersUUIDs);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("An error occurred while fetching the UUIDs from Mojang"));
            throw ex;
        } catch (InterruptedException ex) {
            plugin.getLogger().log(Level.SEVERE, I.t("The migration worker has been interrupted"));
            throw ex;
        }

        if (usersUUIDs.size() != userNamesToFetch.size()) {
            plugin.getLogger().log(Level.WARNING, I.tn("Mojang did not find player data for {0} player",
                    "Mojang did not find player data for {0} players",
                    userNamesToFetch.size() - usersUUIDs.size()));
            plugin.getLogger().log(Level.WARNING, I.t("The following players do not exist or do not have paid accounts :"));

            String missingUsersList = "";

            for (String user : userNamesToFetch) {
                if (!usersUUIDs.containsKey(user)) {
                    missingUsersList += user + ", ";
                }
            }

            plugin.getLogger().info(missingUsersList);
        }

        if (usersUUIDs.size() <= 0) {
            plugin.getLogger().info(I.t("Mojang could not find any of the registered players."));
            plugin.getLogger().info(I.t("There is nothing to migrate. Stopping."));
            return false;
        }

        return true;
    }

    private void mergeMapData() {
        plugin.getLogger().info(I.t("Merging map data..."));

        ArrayDeque<OldSavedMap> remainingMaps = new ArrayDeque<>();
        ArrayDeque<OldSavedPoster> remainingPosters = new ArrayDeque<>();

        ArrayDeque<Integer> missingMapIds = new ArrayDeque<>();

        UUID playerUUID;
        OldSavedMap map;
        while (!mapsToMigrate.isEmpty()) {
            map = mapsToMigrate.pop();
            playerUUID = usersUUIDs.get(map.getUserName());
            if (playerUUID == null) {
                remainingMaps.add(map);
            } else if (!map.isMapValid()) {
                missingMapIds.add((int) map.getMapId());
            } else {
                MapManager.insertMap(map.toImageMap(playerUUID));
            }
        }
        mapsToMigrate.addAll(remainingMaps);

        OldSavedPoster poster;
        while (!postersToMigrate.isEmpty()) {
            poster = postersToMigrate.pop();
            playerUUID = usersUUIDs.get(poster.getUserName());
            if (playerUUID == null) {
                remainingPosters.add(poster);
            } else if (!poster.isMapValid()) {
                missingMapIds.addAll(Arrays.stream(ArrayUtils.toObject(poster.getMapsIds())).map(id -> (int) id).toList());
            } else {
                MapManager.insertMap(poster.toImageMap(playerUUID));
            }
        }
        postersToMigrate.addAll(remainingPosters);

        if (!missingMapIds.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, I.tn("{0} registered minecraft map is missing from the save.",
                    "{0} registered minecraft maps are missing from the save.", missingMapIds.size()));
            plugin.getLogger().log(Level.WARNING, 
                    I.t("These maps will not be migrated,"
                            + " but this could mean the save has been altered or corrupted."));
            plugin.getLogger().log(Level.WARNING, I.t("The following maps are missing : {0} ",
                    StringUtils.join(missingMapIds, ',')));
        }
    }

    /* ****** Utils ***** */

    private void saveChanges() {
        plugin.getLogger().info(I.t("Saving changes..."));
        MapManager.save();
    }

    private void cleanup() throws IOException {
        plugin.getLogger().info(I.t("Cleaning up old data files..."));

        //Cleaning maps file
        if (oldMapsFile != null) {
            if (mapsToMigrate.isEmpty()) {
                plugin.getLogger().info(I.t("Deleting old map data file..."));
                Files.delete(oldMapsFile);
            } else {
                plugin.getLogger().info(I.tn("{0} map could not be migrated.", "{0} maps could not be migrated.",
                        mapsToMigrate.size()));
                YamlConfiguration mapConfig = new YamlConfiguration();
                mapConfig.set("IdCount", mapsToMigrate.size());

                for (OldSavedMap map : mapsToMigrate) {
                    map.serialize(mapConfig);
                }

                mapConfig.save(oldMapsFile.toFile());
            }
        }

        //Cleaning posters file
        if (oldPostersFile != null) {
            if (postersToMigrate.isEmpty()) {
                plugin.getLogger().info(I.t("Deleting old poster data file..."));
                Files.delete(oldPostersFile);
            } else {
                plugin.getLogger().info(I.tn("{0} poster could not be migrated.", "{0} posters could not be migrated.",
                        postersToMigrate.size()));
                YamlConfiguration posterConfig = new YamlConfiguration();
                posterConfig.set("IdCount", postersToMigrate.size());

                for (OldSavedPoster poster : postersToMigrate) {
                    poster.serialize(posterConfig);
                }

                posterConfig.save(oldPostersFile.toFile());
            }
        }

        plugin.getLogger().info(I.t("Data that has not been migrated will be kept in the old data files."));
    }

    /**
     * Executes the full migration, and defines the running status of the migration
     */
    @Override
    public void run() {
        migrate();
    }

}
