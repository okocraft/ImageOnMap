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

package fr.zcraft.quartzlib.components.gui;

import fr.zcraft.quartzlib.tools.Callback;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;


public class PromptGui extends GuiBase {
    private static final int SIGN_LINES_COUNT = 4;
    private static final int SIGN_COLUMNS_COUNT = 15;

    private static boolean isInitialized = false;

    /* ===== Reflection to Sign API ===== */
    private static Field fieldTileEntitySign = null; // 1.11.2-: CraftSign.sign; 1.12+: CraftBlockEntityState.tileEntity
    private static Field fieldTileEntitySignEditable = null; // 1.12+ only: CraftBlockEntityState.isEditable
    private static Method methodGetHandle = null; // CraftPlayer.getHandle()
    private static Method methodOpenSign = null; // EntityHuman.openSign()
    private final Callback<String> callback;
    private Location signLocation;
    private String contents;

    public PromptGui(Callback<String> callback, String contents) {
        this(callback);
        this.contents = contents;
    }

    /**
     * Creates a new prompt GUI, using the given callback.
     *
     * @param callback The callback to be given the input text to.
     */
    public PromptGui(Callback<String> callback) {
        super();
        if (!isAvailable()) {
            throw new IllegalStateException("Sign-based prompt GUI are not available");
        }

        this.callback = callback;
    }

    /**
     * Checks if Prompt GUIs can be correctly used on this Minecraft versions.
     */
    public static boolean isAvailable() {
        if (!isInitialized) {
            init();
        }
        return fieldTileEntitySign != null;
    }

    public static void prompt(Player owner, Callback<String> callback, String contents, GuiBase parent) {
        Gui.open(owner, new PromptGui(callback, contents), parent);
    }

    private static void init() {
        isInitialized = true;

        try {
            final Class<?> CraftBlockEntityState =
                    Reflection.getBukkitClassByName("block.CraftBlockEntityState");
            final Class<?> CraftSign =
                    Reflection.getBukkitClassByName("block.CraftSign");
            final Class<?> classTileEntitySign =
                    Reflection.getMinecraftClassByName("world.level.block.entity.TileEntitySign");
            final Class<?> CraftPlayer =
                    Reflection.getBukkitClassByName("entity.CraftPlayer");
            final Class<?> EntityHuman =
                    Reflection.getMinecraftClassByName("world.entity.player.EntityHuman");

            try {
                fieldTileEntitySign = Reflection.getField(CraftSign, "sign");
            } catch (NoSuchFieldException e) { // 1.12+
                fieldTileEntitySign = Reflection.getField(CraftBlockEntityState, "tileEntity");
            }

            try {
                //1.17+
                fieldTileEntitySignEditable = Reflection.getField(classTileEntitySign, "isEditable");
            } catch (NoSuchFieldException e) {
                try {
                    // PluginLogger.info("before field");
                    fieldTileEntitySignEditable = Reflection.getField(classTileEntitySign, "isEditable");

                } catch (NoSuchFieldException ex) { // 1.11.2 or below
                    fieldTileEntitySignEditable = null;
                }
            }
            methodGetHandle = CraftPlayer.getDeclaredMethod("getHandle");
            methodOpenSign = EntityHuman.getDeclaredMethod("openSign", classTileEntitySign);
        } catch (Exception e) {
            PluginLogger.error("Unable to initialize Sign Prompt API", e);
            fieldTileEntitySign = null;
        }
    }

    private static String getSignContents(String[] lines) {
        StringBuilder content = new StringBuilder(lines[0].trim());

        for (int i = 1; i < lines.length; i++) {
            if (lines[i] == null || lines[i].isEmpty()) {
                continue;
            }
            content.append(" ").append(lines[i].trim());
        }

        return content.toString().trim();
    }

