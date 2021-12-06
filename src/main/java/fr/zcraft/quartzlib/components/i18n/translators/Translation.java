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

package fr.zcraft.quartzlib.components.i18n.translators;

import java.util.List;


/**
 * Represents a translation.
 */
public class Translation {
    private final String original;
    private final String originalPlural;
    private final String context;
    private final List<String> translations;

    /**
     * Creates a new translation.
     */
    public Translation(String context, String original, String originalPlural, List<String> translations) {
        this.context = context;
        this.original = original;
        this.originalPlural = originalPlural;
        this.translations = translations;
    }

    /**
     * Gets the original, untranslated, string.
     * @return The original, untranslated, string.
     */
    public String getOriginal() {
        return original;
    }

    /**
     * Gets the original, untranslated, plural form.
     * @return The original, untranslated, plural form.
     */
    public String getOriginalPlural() {
        return originalPlural;
    }

    /**
     * Gets the translation context.
     * @return The translation context, or {@code null} if no context was set. Note that an empty
     *     context string and a {@code null} one do not mean the same thing.
     */
    public String getContext() {
        return context;
    }

    /**
     * Gets all of the available translations.
     * @return All the available translations.
     */
    public List<String> getTranslations() {
        return translations;
    }
}
