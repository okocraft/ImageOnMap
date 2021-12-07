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

import java.util.Locale;
import org.bukkit.entity.Player;

/**
 * A shortcut to translate texts.
 *
 * <p>Use {@code I.t("text", ...)}, or statically import {@code I.t} and use {@code t("text", ...)} to
 * translate something.</p>
 *
 *
 * <h3>The parameters</h3>
 *
 * <p>Translated string accept parameters in the following format: {@code {0}}, {@code {1}}, etc. These
 * tokens are replaced with the given parameters at runtime; the first parameter replaces the {@code
 * {0}} token, the second one, {@code {1}}, and so on.</p>
 *
 * <p>Other parameters related options are available; see {@linkplain java.text.MessageFormat the
 * {@code MessageFormat} documentation} for more details.</p>
 *
 * <p>Note that the escaping behavior of the {@code '} character presented in the linked documentation
 * above <strong>does not apply</strong> to these translated texts, for convenience reasons.</p>
 *
 *
 * <h3>Extracting strings from the source with {@code xgettext}</h3>
 *
 * <p>Give the following parameters to extract these strings to a {@code .po} file with {@code xgettext}:</p>
 *
 * <pre>
 *     # Lines: reset, with traditional imports and with static ones.
 *     -k
 *     -k"I.t" -k"I.tn:1,2" -k"I.tc:1c,2" -k"I.tcn:1c,2,3" -k"I.tl:2" -k"I.tln:2,3" -k"I.tlc:2c,3" -k"I.tlcn:2c,3,4"
 *         -k"I.sendT:2" -k"I.sendTn:2,3" -k"I.sendTc:2c,3" -k"I.sendTcn:2c,3,4"
 *     -k"t" -k"tn:1,2" -k"tc:1c,2" -k"tcn:1c,2,3" -k"tl:2" -k"tln:2,3" -k"tlc:2c,3" -k"tlcn:2c,3,4" -k"sendT:2"
 *         -k"sendTn:2,3" -k"sendTc:2c,3" -k"sendTcn:2c,3,4"
 * </pre>
 *
 * <p>Example:</p>
 *
 * <pre>
 *     xgettext -c -k -k"I.t" -k"I.tn:1,2" -k"I.tc:1c,2" -k"I.tcn:1c,2,3" -k"I.tl:2" -k"I.tln:2,3" -k"I.tlc:2c,3" \
 *         -k"I.tlcn:2c,3,4" -k"I.sendT:2" -k"I.sendTn:2,3" -k"I.sendTc:2c,3" -k"I.sendTcn:2c,3,4" -k"t" -k"tn:1,2" \
 *         -k"tc:1c,2" -k"tcn:1c,2,3" -k"tl:2" -k"tln:2,3" -k"tlc:2c,3" -k"tlcn:2c,3,4" -k"sendT:2" -k"sendTn:2,3" \
 *         -k"sendTc:2c,3" -k"sendTcn:2c,3,4" --from-code=utf-8 --output=lang.pot *.java
 *
 *     # Or, recursively
 *     find . -iname "*.java" | xargs xgettext -c -k -k"I.t" -k"I.tn:1,2" -k"I.tc:1c,2" -k"I.tcn:1c,2,3" -k"I.tl:2" \
 *         -k"I.tln:2,3" -k"I.tlc:2c,3" -k"I.tlcn:2c,3,4" -k"I.sendT:2" -k"I.sendTn:2,3" -k"I.sendTc:2c,3" \
 *         -k"I.sendTcn:2c,3,4" -k"t" -k"tn:1,2" -k"tc:1c,2" -k"tcn:1c,2,3" -k"tl:2" -k"tln:2,3" -k"tlc:2c,3" \
 *         -k"tlcn:2c,3,4" -k"sendT:2" -k"sendTn:2,3" -k"sendTc:2c,3" -k"sendTcn:2c,3,4" \
 *         --from-code=utf-8 --output=lang.pot
 * </pre>
 */
@SuppressWarnings("checkstyle:LineLength")
public class I {
    /**
     * Translates the string.
     *
     * @param text       The string to translate.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated.
     */
    public static String t(String text, Object... parameters) {
        return I18n.translate(null, null, text, null, null, parameters);
    }

