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

package fr.moribus.imageonmap.i18n.translators.properties;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.ResourceBundle;

import fr.moribus.imageonmap.i18n.I18n;
import fr.moribus.imageonmap.i18n.translators.Translation;
import fr.moribus.imageonmap.i18n.translators.Translator;


/**
 * Loads translations stored in a .properties or .class file, using Java native {@link
 * ResourceBundle resource bundles}.
 *
 * <p>The file are loaded from the file system instead of the JAR file, to allow end-user
 * changes.</p>
 *
 * <ul>
 *
 *     <li>
 *         This translator <strong>does not</strong> support plurals. If plurals are used, the first
 *         string will always be used, and the other, ignored.
 *     </li>
 *
 *     <li>
 *         This translator <strong>does not</strong> support contexts. If a context is provided, it is
 *         simply ignored.
 *     </li>
 *
 * </ul>
 *
 *
 * <h3>Special keys</h3>
 *
 * <p>Some keys have a special meaning:</p>
 *
 * <ul>
 *     <li>{@code meta-author}: the translator;</li>
 *     <li>{@code meta-team}: the translation team;</li>
 *     <li>{@code meta-reports}: the person to contact if translation errors are found.</li>
 * </ul>
 *
 * <p>If you need these keys in your properties files, you can add the {@code quartzlib-i18n-no-metadata}
 * key in the file somewhere, with a non-empty value. The keys above will in this case no longer be
 * special.</p>
 */
public class PropertiesTranslator extends Translator {
    private static final String METADATA_DISABLED = "quartzlib-i18n-no-metadata";
    private static final String METADATA_AUTHOR = "meta-author";
    private static final String METADATA_TEAM = "meta-team";
    private static final String METADATA_REPORTS = "meta-reports";

    private String author = null;
    private String team = null;
    private String report = null;


    public PropertiesTranslator(Locale locale, File file) {
        super(locale, file);
    }

    public PropertiesTranslator(Locale locale, String resourceReference) {
        super(locale, resourceReference);
    }

    @Override
    protected void load() {
        final ZLibResourceBundleControl control =
                file != null ? new ZLibResourceBundleControl(file) : new ZLibResourceBundleControl(resourceReference);

        final ResourceBundle bundle =
                ResourceBundle.getBundle(control.toBundleName(I18n.getI18nDirectory(), locale), locale, control);
        final boolean disableMetadata = bundle.containsKey(METADATA_DISABLED);

        for (final String key : bundle.keySet()) {
            final String value = bundle.getString(key);

            if (disableMetadata) {
                if (!key.equals(METADATA_DISABLED)) {
                    registerTranslation(new Translation(null, key, null, Collections.singletonList(value)));
                }
            } else {
                switch (key) {
                    case METADATA_AUTHOR:
                        author = value;
                        break;

                    case METADATA_TEAM:
                        team = value;
                        break;

                    case METADATA_REPORTS:
                        report = value;
                        break;

                    case METADATA_DISABLED:
                        break;

                    default:
                        registerTranslation(new Translation(null, key, null, Collections.singletonList(value)));
                        break;
                }
            }
        }
    }

    @Override
    public String getLastTranslator() {
        return author;
    }

    @Override
    public String getTranslationTeam() {
        return team;
    }

    @Override
    public String getReportErrorsTo() {
        return report;
    }

    /**
     * The context should always be ignored.
     *
     * {@inheritDoc}
     */
    @Override
    protected String getContextKey(String context) {
        return NO_CONTEXT_KEY;
    }
}
