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

package fr.zcraft.quartzlib.tools.world;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

/**
 * Utility class for dealing with worlds and locations.
 */
public class WorldUtils {
    /**
     * Returns if the two given locations point to the same block, i.e if their
     * block coordinates are equal.
     *
     * @param loc1 The first location
     * @param loc2 The second location
     * @return True if the two given locations point to the same block.
     */
    public static boolean blockEquals(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }

    /**
     * Returns the orientation of the specified location, as a BlockFace.
     * The precision of the returned BlockFace is restricted to NORTH, SOUTH,
     * EAST and WEST only.
     *
     * @param loc The location.
     * @return the orientation of the specified location, as a BlockFace.
     */
    public static BlockFace get4thOrientation(Location loc) {
        float yaw = Math.abs(loc.getYaw()) - 180f;

        if (yaw <= 45 && yaw > -45) {
            return BlockFace.NORTH;
        }

        if (yaw <= -45 && yaw > -135) {
            return BlockFace.WEST;
        }

        if (yaw <= -135 || yaw > 135) {
            return BlockFace.SOUTH;
        }

        if (yaw <= 135 && yaw > 45) {
            return BlockFace.EAST;
        }

        return BlockFace.SELF;
    }
}
