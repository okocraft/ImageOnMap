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

package fr.moribus.imageonmap.i18n.translators;

import fr.moribus.imageonmap.i18n.translators.gettext.GettextPOTranslator;
import fr.moribus.imageonmap.i18n.translators.properties.PropertiesTranslator;
import fr.moribus.imageonmap.i18n.translators.yaml.YAMLTranslator;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;


/**
 * Classes used to load &amp; store the translations extends this class.
 * <p>Translators are lazy: strings are only loaded on first use.</p>
 */
public abstract class Translator {
    /**
     * We need a string without meaning to use as the no-context key below.
     * A {@code null} value cannot be used because {@link ConcurrentHashMap}s does not accept {@code null} keys.
     * An empty value cannot be used because an empty context is not the same as no context, and some applications
     * may use them.
     */
    protected static final String NO_CONTEXT_KEY = UUID.randomUUID().toString();

    protected final Locale locale;
    protected final File file;
    protected final String resourceReference;
    /**
     * Context → messageId → translation.
     */
    protected final Map<String, Map<String, Translation>> translations = new ConcurrentHashMap<>();
    private boolean loaded = false;
    private int priority = 0;

    /**
     * Creates a new translator using a file.
     * @param locale The locale associated to the translator.
     * @param file The file to load.
     */
    public Translator(Locale locale, File file) {
        this.locale = locale;
        this.file = file;
        this.resourceReference = null;
    }

    /**
     * Creates a new translator using a resource reference.
     * @param locale The locale associated to the translator.
     * @param resourceReference The reference of the resource to load.
     */
    public Translator(final Locale locale, final String resourceReference) {
        this.locale = locale;
        this.resourceReference = resourceReference;
        this.file = null;
    }

    /**
     * Returns a new translations loader for this locale and this file.
     *
     * @param locale The locale
     * @param file   The file
     * @return A translations loader for this file.
     */
    public static Translator getInstance(Locale locale, File file) {
        final String[] fileNameParts = file.getName().split("\\.");

        if (fileNameParts.length < 2) {
            return null;
        }

        return switch (fileNameParts[fileNameParts.length - 1].toLowerCase()) {
            case "po" -> new GettextPOTranslator(locale, file);
            case "yml", "yaml" -> new YAMLTranslator(locale, file);
            case "properties", "class" -> new PropertiesTranslator(locale, file);
            default -> null;
        };
    }

    /**
     * Returns a new translations loader for this locale and a resource from the plugin's JAR.
     *
     * @param locale            The locale
     * @param resourceReference The path to the resource.
     * @return A translations loader for this file.
     */
    public static Translator getInstance(Locale locale, String resourceReference) {
        final String[] fileNameParts = resourceReference.split("\\.");

        if (fileNameParts.length < 2) {
            return null;
        }

        return switch (fileNameParts[fileNameParts.length - 1].toLowerCase()) {
            case "po" -> new GettextPOTranslator(locale, resourceReference);
            case "yml", "yaml" -> new YAMLTranslator(locale, resourceReference);
            case "properties", "class" -> new PropertiesTranslator(locale, resourceReference);
            default -> null;
        };
    }

