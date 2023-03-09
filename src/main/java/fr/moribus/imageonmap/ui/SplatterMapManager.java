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

package fr.moribus.imageonmap.ui;

import fr.moribus.imageonmap.gui.GuiUtils;
import fr.moribus.imageonmap.i18n.I;
import fr.moribus.imageonmap.image.MapInitEvent;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import fr.zcraft.quartzlib.tools.world.FlatLocation;
import fr.zcraft.quartzlib.tools.world.WorldUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;


public abstract class SplatterMapManager {

    private static final NamespacedKey SPLATTER_KEY = new NamespacedKey("imageonmap", "splatter");

    private SplatterMapManager() {
    }

    @SuppressWarnings("deprecation")
    public static ItemStack makeSplatterMap(PosterMap map) {

        ItemStack splatter = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) splatter.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + map.getName() + ChatColor.DARK_GRAY + " - " + I.t("Splatter Map")
                + ChatColor.DARK_GRAY + " - " + I.t("{0} × {1}", map.getColumnCount(), map.getRowCount()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + map.getId());
        lore.add("");
        lore.add(ChatColor.BLUE + I.t("Item frames needed"));
        lore.add(ChatColor.GRAY + I.t("{0} × {1} (total {2} frames)", map.getColumnCount(), map.getRowCount(),
                map.getColumnCount() * map.getRowCount()));
        lore.add("");
        lore.add(ChatColor.BLUE + I.t("How to use this?"));
        lore.addAll(GuiUtils
                .generateLore(ChatColor.GRAY + I.t("Place empty item frames on a wall, enough to host the whole map."
                        + " Then, right-click on the bottom-left frame with this map."), 40));
        lore.add("");
        lore.addAll(GuiUtils.generateLore(
                ChatColor.GRAY + I.t("Shift-click one of the placed maps to remove the whole poster in one shot."),
                40));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        meta.setMapId(map.getMapIdAt(0));
        meta.setColor(Color.GREEN);
        splatter.setItemMeta(meta);

