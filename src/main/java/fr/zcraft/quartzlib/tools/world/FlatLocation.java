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
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

/**
 * This class provides extra utilities to the location class, to manipulate its
 * coordinates relative to an in-game 2D plane.
 * <p>Note that only NORTH, SOUTH, EAST and WEST orientations are supported.
 * If passing any other orientation, an exception will be thrown.</p>
 * <p>In this documentation, the X and Y coordinates always refer to the plane's
 * coordinates, unless otherwise specified (referred as 'real world coordinates').</p>
 */
public class FlatLocation extends Location {
    /**
     * The orientation of the in-game plane.
     */
    private final BlockFace facing;

    /**
     * Creates a new FlatLocation from a world, real-world coordinates and the facing direction.
     *
     * @param world  The world.
     * @param x      The real-world X coordinate.
     * @param y      The real-world Y coordinate.
     * @param z      The real-world Z coordinate.
     * @param yaw    The yaw.
     * @param pitch  The pitch.
     * @param facing The facing direction of the plane.
     */
    public FlatLocation(World world, double x, double y, double z, float yaw, float pitch, BlockFace facing) {
        super(world, x, y, z, yaw, pitch);
        this.facing = facing;
    }

    /**
     * Creates a new FlatLocation from a world, real-world coordinates and the facing direction.
     *
     * @param world  The world.
     * @param x      The real-world X coordinate.
     * @param y      The real-world Y coordinate.
     * @param z      The real-world Z coordinate.
     * @param facing The facing direction.
     */
    public FlatLocation(World world, double x, double y, double z, BlockFace facing) {
        this(world, x, y, z, 0, 0, facing);
    }

    /**
     * Creates a new FlatLocation from a world and the facing direction.
     * All coordinates are initialized to 0.
     *
     * @param world  The world.
     * @param facing The facing direction.
     */
    public FlatLocation(World world, BlockFace facing) {
        this(world, 0, 0, 0, facing);
    }

    /**
     * Creates a new FlatLocation from a Bukkit real-world Location and the facing direction.
     *
     * @param loc    The location
     * @param facing The facing direction.
     */
    public FlatLocation(Location loc, BlockFace facing) {
        this(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(), facing);
    }

    /**
     * Creates a new FlatLocation from another one.
     *
     * @param other The other FlatLocation
     */
    public FlatLocation(FlatLocation other) {
        this(other, other.facing);
    }

    /**
     * Returns the block distance (on the plane's X axis) between two locations.
     * If the two locations aren't one the same plane, an exception is thrown.
     *
     * @param loc1 The first location.
     * @param loc2 The second location.
     * @return The block distance
     */
    public static int flatBlockDistanceX(FlatLocation loc1, FlatLocation loc2) {
        checkSimilarLocations(loc1, loc2);

        return switch (loc1.getFacing()) {
            case NORTH, SOUTH -> Math.abs(loc1.getBlockX() - loc2.getBlockX());
            case EAST, WEST -> Math.abs(loc1.getBlockZ() - loc2.getBlockZ());
            default -> throw new UnsupportedOperationException("Non-N/S/E/W orientations are not supported.");
        };
    }

    /**
     * Returns the block distance (on the plane's Y axis) between two locations.
     * If the two locations aren't one the same plane, an exception is thrown.
     *
     * @param loc1 The first location.
     * @param loc2 The second location.
     * @return The block distance
     */
    public static int flatBlockDistanceY(FlatLocation loc1, FlatLocation loc2) {
        checkSimilarLocations(loc1, loc2);

        return Math.abs(loc1.getBlockY() - loc2.getBlockY());
    }

    protected static void checkSimilarLocations(FlatLocation loc1, FlatLocation loc2) throws IllegalArgumentException {
        if (loc1.getFacing() != loc2.getFacing()) {
            throw new IllegalArgumentException("Trying to compare two FlatLocations on different axes.");
        }
        if (loc1.getWorld() != loc2.getWorld()) {
            throw new IllegalArgumentException("Trying to compare two Locations from different worlds.");
        }
    }

    /**
     * Creates a new FlatLocation, from the minimal coordinates of the two given.
     *
     * @param loc1 The first location
     * @param loc2 The second location
     * @return The new location
     */
    public static FlatLocation minMerged(FlatLocation loc1, FlatLocation loc2) {
        checkSimilarLocations(loc1, loc2);
        FlatLocation loc = new FlatLocation(loc1.getWorld(), loc1.getFacing());
        loc.setY(Math.min(loc1.getY(), loc2.getY()));

        switch (loc1.getFacing()) {
            case NORTH -> {
                loc.setX(Math.max(loc1.getX(), loc2.getX()));
                loc.setZ(loc1.getZ());
            }
            case SOUTH -> {
                loc.setX(Math.min(loc1.getX(), loc2.getX()));
                loc.setZ(loc1.getZ());
            }
            case EAST -> {
                loc.setZ(Math.max(loc1.getZ(), loc2.getZ()));
                loc.setX(loc1.getX());
            }
            case WEST -> {
                loc.setZ(Math.min(loc1.getZ(), loc2.getZ()));
                loc.setX(loc1.getX());
            }
        }

        return loc;
    }

    /**
     * Gets the orientation of the plane.
     * @return The orientation of the plane.
     */
    public BlockFace getFacing() {
        return facing;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public @NotNull FlatLocation clone() {
        return new FlatLocation(this);
    }

    /**
     * Adds the given values to the plane's coordinates.
     *
     * @param x The X coordinate
     * @param z The Z coordinate
     * @return This location.
     */
    public FlatLocation addH(double x, double z, BlockFace bf) {
        switch (bf) {
            case NORTH -> add(x, 0, -z);
            case EAST -> add(x, 0, z);
            case WEST -> add(-x, 0, -z);
            case SOUTH -> add(-x, 0, z);
        }

        return this;
    }

    /**
     * Adds the given values to the plane's coordinates.
     *
     * @param x The X coordinate
     * @param y The Y coordinate
     * @return This location.
     */
    public FlatLocation add(double x, double y) {
        switch (facing) {
            case NORTH -> add(-x, y, 0);
            case SOUTH -> add(x, y, 0);
            case EAST -> add(0, y, -x);
            case WEST -> add(0, y, x);
            default -> throw new UnsupportedOperationException("addToLocation called with non-N/S/E/W orientation.");
        }

        return this;
    }
}