    protected Reader getReader() {
        if (file != null) {
            try {
                return new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(file), StandardCharsets.UTF_8));

            } catch (IOException e) {
                PluginLogger.error("Unable to load file {0} in translator {1}", e, getFilePath(),
                        getClass().getSimpleName());
                return null;
            }
        } else if (resourceReference != null) {
            final InputStream stream = QuartzLib.getPlugin().getResource(resourceReference);

            if (stream == null) {
                PluginLogger
                        .error("Unable to load file {0} in translator {1}", getFilePath(), getClass().getSimpleName());
                return null;
            }

            return new InputStreamReader(stream);
        } else {
            return null;
        }
    }

    /**
     * Gets the path of the underlying file.
     * @return the path of the underlying file.
     */
    public String getFilePath() {
        if (file != null) {
            return file.getAbsolutePath();
        } else if (resourceReference != null) {
            return "jar:" + resourceReference;
        } else {
            return "<unknown>";
        }
    }

    /**
     * Loads the translations into the {@link #translations} map. Translators are
     * lazy: this method will automatically be called on first translation request.
     */
    protected abstract void load();

    /**
     * Loads the translations if not already loaded.
     */
    private void load0() {
        if (!loaded) {
            load();
            loaded = true;
        }
    }

    /**
     * Translates a string into the given locale.
     *
     * @param context         The translation context. {@code null} if no context defined.
     * @param messageId       The string to translate.
     * @param messageIdPlural The plural version of the string to translate. {@code null} if this
     *                        translation does not have a plural form.
     * @param count           The count of items to use to choose the singular or plural form.
     *                        {@code null} if this translation does not have a plural form.
     * @return The translated sentence. {@code null} if the sentence is not translated into this
     *     locale.
     */
    public String translate(String context, String messageId, String messageIdPlural, Integer count) {
        load0();

        final Map<String, Translation> contextMap = translations.get(getContextKey(context));
        if (contextMap == null) {
            return null;
        }

        Translation translation = contextMap.get(messageId);
        if (translation == null || translation.getTranslations().size() == 0) {
            return null;
        }

        if (count != null && translation.getTranslations().size() != 1) {
            Integer pluralIndex = getPluralIndex(count);

            // Ensures the translation is available
            if (translation.getTranslations().size() <= pluralIndex) {
                return null;
            }
            return translation.getTranslations().get(pluralIndex);
        } else {
            return translation.getTranslations().get(0);
        }
    }

    /**
     * Returns the plural index to use for the given integer.
     * <p>Translators should override this to use custom plural rules loaded from the files,
     * if available.<br> The default implementation ignores the plural and always returns {@code
     * 0}.</p>
     *
     * @param count The count.
     * @return The translation index to use for this count.
     */
    public Integer getPluralIndex(Integer count) {
        return 0;
    }

    /**
     * Gets the last translator.
     * @return The last translator.
     */
    public abstract String getLastTranslator();

    /**
     * Gets the name of the translation team.
     * @return The name of the translation team.
     */
    public abstract String getTranslationTeam();

    /**
     * Gets the person to contact if there is an error in the translations.
     * @return The person to contact if there is an error in the translations.
     */
    public abstract String getReportErrorsTo();

    /**
     * Gets the locale loaded by this loader.
     * @return The Locale loaded by this loader.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Gets the priority of this translator.
     * @return The priority of this translator: higher priority translators will
     *     called first for a translation in the translators chain.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority of this translator. Higher priority translators
     * will be called first for a translation in the translators chain.
     *
     * @param priority The priority. Default to 0 if unset.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Registers a translation to be stored in the system.
     *
     * @param translation The translation to store.
     */
    protected void registerTranslation(Translation translation) {
        final String context = getContextKey(translation.getContext());
        Map<String, Translation> contextMap = translations.get(context);

        if (contextMap == null) {
            contextMap = new ConcurrentHashMap<>();
            translations.put(context, contextMap);
        }

        contextMap.put(translation.getOriginal(), translation);
    }

    /**
     * Returns the key to use in the translation map for the given context. Must be consistent:
     * multiple calls with the same context must return the same value.
     *
     * <p>The default implementation returns the context, or a randomly generated no-context key if
     * the context is null.</p>
     *
     * @param context The context. May be {@code null}.
     * @return The key to use. Cannot be {@code null}.
     */
    protected String getContextKey(String context) {
        return context != null ? context : NO_CONTEXT_KEY;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Translator that = (Translator) o;

        return new EqualsBuilder()
                .append(priority, that.priority)
                .append(locale, that.locale)
                .append(getFilePath(), that.getFilePath())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(locale)
                .append(getFilePath())
                .append(priority)
                .toHashCode();
    }
}
