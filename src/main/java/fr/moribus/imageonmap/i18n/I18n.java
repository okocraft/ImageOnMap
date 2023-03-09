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

package fr.moribus.imageonmap.i18n;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.i18n.translators.Translator;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.MessageFormat;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class I18n {
    private static final Map<Locale, Set<Translator>> translators = new ConcurrentHashMap<>();

    private static final Comparator<Translator> TRANSLATORS_COMPARATOR =
            (translator1, translator2) -> {
                if (translator1.equals(translator2)) {
                    return 0;
                } else if (translator1.getPriority() == translator2.getPriority()) {
                    return translator1.getFilePath().compareTo(translator2.getFilePath());
                } else {
                    return Integer.compare(translator2.getPriority(), translator1.getPriority());
                }
            };

    private static Locale primaryLocale = null;
    private static Locale fallbackLocale = null;

    private static final String i18nDirectory = "i18n";

    private static final boolean userFriendlyFormatting = true;
    private static final String errorColor = ChatColor.RED.toString();
    private static final String noticeColor = ChatColor.WHITE.toString();
    private static final String successColor = ChatColor.GREEN.toString();
    private static final String statusColor = ChatColor.GRAY.toString();
    private static final String commandColor = ChatColor.GOLD.toString();

    /**
     * @return The name of the subdirectory where the translations are stored. Default: "i18n".
     */
    public static String getI18nDirectory() {
        return i18nDirectory;
    }

    /**
     * Return the locale used by the player's client.
     *
     * @param player The player
     * @return The player's client locale.
     */
    @SuppressWarnings("deprecation")
    public static Locale getPlayerLocale(Player player) {
        if (player == null) {
            return null;
        }
        return localeFromString(player.getLocale());
    }

    /**
     * Loads the translations from the plugin's JAR file under the
     * given directory (recursively).
     *
     * @param directory The directory to scan inside the plugin's JAR.
     */
    public static void loadFromJar(final JarFile jarFile, final String directory) {
        if (jarFile == null) {
            throw new IllegalStateException("I18n.jarFile must be set to use the loadFromJar method.");
        }

        final String normalizedDirectory = normalizeDirectory(directory);

        final Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            final String name = entries.nextElement().getName();

            if (name.startsWith(normalizedDirectory)) {
                if (!name.endsWith("/")) {
                    final String[] nameParts = name.split("/");
                    final String basename = nameParts[nameParts.length - 1];

                    final Locale locale = localeFromString(basename.split("\\.")[0]);
                    final Translator translator = Translator.getInstance(locale, name);

                    if (translator != null) {
                        registerTranslator(locale, translator, 0);
                    }
                }
            }
        }
    }

    private static String normalizeDirectory(String directory) {
        var result = directory.trim();
        int length = result.length();

        int startIndex = result.indexOf('/') == 0 ? 1 : 0;
        boolean appendSlash = result.lastIndexOf('/') != length - 1;

        result = directory.substring(startIndex, length);

        if (appendSlash) {
            result = result + "/";
        }

        return result;
    }


    /* **  TRANSLATIONS LOADING METHODS  ** */

    /**
     * Loads a file into the translations system.
     * <p>If this file is a directory, all files inside will be loaded, recursively.</p>
     * <p>The locale will be extracted from the file name, and the format, from the file's extension.</p>
     *
     * @param file     The file to load.
     * @param priority The priority to set for this translator. Translators with
     *                 a higher priority will be called first for a translation.
     */
    public static void load(final File file, int priority) {
        Objects.requireNonNull(file, "The File to load into the i18n component cannot be null.");

        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            final File[] children = file.listFiles();
            if (children == null) {
                return;
            }

            for (final File child : children) {
                load(child, priority);
            }
        } else if (file.isFile()) {
            final Locale locale = localeFromString(file.getName().split("\\.")[0]);
            final Translator translator = Translator.getInstance(locale, file);

            if (translator != null) {
                registerTranslator(locale, translator, priority);
            }
        }
    }

    /**
     * Registers a translator into the translations system.
     *
     * @param locale     The locale to register this translator for.
     * @param translator The translator.
     * @param priority   The priority to set for this translator. Translators with
     *                   a higher priority will be called first for a translation.
     */
    public static void registerTranslator(final Locale locale, final Translator translator, final int priority) {
        translator.setPriority(priority);
        getTranslatorsChain(locale).add(translator);
    }

    /**
     * Retrieves the translators chain for a given locale.
     *
     * @param locale The locale.
     * @return The chain, in an ordered {@link TreeSet}. Will never be {@code null}, but can
     * be empty if no translator was registered for that locale.
     */
    private static Set<Translator> getTranslatorsChain(final Locale locale) {
        return I18n.translators.computeIfAbsent(locale, k -> new TreeSet<>(TRANSLATORS_COMPARATOR));
    }

    /**
     * Tries to translate the given string from the given chain.
     *
     * @param chain           The translators chain. All translators will be tested one after another
     *                        in order, until one yields a translation.
     * @param context         The translation context. {@code null} if no context defined.
     * @param messageId       The string to translate.
     * @param count           The count of items to use to choose the singular or plural form.
     *                        {@code null} if this translation does not have a plural form.
     * @return The non-formatted translated string, if one of the translators was able to
     * translate it; else, {@code null}.
     */
    private static String translateFromChain(Set<Translator> chain, String context, String messageId, Integer count) {
        for (Translator translator : chain) {
            final String translated = translator.translate(context, messageId, count);
            if (translated != null) {
                return translated;
            }
        }

        return null;
    }

    /**
     * Translates the given string using the given locale.
     *
     * <p> If the given locale is null, tries to use the primary locale;
     * fallbacks to the fallback locale if the string cannot be translated;
     * fallbacks to the input text if the string still cannot be translated. </p>
     *
     * <p> The count is likely to be used in the string, so if, for a translation with plurals, only
     * a count is given, this count is also interpreted as a parameter (the first and only one, {@code
     * {0}}).</p>
     *
     * @param locale          The locale to use to translate the string.
     * @param context         The translation context. {@code null} if no context defined.
     * @param messageId       The string to translate.
     * @param messageIdPlural The plural version of the string to translate. {@code null} if this
     *                        translation does not have a plural form.
     * @param count           The count of items to use to choose the singular or plural form.
     *                        {@code null} if this translation does not have a plural form.
     * @param parameters      The parameters, replacing values like {@code {0}} in the translated
     *                        string.
     * @return The translated text, with the parameters replaced by their values.
     */
    public static String translate(Locale locale, String context, String messageId, String messageIdPlural,
                                   Integer count, Object... parameters) {
        String translated = null;
        Locale usedLocale = Locale.getDefault();

        // Simplifies the programmer's work. The count is likely to be used in the string, so if,
        // for a translation with plurals, only a count is given, this count is also interpreted as
        // a parameter (the first and only one, {0}).
        if (count != null && (parameters == null || parameters.length == 0)) {
            parameters = new Object[]{count};
        }

        // We first try the given locale, or a close one, if non-null.
        if (locale != null) {
            // We first try the given locale
            translated = translateFromChain(getTranslatorsChain(locale), context, messageId, count);
            if (translated != null) {
                usedLocale = locale;
            } else {
                // Then, we lookup for a close locale (same language and country, then same language)
                Locale perfect = null;
                final Set<Locale> close = new HashSet<>();

                for (Locale curLocale : translators.keySet()) {
                    if (curLocale.getLanguage().equals(locale.getLanguage())) {
                        if (curLocale.getCountry().equals(locale.getCountry())) {
                            perfect = curLocale;
                        } else {
                            close.add(curLocale);
                        }
                    }
                }

                if (perfect != null && (translated =
                        translateFromChain(getTranslatorsChain(perfect), context, messageId, count))
                        != null) {
                    usedLocale = perfect;
                } else {
                    for (Locale closeLocale : close) {
                        if ((translated = translateFromChain(getTranslatorsChain(closeLocale), context, messageId, count)) != null) {
                            usedLocale = closeLocale;
                            break;
                        }
                    }
                }
            }
        }

        // If we still don't have anything, we try the primary then fallback locales
        if (translated == null && primaryLocale != null && (translated =
                translateFromChain(getTranslatorsChain(primaryLocale), context, messageId, count))
                != null) {
            usedLocale = primaryLocale;
        }
        if (translated == null && fallbackLocale != null && (translated =
                translateFromChain(getTranslatorsChain(fallbackLocale), context, messageId, count))
                != null) {
            usedLocale = fallbackLocale;
        }

        // If it's still null, we use the messageId singular or plural, using
        // English rules to select the one.
        if (translated == null) {
            if (count != null && count != 1 && messageIdPlural != null) {
                translated = messageIdPlural;
            } else {
                translated = messageId;
            }

            usedLocale = primaryLocale != null ? primaryLocale :
                    (fallbackLocale != null ? fallbackLocale : Locale.getDefault());
        }


        if (userFriendlyFormatting) {
            translated = replaceFormattingCodes(translated);
        }

        // We replace « ' » with « '' » to escape single quotes, so the formatter leave them alive
        MessageFormat formatter = new MessageFormat(translated.replace("'", "''"), usedLocale);

        // We remove non-breaking spaces, as Minecraft ignores them (breaking texts regardless of their presence) and
        // often badly displays them (dashed square with NBSP inside).
        return formatter.format(parameters)
                .replace("\u00A0", " ").replace("\u2007", " ").replace("\u202F", " ")  // Non-breaking spaces
                .replace("\u2009", " ") // Thin space
                .replace("\u2060",
                        ""); // “WORD-JOINER” non-breaking space (zero-width)
    }



    /* **  TRANSLATION METHODS  ** */

    /**
     * Replaces some formatting codes into system codes.
     *
     * @param text The input text.
     * @return The text with formatters replaced.
     */
    private static String replaceFormattingCodes(String text) {
        return text.replace("{black}", ChatColor.BLACK.toString())
                .replace("{darkblue}", ChatColor.DARK_BLUE.toString())
                .replace("{darkgreen}", ChatColor.DARK_GREEN.toString())
                .replace("{darkaqua}", ChatColor.DARK_AQUA.toString())
                .replace("{darkred}", ChatColor.DARK_RED.toString())
                .replace("{darkpurple}", ChatColor.DARK_PURPLE.toString())
                .replace("{gold}", ChatColor.GOLD.toString())
                .replace("{gray}", ChatColor.GRAY.toString())
                .replace("{darkgray}", ChatColor.DARK_GRAY.toString())
                .replace("{blue}", ChatColor.BLUE.toString())
                .replace("{green}", ChatColor.GREEN.toString())
                .replace("{aqua}", ChatColor.AQUA.toString())
                .replace("{red}", ChatColor.RED.toString())
                .replace("{lightpurple}", ChatColor.LIGHT_PURPLE.toString())
                .replace("{yellow}", ChatColor.YELLOW.toString())
                .replace("{white}", ChatColor.WHITE.toString())

                .replace("{bold}", ChatColor.BOLD.toString())
                .replace("{strikethrough}", ChatColor.STRIKETHROUGH.toString())
                .replace("{underline}", ChatColor.UNDERLINE.toString())
                .replace("{italic}", ChatColor.ITALIC.toString())
                .replace("{obfuscated}", ChatColor.MAGIC.toString())

                .replace("{reset}", ChatColor.RESET.toString())

                .replace("{ce}", errorColor)
                .replace("{cc}", commandColor)
                .replace("{ci}", noticeColor)
                .replace("{cs}", successColor)
                .replace("{cst}", statusColor);
    }

    /**
     * Sets the primary locale, the locale always used if available to translate the strings.
     *
     * @param locale The locale. If {@code null}, system locale used.
     */
    public static void setPrimaryLocale(final Locale locale) {
        primaryLocale = locale != null ? locale : Locale.getDefault();
    }


    /* **  METADATA METHODS  ** */

    /**
     * Sets the fallback locale, used if a translation is not available in the primary locale.
     *
     * @param locale The locale.
     */
    public static void setFallbackLocale(final Locale locale) {
        fallbackLocale = locale;
    }

    public static Locale localeFromString(final String localeName) {
        String[] splitLocale = localeName.split("[_\\-]", 2);
        if (splitLocale.length >= 2) {
            return new Locale(splitLocale[0], splitLocale[1]);
        } else {
            return new Locale(localeName);
        }
    }

    public static void onEnable(final JarFile jarFile) {
        setPrimaryLocale(Locale.getDefault());
        setFallbackLocale(Locale.US);

        if (jarFile != null) {
            loadFromJar(jarFile, i18nDirectory);
        }

        load(new File(ImageOnMap.getPlugin().getDataFolder(), i18nDirectory), 100);
    }
}