    private static void setSignContents(Sign sign, String content) {
        String[] lines = new String[SIGN_LINES_COUNT + 1];
        String curLine;
        int curLineIndex = 0;
        int spacePos;

        if (content != null) {
            lines[0] = content;
            while (curLineIndex < SIGN_LINES_COUNT) {
                curLine = lines[curLineIndex];
                if (curLine.length() <= SIGN_COLUMNS_COUNT) {
                    break;
                }

                spacePos = curLine.lastIndexOf(' ', SIGN_COLUMNS_COUNT);
                if (spacePos < 0) {
                    break;
                }
                lines[curLineIndex + 1] = curLine.substring(spacePos + 1);
                lines[curLineIndex] = curLine.substring(0, spacePos);
                curLineIndex++;
            }
        }

        for (int i = SIGN_LINES_COUNT; i-- > 0; ) {
            sign.setLine(i, lines[i]);
        }
    }

    private static Location findAvailableLocation(Player player) {
        World world = player.getWorld();
        Chunk playerChunk = player.getLocation().getChunk();
        Chunk firstChunk = world.getChunkAt(playerChunk.getX() - 1, playerChunk.getZ() - 1);
        Location firstLoc = firstChunk.getBlock(0, 255, 0).getLocation();
        Location loc;

        for (int i = 48; i-- > 0; ) {
            for (int j = 0; j-- > -10; ) {
                for (int k = 48; k-- > 0; ) {
                    loc = firstLoc.add(i, j, k);
                    if (hasSpace(world, loc)) {
                        return loc;
                    }
                }
            }
        }

        return null;
    }

    private static boolean hasSpace(World world, Location loc) {
        if (!Material.AIR.equals(world.getBlockAt(loc).getType())) {
            return false;
        }

        for (int i = 1; i-- > -1; ) {
            for (int j = 1; j-- > -2; ) {
                for (int k = 1; k-- > -1; ) {
                    if (!Material.AIR.equals(world.getBlockAt(
                            loc.getBlockX() + i, loc.getBlockY() + j, loc.getBlockZ() + k)
                            .getType())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    protected void open(final Player player) {
        // PluginLogger.info("\n\nopen");
        super.open(player);

        signLocation = findAvailableLocation(player);

        if (signLocation == null) {
            throw new RuntimeException("Too many players are using a PromptGui at the same time."
                    + " (…wait, the limit is about 7500, how did you do that??)");
        }

        // Ugly workaround for spigot still applying physics in spigot 1.9.2+
        signLocation.getWorld().getBlockAt(signLocation.clone().add(0, -1, 0)).setType(Material.GLASS);

        final Block block = signLocation.getWorld().getBlockAt(signLocation);

        block.setType(Material.OAK_SIGN, false);

        final Sign sign = (Sign) block.getState();

        // PluginLogger.info("before setting sign content");
        setSignContents(sign, contents);
        // PluginLogger.info("before sign updtae");
        sign.update();
        // PluginLogger.info("after sign update");

        // PluginLogger.info("isEditable " + sign.isEditable());

        RunTask.later(() -> {
            try {
                // PluginLogger.info("task run later");
                final Object signTileEntity = fieldTileEntitySign.get(sign);
                final Object playerEntity = methodGetHandle.invoke(player);
                // PluginLogger.info("test");
                // In Minecraft 1.12+, there's a lock on the signs to avoid them
                // to be edited after they are loaded into the game.
                if (fieldTileEntitySignEditable != null) {
                    // PluginLogger.info("here?");
                    fieldTileEntitySignEditable.set(signTileEntity, true);
                    // PluginLogger.info("there?");
                }
                // PluginLogger.info("invoke");
                methodOpenSign.invoke(playerEntity, signTileEntity);
                // PluginLogger.info("invoked");
            } catch (final Throwable e) {
                PluginLogger.error("Error while opening Sign prompt", e);
            }
        }, 3);
    }

    private void validate(String[] lines) {
        callback.call(getSignContents(lines));

        // Bukkit sends extra InventoryCloseEvents when closing a sign GUI...
        this.close(true);
    }

    @Override
    protected Listener getEventListener() {
        return new PromptGuiListener();
    }

    private final class PromptGuiListener implements Listener {
        @EventHandler
        public void onSignChange(SignChangeEvent event) {
            if (event.getPlayer() != getPlayer()) {
                return;
            }
            validate(event.getLines());
        }
    }
}