    /**
     * Translates the string using the given locale.
     *
     * @param locale     The locale to use to translate the string.
     * @param text       The string to translate.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated.
     * @deprecated Use {@link #tl(Locale, String, Object...)} for the gettext
     *     extractor to be able to read the strings.
     */
    @Deprecated
    public static String t(Locale locale, String text, Object... parameters) {
        return I18n.translate(locale, null, text, null, null, parameters);
    }

    /**
     * Translates the string with a plural.
     *
     * <p>The count is likely to be used in the string, so if only a count is given, this count is
     * also interpreted as a parameter (the first and only one, {@code {0}}). If this behavior annoys
     * you, you can disable it using {@link I18n#addCountToParameters(boolean)}.</p>
     *
     * @param singular   The singular version of the string.
     * @param plural     The plural version of the string.
     * @param count      The items count, used to choose the plural form according to the language
     *                   plural rules.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated, chosen according to the language
     *     plural rules.
     */
    public static String tn(String singular, String plural, Integer count, Object... parameters) {
        return I18n.translate(null, null, singular, plural, count, parameters);
    }

    /**
     * Translates the string with a plural using the given locale.
     *
     * <p>The count is likely to be used in the string, so if only a count is given, this count is
     * also interpreted as a parameter (the first and only one, {@code {0}}). If this behavior annoys
     * you, you can disable it using {@link I18n#addCountToParameters(boolean)}.</p>
     *
     * @param locale     The locale to use to translate the string.
     * @param singular   The singular version of the string.
     * @param plural     The plural version of the string.
     * @param count      The items count, used to choose the plural form according to the language
     *                   plural rules.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated, chosen according to the language
     *     plural rules.
     * @deprecated Use {@link #tln(Locale, String, String, Integer, Object...)} for the gettext
     *     extractor to be able to read the strings.
     */
    @Deprecated
    public static String tn(Locale locale, String singular, String plural, Integer count, Object... parameters) {
        return I18n.translate(locale, null, singular, plural, count, parameters);
    }

    /**
     * Translates the string in the given context using the given locale.
     *
     * <p>The context is used when you have two identical strings to translate that may be
     * translated differently according to the context.</p>
     *
     * @param locale     The locale to use to translate the string.
     * @param context    The context.
     * @param text       The string to translate.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated.
     * @deprecated Use {@link #tlc(Locale, String, String, Object...)} for the gettext
     *     extractor to be able to read the strings.
     */
    public static String tc(Locale locale, String context, String text, Object... parameters) {
        return I18n.translate(locale, context, text, null, null, parameters);
    }

    /**
     * Translates the string in the given context, with a plural using the given locale.
     *
     * <p>The context is used when you have two identical strings to translate that may be
     * translated differently according to the context.</p>
     *
     * <p>The count is likely to be used in the string, so if only a count is given, this count is
     * also interpreted as a parameter (the first and only one, {@code {0}}). If this behavior annoys
     * you, you can disable it using {@link I18n#addCountToParameters(boolean)}.</p>
     *
     * @param locale     The locale to use to translate the string.
     * @param context    The context.
     * @param singular   The singular version of the string.
     * @param plural     The plural version of the string.
     * @param count      The items count, used to choose the plural form according to the language
     *                   plural rules.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated, chosen according to the language
     *     plural rules.
     * @deprecated Use {@link #tlcn(Locale, String, String, String, Integer, Object...)} for the gettext
     *     extractor to be able to read the strings.
     */
    public static String tcn(Locale locale, String context, String singular, String plural, Integer count,
                             Object... parameters) {
        return I18n.translate(locale, context, singular, plural, count, parameters);
    }

    /**
     * Translates the string using the given locale.
     *
     * @param locale     The locale to use to translate the string.
     * @param text       The string to translate.
     * @param parameters The parameters. See the class description for details.
     * @return The translated string, with parameters incorporated.
     */
    public static String tl(Locale locale, String text, Object... parameters) {
        return I18n.translate(locale, null, text, null, null, parameters);
    }

    /**
     * Translates the string and sends it to the given player.
     *
     * @param player     The player to send the message to.
     * @param text       The string to translate.
     * @param parameters The parameters. See the class description for details.
     */
    public static void sendT(Player player, String text, Object... parameters) {
        player.sendMessage(I18n.translate(I18n.getPlayerLocale(player), null, text, null, null, parameters));
    }
}
