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

package fr.zcraft.quartzlib.components.scoreboard.sender;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.lang.Validate;


/**
 * Represents an objective behind a {@link fr.zcraft.quartzlib.components.scoreboard.Sidebar Sidebar}.
 */
public class SidebarObjective {
    public static final int MAX_LENGTH_OBJECTIVE_NAME = 16;
    public static final int MAX_LENGTH_OBJECTIVE_DISPLAY_NAME = 32;
    public static final int MAX_LENGTH_SCORE_NAME = 40;

    private final String name;
    private final Map<String, Integer> scores = new HashMap<>();
    private final Set<UUID> receivers = new HashSet<>();
    private String displayName;


    /**
     * Constructs a new sidebar objective.
     *
     * @param name        The name of the objective.
     * @param displayName The display name of this objective (i.e. the title of the sidebar).
     */
    public SidebarObjective(String name, String displayName) {
        if (name == null) {
            name = Math.abs(UUID.randomUUID().getLeastSignificantBits()) + "";
        }

        this.name = name.substring(0, Math.min(MAX_LENGTH_OBJECTIVE_NAME, name.length()));
        setDisplayName(displayName);
    }

    /**
     * Constructs a new sidebar objective with a random name.
     *
     * @param displayName The display name of this objective (i.e. the title of the sidebar).
     */
    public SidebarObjective(String displayName) {
        this(null, displayName);
    }


    /**
     * Sets a score.
     *
     * @param name  The name of the score.
     * @param score The score associated.
     */
    public void setScore(String name, Integer score) {
        Validate.notNull(name, "The score name cannot be null!");
        Validate.notNull(score, "The score cannot be null!");

        if (name.length() > MAX_LENGTH_SCORE_NAME) {
            name = name.substring(0, MAX_LENGTH_SCORE_NAME);
        }

        scores.put(name, score);
    }

    /**
     * Removes a score.
     * @param name The name of the score.
     */
    public void removeScore(String name) {
        Validate.notNull(name, "The score name cannot be null!");

        scores.remove(name);
    }

    /**
     * Adds a receiver of this objective.
     *
     * @param id The player's UUID.
     */
    public void addReceiver(UUID id) {
        receivers.add(id);
    }

    /**
     * Removes a receiver from this objective.
     *
     * @param id The player's UUID.
     */
    public void removeReceiver(UUID id) {
        receivers.remove(id);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the objective's display name (i.e. the title of the sidebar).
     *
     * @param displayName The new display name.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName != null
                ? displayName.substring(0, Math.min(MAX_LENGTH_OBJECTIVE_DISPLAY_NAME, displayName.length())) : "";
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public Set<UUID> getReceivers() {
        return receivers;
    }
}
