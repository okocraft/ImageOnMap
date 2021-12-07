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

import fr.moribus.imageonmap.ImageOnMap;
import java.util.Locale;
import java.util.function.Function;
import java.util.logging.Level;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PluralForms {
    /**
     * The default plural forms function when we cannot do anything else. It's the English plural rule.
     */
    private static final Function<Long, Integer> FORMS_FUNCTION_FALLBACK = n -> n != 1 ? 1 : 0;

    /**
     * The number of plural forms for this script.
     */
    private final int formsCount;

    /**
     * The form script, as written in the PO/MO file.
     */
    @NotNull
    private final String formsScript;

    /**
     * A function to call to compute the correct plural form
     * from a count.
     */
    private final Function<Long, Integer> formsFunction;

    /**
     * Constructs a new Gettext plural form.
     *
     * <p>The script will be matched against a database of known script. If one match, it will be implemented in
     * pure Java for better performances. Else, we'll fallback on a JavaScript Engine to do the work. If no
     * engine can be loaded, we'll fallback on hardcoded English rules.
     *
     * @param formsCount  The number of plural forms.
     * @param formsScript The raw script (without “plural=” prefix).
     */
    public PluralForms(int formsCount, @Nullable String formsScript) {
        this.formsCount = formsCount;
        this.formsScript = formsScript.trim();

        this.formsFunction = computeFormsFunction();
    }

    /**
     * For a given number, compute the plural index to use for the locale of this file.
     *
     * <p>Some plural scripts are very commons. For them, we hardcode native functions.
     * We then do not depend on a JavaScript engine, and it's order of magnitude faster.
     * If you can use them, it's always better.
     *
     * <p>This method can only work correctly with Plural-Forms listed at:
     * http://www.gnu.org/software/gettext/manual/html_node/Plural-forms.html#Plural-forms,
     * as well as POEdit-generated plural forms.
     *
     * @param count The count to compute plural for.
     * @return The plural index.
     */
    public int computePluralForm(long count) {
        final int index = this.formsFunction.apply(count);

        if (index < 0 || index > formsCount) {
            return 0;
        } else {
            return index;
        }
    }

    /**
     * Computes the function to use to compute the plural form to use for a given count.
     *
     * <p>We first try to load a pure-Java implementation from a list of known plural scripts.
     * If this fail, we try two JavaScript engines to compute the form. If this fail too, we
     * fallback to English rules.
     *
     * @return a function that can be used to compute the plural index from a count.
     * @see #computePluralForm(long) Method to use the computed function.
     */
    private Function<Long, Integer> computeFormsFunction() {
        if (formsScript == null || formsScript.isEmpty()) {
            return formsFunctionFallback();
        }

        // We first try if this script is known
        Function<Long, Integer> function = formsFunctionFromKnownScripts();

        // Else we try two JS engines, and fallback to English rules if none of them work.
        if (function == null) {
            function = formsFunctionFromNashorn();

            if (function == null) {
                function = formsFunctionFromGraalVM();

                if (function == null) {
                    function = formsFunctionFallback();
                }
            }
        }

        return function;
    }

    /**
     * Normalizes a forms script so we can compare them to a list of known scripts.
     *
     * <p>The normalized version is lowercase, without spaces, and without unnecessary
     * surounding parenthesis.
     *
     * @param script The script.
     * @return The normalized version.
     */
    private String normalizeFormsScript(final String script) {
        final String normalized = script.toLowerCase(Locale.ROOT).replace(" ", "").trim();

        // POEdit tends to add unnecessary parenthesis around the plural scripts. For them to correctly
        // match our known scripts, we strip them.
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            return normalized.substring(1, normalized.length() - 1);
        } else {
            return normalized;
        }
    }

    /**
     * Some plural scripts are very commons. For them, we hardcode native functions.
     * We then do not depend on a JavaScript engine, and it's order of magnitude faster.
     *
     * <p>This evaluate method can only work correctly with Plural-Forms listed at:
     * http://www.gnu.org/software/gettext/manual/html_node/Plural-forms.html#Plural-forms as
     * well as POEdit-generated plural forms.
     *
     * @return a Function to compute the plural index from the given count.
     */
    private Function<Long, Integer> formsFunctionFromKnownScripts() {
        return switch (normalizeFormsScript(formsScript)) {
            // Only one form
            // Japanese, Vietnamese, Korean, Thai
            case "0" -> n -> 0;

            // Two forms, singular used for one only
            // Bahasa Indonesian, Bulgarian, Danish, Dutch, English, Esperanto, Estonian, Faroese, Finnish, German,
            // Greek, Hebrew, Hungarian, Italian, Norwegian, Portuguese, Spanish, Swedish, Turkish
            // Yup. POEdit can generate this.
            case "n!=1", "n<1||n>1", "n>1||n<1" -> n -> n != 1 ? 1 : 0;
            case "n!=0" -> n -> n != 0 ? 1 : 0;
            case "n==0||n==1", "n==1||n==0" -> n -> n == 0 || n == 1 ? 1 : 0;
            case "n>0" -> n -> n > 0 ? 1 : 0;

            // Two forms, singular used for zero and one
            // Brazilian Portuguese, French
            case "n>1" -> n -> n > 1 ? 1 : 0;

            // Icelandic, Macedonian
            case "n%10==1&&n%100!=11" -> n -> n % 10 == 1 && n % 100 != 11 ? 1 : 0;
            case "n<=1||(n>=11&&n<=99)" -> n -> n <= 1 || (n >= 11 && n <= 99) ? 1 : 0;

            // Three forms, special case for zero
            // Latvian
            case "n%10==1&&n%100!=11?0:n!=0?1:2" -> n -> n % 10 == 1 && n % 100 != 11 ? 0 : (n != 0 ? 1 : 2);

            // Filipino, Tagalog
            case "n==1||n==2||n==3||(n%10!=4&&n%10!=6&&n%10!=9)" -> n -> n == 1 || n == 2 || n == 3 || (n % 10 != 4 && n % 10 != 6 && n % 10 != 9) ? 1 : 0;

            // Three forms, special cases for one and two
            // Gaeilge (Irish), Cornish, Nama, Northern Sami
            case "n==1?0:n==2?1:2" -> n -> n == 1 ? 0 : (n == 2 ? 1 : 2);

            // Three forms, special case for numbers ending in 00 or [2-9][0-9]
            // Romanian
            case "n==1?0:(n==0||(n%100>0&&n%100<20))?1:2" -> n -> n == 1 ? 0 : ((n == 0 || (n % 100 > 0 && n % 100 < 20)) ? 1 : 2);

            // Three forms, special case for numbers ending in 1[2-9]
            // Lithuanian
            case "n%10==1&&n%100!=11?0:n%10>=2&&(n%100<10||n%100>=20)?1:2" -> n -> n % 10 == 1 && n % 100 != 11 ? 0 : (n % 10 >= 2 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);

            // Three forms, special cases for numbers ending in 1 and 2, 3, 4, except those ending in 1[1-4]
            // Belarusian, Bosnian, Croatian, Russian, Serbian, Ukrainian
            case "n%10==1&&n%100!=11?0:n%10>=2&&n%10<=4&&(n%100<10||n%100>=20)?1:2" -> n -> n % 10 == 1 && n % 100 != 11 ? 0 :
                    (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);

            // Alternative for the above, sometime encountered
            // Belarusian, Bosnian, Croatian, Russian, Serbian, Ukrainian
            case "n%10==1&&n%100!=11?0:n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?1:2" -> n -> n % 10 == 1 && n % 100 != 11 ? 0 :
                    (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 >= 14) ? 1 : 2);

            // Three forms, special cases for 1 and 2, 3, 4
            // Czech, Slovak
            case "(n==1)?0:(n>=2&&n<=4)?1:2", "n==1?0:(n>=2&&n<=4)?1:2", "n==1?0:n>=2&&n<=4?1:2" -> n -> n == 1 ? 0 : ((n >= 2 && n <= 4) ? 1 : 2);

            // Three forms, special case for one and some numbers ending in 2, 3, or 4
            // Polish
            case "n==1?0:n%10>=2&&n%10<=4&&(n%100<10||n%100>=20)?1:2" -> n -> n == 1 ? 0 : (n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 10 || n % 100 >= 20) ? 1 : 2);

            // Polish (alternative)
            case "n==1?0:n%10>=2&&n%10<=4&&(n%100<12||n%100>14)?1:2" -> n -> n == 1 ? 0 : n % 10 >= 2 && n % 10 <= 4 && (n % 100 < 12 || n % 100 > 14) ? 1 : 2;

            // Colognian
            case "n==0?0:n==1?1:2" -> n -> n == 0 ? 0 : n == 1 ? 1 : 2;

            // Langi
            case "n==0?0:(n==0||n==1)&&n!=0?1:2" -> n -> n == 0 ? 0 : (n == 0 || n == 1) && n != 0 ? 1 : 2;

            // Lithuanian
            case "n%10==1&&(n%100<11||n%100>19)?0:n%10>=2&&n%10<=9&&(n%100<11||n%100>19)?1:2" -> n -> n % 10 == 1 && (n % 100 < 11 || n % 100 > 19) ? 0 :
                    n % 10 >= 2 && n % 10 <= 9 && (n % 100 < 11 || n % 100 > 19) ? 1 : 2;

            // Latvian
            case "n%10==0||(n%100>=11&&n%100<=19)?0:n%10==1&&n%100!=11?1:2" -> n -> n % 10 == 0 || (n % 100 >= 11 && n % 100 <= 19) ? 0 : n % 10 == 1 && n % 100 != 11 ? 1 : 2;

            // Romanian, Moldavian
            case "n==1?0:n==0||(n!=1&&n%100>=1&&n%100<=19)?1:2" -> n -> n == 1 ? 0 : n == 0 || (n != 1 && n % 100 >= 1 && n % 100 <= 19) ? 1 : 2;
            case "n==0||n==1?0:n>=2&&n<=10?1:2" -> n -> n == 0 || n == 1 ? 0 : n >= 2 && n <= 10 ? 1 : 2;

            // Four forms, special case for one and all numbers ending in 02, 03, or 04
            // Slovenian
            case "n%100==1?0:n%100==2?1:n%100==3||n%100==4?2:3" -> n -> n % 100 == 1 ? 0 : (n % 100 == 2 ? 1 : (n % 100 == 3 || n % 100 == 4 ? 2 : 3));

            // Slovenian (alternative), Upper Sorbian
            case "n%100==1?0:n%100==2?1:n%100>=3&&n%100<=4?2:3" -> n -> n % 100 == 1 ? 0 : n % 100 == 2 ? 1 : n % 100 >= 3 && n % 100 <= 4 ? 2 : 3;

            // Manx
            case "n%10==1?0:n%10==2?1:n%100==0||n%100==20||n%100==40||n%100==60||n%100==80?2:3" -> n -> n % 10 == 1 ? 0 : n % 10 == 2 ? 1 :
                    n % 100 == 0 || n % 100 == 20 || n % 100 == 40 || n % 100 == 60 || n % 100 == 80 ? 2 : 3;

            // Hebrew
            case "n==1?0:n==2?1:n>10&&n%10==0?2:3" -> n -> n == 1 ? 0 : n == 2 ? 1 : n > 10 && n % 10 == 0 ? 2 : 3;

            // Scottish Gaelic
            case "n==1||n==11?0:n==2||n==12?1:(n>=3&&n<=10)||(n>=13&&n<=19)?2:3" -> n -> n == 1 || n == 11 ? 0 :
                    n == 2 || n == 12 ? 1 : (n >= 3 && n <= 10) || (n >= 13 && n <= 19) ? 2 : 3;

            // Maltese
            case "n==1?0:n==0||(n%100>=2&&n%100<=10)?1:n%100>=11&&n%100<=19?2:3" -> n -> n == 1 ? 0 :
                    n == 0 || (n % 100 >= 2 && n % 100 <= 10) ? 1 : n % 100 >= 11 && n % 100 <= 19 ? 2 : 3;

            // Breton (are you serious??)
            case "n%10==1&&n%100!=11&&n%100!=71&&n%100!=91?0:n%10==2&&n%100!=12&&n%100!=72&&n%100!=92?1:((n%10>=3&&n%10"
                    + "<=4)||n%10==9)&&(n%100<10||n%100>19)&&(n%100<70||n%100>79)&&(n%100<90||n%100>99)?2:n!=0&&n%10000"
                    + "00==0?3:4" -> n -> n % 10 == 1 && n % 100 != 11 && n % 100 != 71 && n % 100 != 91 ? 0 :
                    n % 10 == 2 && n % 100 != 12 && n % 100 != 72 && n % 100 != 92 ? 1 :
                            ((n % 10 >= 3 && n % 10 <= 4) || n % 10 == 9) && (n % 100 < 10 || n % 100 > 19)
                                    && (n % 100 < 70 || n % 100 > 79) && (n % 100 < 90 || n % 100 > 99) ? 2 :
                                    n != 0 && n % 1000000 == 0 ? 3 : 4;

            // Irish
            case "n==1?0:n==2?1:n>=3&&n<=6?2:n>=7&&n<=10?3:4" -> n -> n == 1 ? 0 : n == 2 ? 1 : n >= 3 && n <= 6 ? 2 : n >= 7 && n <= 10 ? 3 : 4;

            // Six forms, special cases for one, two, all numbers ending in 02, 03, … 10, all numbers ending in 11 … 99,
            // and others
            // Arabic
            case "n==0?0:n==1?1:n==2?2:n%100>=3&&n%100<=10?3:n%100>=11?4:5" -> n -> n == 0 ? 0 :
                    (n == 1 ? 1 : (n == 2 ? 2 : (n % 100 >= 3 && n % 100 <= 10 ? 3 : (n % 100 >= 11 ? 4 : 5))));
            case "n==0?0:n==1?1:n==2?2:n%100>=3&&n%100<=10?3:n%100>=11&&n%100<=99?4:5" -> n -> n == 0 ? 0 :
                    (n == 1 ? 1 : (n == 2 ? 2 :
                            (n % 100 >= 3 && n % 100 <= 10 ? 3 : (n % 100 >= 11 && n % 100 <= 99 ? 4 : 5))));
            case "n==0?0:n==1?1:n==2?2:n==3?3:n==6?4:5" -> n -> n == 0 ? 0 : n == 1 ? 1 : n == 2 ? 2 : n == 3 ? 3 : n == 6 ? 4 : 5;
            default -> null;
        };
    }

    /**
     * If the script is non-standard, we fallback on the buiilt-in Nashorn (available before Java 15).
     *
     * @return a Function to compute the plural index from the given count.
     */
    private Function<Long, Integer> formsFunctionFromNashorn() {
        return formsFunctionFromJSEngine("JavaScript");
    }

    /**
     * In Java 15+, we fallback on GraalVM.
     *
     * @return a Function to compute the plural index from the given count.
     */
    private Function<Long, Integer> formsFunctionFromGraalVM() {
        return formsFunctionFromJSEngine("graal.js");
    }

    /**
     * Generic computation from a JS engine.
     *
     * <p>The official documentation mentions that the plural determination script is in C format, but
     * the JavaScript format is the same for these scripts (containing only basic mathematics and
     * ternary operators), excepted for the booleans, but this case is handled manually.
     *
     * @return a Function to compute the plural index from the given count.
     */
    private Function<Long, Integer> formsFunctionFromJSEngine(final String engine) {
        final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(engine);
        if (scriptEngine == null) {
            return null;
        }

        return n -> {
            try {
                scriptEngine.put("n", n);
                Object rawPluralIndex = scriptEngine.eval(formsScript);

                // If the index is a boolean, as some po files use the C handling of booleans, we convert them
                // into the appropriate numbers.
                // Else, we try to convert the output to an integer.
                return rawPluralIndex instanceof Boolean ? (((Boolean) rawPluralIndex) ? 1 : 0) :
                        (rawPluralIndex instanceof Number ? ((Number) rawPluralIndex).intValue() :
                                Integer.valueOf(rawPluralIndex.toString()));
            } catch (ScriptException | NumberFormatException e) {
                ImageOnMap.getPlugin().getLogger().log(Level.SEVERE, "Invalid plural forms script “" + formsScript + "”", e);
                return 0;
            }
        };
    }

    /**
     * If nothing work, we fallback on hardcoded English rules.
     *
     * @return a Function to compute the plural index from the given count.
     */
    private Function<Long, Integer> formsFunctionFallback() {
        ImageOnMap.getPlugin().getLogger().warning(
                "Unknown plural rule “" + formsScript + "”; without JavaScript engine available, we'll fallback to English "
                        + "pluralization rules. If you want your language's plural rules supported without JavaScript "
                        + "engine, please open an issue with your language and its plural rules at "
                        + "https://github.com/zDevelopers/QuartzLib/issues."
        );

        return FORMS_FUNCTION_FALLBACK;
    }
}