        return addSplatterAttribute(splatter);
    }

    /**
     * To identify image on maps for the auto-splattering to work, we mark the
     * items using an enchantment maps are not supposed to have (Mending).
     *
     * <p>
     * Then we check if the map is enchanted at all to know if it's a splatter
     * map. This ensure compatibility with old splatter maps from 3.x, where
     * zLib's glow effect was used.
     * </p>
     * An AttributeModifier (using zLib's attributes system) is not used,
     * because Minecraft (or Spigot) removes them from maps in 1.14+, so that
     * wasn't stable enough (and the glowing effect of enchantments is
     * prettier).
     *
     * @param itemStack The item stack to mark as a splatter map.
     * @return The modified item stack. The instance may be different if the passed item stack is not a craft itemstack.
     */
    public static ItemStack addSplatterAttribute(final ItemStack itemStack) {
        var meta = itemStack.getItemMeta();

        meta.getPersistentDataContainer().set(SPLATTER_KEY, PersistentDataType.BYTE, (byte) 1);

        meta.addEnchant(Enchantment.DURABILITY, 1, false);

        itemStack.setItemMeta(meta);

        return itemStack;
    }

    /**
     * Checks if an item have the splatter attribute set (i.e. if the item is
     * enchanted in any way).
     *
     * @param itemStack The item to check.
     * @return True if the attribute was detected.
     */
    public static boolean hasSplatterAttributes(ItemStack itemStack) {
        var value = itemStack.getItemMeta().getPersistentDataContainer().getOrDefault(SPLATTER_KEY, PersistentDataType.BYTE, (byte) 0);
        return value == 1;
    }

    /**
     * Return true if it is a splatter map
     *
     * @param itemStack The item to check.
     * @return True if is a splatter map
     */
    public static boolean isSplatterMap(ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }
        return hasSplatterAttributes(itemStack) && MapManager.managesMap(itemStack);
    }


    /**
     * Return true if it has a specified splatter map
     *
     * @param player The player to check.
     * @param map    The map to check.
     * @return True if the player has this map
     */
    public static boolean hasSplatterMap(Player player, PosterMap map) {
        Inventory playerInventory = player.getInventory();

        for (int i = 0; i < playerInventory.getSize(); ++i) {
            ItemStack item = playerInventory.getItem(i);
            if (isSplatterMap(item) && map.managesMap(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Place a splatter map
     *
     * @param startFrame Frame clicked by the player
     * @param player     Player placing map
     * @return true if the map was correctly placed
     */
    @SuppressWarnings("deprecation")
    public static boolean placeSplatterMap(ItemFrame startFrame, Player player) {
        ImageMap map = MapManager.getMap(player.getInventory().getItemInMainHand());

        if (!(map instanceof PosterMap poster)) {
            return false;
        }
        PosterWall wall = new PosterWall();

        if (startFrame.getFacing().equals(BlockFace.DOWN) || startFrame.getFacing().equals(BlockFace.UP)) {
            // If it is on floor or ceiling
            PosterOnASurface surface = new PosterOnASurface();
            FlatLocation startLocation = new FlatLocation(startFrame.getLocation(), startFrame.getFacing());
            FlatLocation endLocation = startLocation.clone().addH(poster.getColumnCount(), poster.getRowCount(),
                    WorldUtils.get4thOrientation(player.getLocation()));

            surface.loc1 = startLocation;
            surface.loc2 = endLocation;

            if (!surface.isValid(player)) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(I.t("{ce}There is not enough space to place this map ({0} × {1}).",
                                poster.getColumnCount(), poster.getRowCount())));

                return false;
            }

            int i = 0;
            for (ItemFrame frame : surface.frames) {
                BlockFace bf = WorldUtils.get4thOrientation(player.getLocation());
                int id = poster.getMapIdAtReverseZ(i, startFrame.getFacing());
                Rotation rot = Rotation.NONE;
                switch (frame.getFacing()) {
                    case UP:
                        break;
                    case DOWN:
                        rot = Rotation.FLIPPED;
                        break;
                    default:
                        //throw new IllegalStateException("Unexpected value: " + frame.getFacing());
                }
                //Rotation management relative to player rotation the default position is North,
                // when on ceiling we flipped the rotation
                RunTask.later(() -> {
                    ItemStack item = new ItemStack(Material.FILLED_MAP);
                    MapMeta meta = (MapMeta) item.getItemMeta();
                    meta.setMapId(id);
                    item.setItemMeta(meta);

                    frame.setItem(item);
                }, 5L);

                if (i == 0) {
                    //First map need to be rotate one time CounterClockwise
                    rot = rot.rotateCounterClockwise();
                }

                switch (bf) {
                    case NORTH -> {
                        if (frame.getFacing() == BlockFace.DOWN) {
                            rot = rot.rotateClockwise();
                            rot = rot.rotateClockwise();
                        }
                        frame.setRotation(rot);
                    }
                    case EAST -> {
                        rot = rot.rotateClockwise();
                        frame.setRotation(rot);
                    }
                    case SOUTH -> {
                        if (frame.getFacing() == BlockFace.UP) {
                            rot = rot.rotateClockwise();
                            rot = rot.rotateClockwise();
                        }
                        frame.setRotation(rot);
                    }
                    case WEST -> {
                        rot = rot.rotateCounterClockwise();
                        frame.setRotation(rot);
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + bf);
                }

                MapInitEvent.initMap(id);
                i++;
            }
        } else {
            // If it is on a wall NSEW
            FlatLocation startLocation = new FlatLocation(startFrame.getLocation(), startFrame.getFacing());
            FlatLocation endLocation = startLocation.clone().add(poster.getColumnCount(), poster.getRowCount());

            wall.loc1 = startLocation;
            wall.loc2 = endLocation;

            if (!wall.isValid()) {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(I.t("{ce}There is not enough space to place this map ({0} × {1}).",
                                poster.getColumnCount(), poster.getRowCount())));
                return false;
            }

            int i = 0;
            for (ItemFrame frame : wall.frames) {

                int id = poster.getMapIdAtReverseY(i);

                RunTask.later(() -> {
                    ItemStack item = new ItemStack(Material.FILLED_MAP);
                    MapMeta meta = (MapMeta) item.getItemMeta();
                    meta.setMapId(id);
                    item.setItemMeta(meta);

                    frame.setItem(item);
                }, 5L);


                //Force reset of rotation
                frame.setRotation(Rotation.NONE);
                MapInitEvent.initMap(id);
                ++i;
            }
        }
        return true;
    }

    /**
     * Remove splattermap
     *
     * @param startFrame Frame clicked by the player
     * @param player     The player removing the map
     * @return the {@link PosterMap}
     **/
    public static PosterMap removeSplatterMap(ItemFrame startFrame, Player player) {
        final ImageMap map = MapManager.getMap(startFrame.getItem());
        if (!(map instanceof PosterMap poster)) {
            return null;
        }
        if (!poster.hasColumnData()) {
            return null;
        }
        FlatLocation loc = new FlatLocation(startFrame.getLocation(), startFrame.getFacing());
        ItemFrame[] matchingFrames = switch (startFrame.getFacing()) {
            case UP, DOWN -> PosterOnASurface.getMatchingMapFrames(poster, loc,
                    MapManager.getMapIdFromItemStack(startFrame.getItem()),
                    WorldUtils.get4thOrientation(player.getLocation()));//startFrame.getFacing());
            case NORTH, SOUTH, EAST, WEST -> PosterWall.getMatchingMapFrames(poster, loc,
                    MapManager.getMapIdFromItemStack(startFrame.getItem()));
            default -> throw new IllegalStateException("Unexpected value: " + startFrame.getFacing());
        };

        if (matchingFrames == null) {
            return null;
        }

        PlayerInventory inv = player.getInventory();

        List<Integer> maps = Arrays.stream(poster.getMapsIDs()).boxed().collect(Collectors.toList());

        Map<Integer, Integer> invMapSlots = toMapIdSlotMap(player.getInventory());
        invMapSlots.keySet().removeIf(Predicate.not(maps::contains));

        Set<Integer> matchingFrameMapIds = Arrays.stream(matchingFrames)
                .filter(Objects::nonNull)
                .map(ItemFrame::getItem)
                .filter(MapManager::managesMap)
                .map(MapManager::getMapIdFromItemStack)
                .collect(Collectors.toSet());

        matchingFrameMapIds.addAll(invMapSlots.keySet());

        if (matchingFrameMapIds.containsAll(maps)) {
            for (ItemFrame frame : matchingFrames) {
                if (frame != null) {
                    maps.remove(Integer.valueOf(MapManager.getMapIdFromItemStack(frame.getItem())));
                    frame.setItem(null);
                }
            }

            for (int mapId : maps) {
                int invMapSlot = invMapSlots.get(mapId);
                ItemStack invMap = inv.getItem(invMapSlot);
                if (invMap == null) {
                    // will not reach.
                    continue;
                }
                invMap.setAmount(invMap.getAmount() - 1);
                inv.setItem(invMapSlot, invMap);
            }

            return poster;
        } else {
            for (ItemFrame frame : matchingFrames) {
                if (frame != null) {
                    ItemStack drop = MapItemManager.createMapItem(frame.getItem());
                    if (drop != null) {
                        frame.getWorld().dropItemNaturally(frame.getLocation(), drop);
                    }
                    frame.setItem(null);
                }
            }
            return null;
        }
    }

    private static Map<Integer, Integer> toMapIdSlotMap(Inventory inv) {
        Map<Integer /* :mapId */, Integer /* :slot */> invMapSlots = new HashMap<>();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack invItem = inv.getItem(slot);
            if (!MapManager.managesMap(invItem)) {
                continue;
            }
            int id = MapManager.getMapIdFromItemStack(invItem);
            invMapSlots.put(id, slot);
        }
        return invMapSlots;
    }
}
