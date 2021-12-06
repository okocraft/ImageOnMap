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

package fr.zcraft.quartzlib.components.i18n.translators.yaml;

import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.i18n.translators.Translation;
import fr.zcraft.quartzlib.components.i18n.translators.Translator;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;


/**
 * Loads translations stored in a YAML file.
 *
 * <ul>
 *
 *     <li>
 *         This translator <strong>does not</strong> support plurals. If plurals are used, the first
 *         string will always be used, and the other, ignored.
 *     </li>
 *
 *     <li>
 *         This translator <strong>does</strong> support contexts. The context is the superkey in the
 *         YAML structure (see below). Without context, the default one is {@code keys}.
 *     </li>
 *
 * </ul>
 *
 *
 * <h3>YAML structure</h3>
 *
 * <p>The YAML structure must be this one:</p>
 * <pre>
 *     author: "The author name."
 *     team: "The translation team name. Defaults to the author if undefined."
 *     reports: "The contact to use to report translation errors. Defaults to the author if undefined."
 *
 *     keys:
 *         greetings:
 *             hi: "Hi there"
 *             how: "How are you?"
 *     other_context:
 *         greetings:
 *             hi: "Hi!"
 * </pre>
 * <p>With this structure, the keys are retrieved as follow:</p>
 *
 * <pre>
 *     {@link I#t I.t}("greetings.hi")                       # Returns "Hi there"
 *     {@link I#t I.t}("greetings.how")                      # Returns "How are you?"
 * </pre>
 */
public class YAMLTranslator extends Translator {
    private String author = null;
    private String team = null;
    private String reports = null;

    public YAMLTranslator(Locale locale, File file) {
        super(locale, file);
    }

    public YAMLTranslator(Locale locale, String resourceReference) {
        super(locale, resourceReference);
    }

    @Override
    protected void load() {
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(getReader());

        if (configuration.getKeys(false).isEmpty()) {
            PluginLogger.error("Cannot load the {0} translation file.", getFilePath());
            return;
        }

        for (final Map.Entry<String, Object> entry : configuration.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigurationSection) {
                final ConfigurationSection context = (ConfigurationSection) entry.getValue();
                final String contextName = entry.getKey().equals("keys") ? null : entry.getKey();

                for (Map.Entry<String, Object> translationEntry : context.getValues(true).entrySet()) {
                    if (!(translationEntry.getValue() instanceof ConfigurationSection)) {
                        registerTranslation(new Translation(contextName, translationEntry.getKey(), null,
                                Collections.singletonList(translationEntry.getValue().toString())));
                    }
                }
            } else {
                final String value = entry.getValue().toString();

                switch (entry.getKey().toLowerCase()) {
                    case "author" -> author = value;
                    case "team" -> team = value;
                    case "reports" -> reports = value;
                    default -> {
                    }
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
        return team != null ? team : author;
    }

    @Override
    public String getReportErrorsTo() {
        return reports != null ? reports : author;
    }
}
