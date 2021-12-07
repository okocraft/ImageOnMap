/*
 * This file is part of QuartzLib.
 *
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015 - 2021)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2015 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to create Minecraft mods
 * with the Bukkit API easily.
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

package fr.moribus.imageonmap.i18n.translators.gettext;

import fr.moribus.imageonmap.i18n.translators.Translation;
import fr.moribus.imageonmap.i18n.translators.Translator;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.File;
import java.io.Reader;
import java.util.Locale;


/**
 * Loads Gettext .po files (uncompiled).
 */
public class GettextPOTranslator extends Translator {
    private POFile source = null;

    public GettextPOTranslator(Locale locale, File file) {
        super(locale, file);
    }

    public GettextPOTranslator(Locale locale, String resourceReference) {
        super(locale, resourceReference);
    }

    @Override
    protected void load() {
        try {
            final Reader reader = getReader();
            if (reader == null) {
                return;
            }

            source = new POFile(getReader());

            source.parse();

            for (final Translation translation : source.getTranslations()) {
                registerTranslation(translation);
            }
        } catch (POFile.CannotParsePOException e) {
            PluginLogger.error("Cannot parse the {0} translations file.", e, getFilePath());
            source = null;
        }
    }

    @Override
    public Integer getPluralIndex(Integer count) {
        if (source == null) {
            return count != 1 ? 1 : 0;
        }

        return source.computePluralForm(count);
    }

    @Override
    public String getLastTranslator() {
        return source != null ? source.getLastTranslator() : null;
    }

    @Override
    public String getTranslationTeam() {
        return source != null ? source.getTranslationTeam() : null;
    }

    @Override
    public String getReportErrorsTo() {
        return source != null ? source.getReportErrorsTo() : null;
    }
}
