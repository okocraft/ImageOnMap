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

package fr.zcraft.quartzlib.tools.items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;


/**
 * Utility class to manipulate banners.
 * <p>Original repository : https://github.com/FlorianCassayre/BannersUtils</p>
 *
 * @author Florian Cassayre
 */
public final class TextualBanners {
    /**
     * The map containing all the supported characters. White color represents the background and
     * black color the font color.
     */
    private static final Map<Character, BannerMeta> chars = new HashMap<>();

    static {
        chars.put('a', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_TOP, PatternType.STRIPE_LEFT, PatternType.STRIPE_RIGHT,
                        PatternType.STRIPE_MIDDLE)));
        chars.put('b', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_TOP, PatternType.STRIPE_LEFT, PatternType.STRIPE_RIGHT,
                        PatternType.STRIPE_MIDDLE, PatternType.STRIPE_BOTTOM)));
        chars.put('c', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_LEFT, PatternType.STRIPE_TOP,
                        PatternType.STRIPE_BOTTOM)));
        chars.put('d', getBannerMeta(DyeColor.BLACK,
                Arrays.asList(new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT))));
        chars.put('e', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT))));
        chars.put('f', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('g', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('h', getBannerMeta(DyeColor.BLACK, Arrays.asList(new Pattern(DyeColor.WHITE, PatternType.STRIPE_TOP),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT))));
        chars.put('i', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_BOTTOM, PatternType.STRIPE_TOP,
                        PatternType.STRIPE_CENTER)));
        chars.put('j', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT))));
        chars.put('k', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT))));
        chars.put('l', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_LEFT, PatternType.STRIPE_BOTTOM)));
        chars.put('m', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP),
                        new Pattern(DyeColor.WHITE, PatternType.TRIANGLES_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT))));
        chars.put('n', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_TOP),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT))));
        chars.put('o', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_TOP, PatternType.STRIPE_RIGHT, PatternType.STRIPE_BOTTOM,
                        PatternType.STRIPE_LEFT)));
        chars.put('p', getBannerMeta(DyeColor.BLACK,
                Arrays.asList(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT))));
        chars.put('q', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('r', getBannerMeta(DyeColor.BLACK,
                Arrays.asList(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_VERTICAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE))));
        chars.put('s', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_TOP_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_BOTTOM_LEFT),
                        new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT))));
        chars.put('t', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_CENTER, PatternType.STRIPE_TOP)));
        chars.put('u', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_BOTTOM, PatternType.STRIPE_RIGHT,
                        PatternType.STRIPE_LEFT)));
        chars.put('v', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.TRIANGLE_BOTTOM),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT))));
        chars.put('w', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM),
                        new Pattern(DyeColor.WHITE, PatternType.TRIANGLES_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT))));
        chars.put('x', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT, PatternType.STRIPE_DOWNRIGHT)));
        chars.put('y', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_VERTICAL_MIRROR),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT))));
        chars.put('z', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_TOP_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_BOTTOM_RIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT))));

        chars.put('0', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_TOP, PatternType.STRIPE_RIGHT, PatternType.STRIPE_BOTTOM,
                        PatternType.STRIPE_LEFT)));
        chars.put('1', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.SQUARE_TOP_LEFT),
                        new Pattern(DyeColor.WHITE, PatternType.BORDER),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_CENTER))));
        chars.put('2', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_TOP),
                        new Pattern(DyeColor.BLACK, PatternType.TRIANGLE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_TOP_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.SQUARE_BOTTOM_RIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.RHOMBUS_MIDDLE),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT))));
        chars.put('3', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('4', getBannerMeta(DyeColor.BLACK,
                Arrays.asList(new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.WHITE, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE))));
        chars.put('5', getBannerMeta(DyeColor.BLACK,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.HALF_VERTICAL_MIRROR),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.WHITE, PatternType.DIAGONAL_RIGHT_MIRROR),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('6', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT),
                        new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_BOTTOM),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                        new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP))));
        chars.put('7', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                new Pattern(DyeColor.WHITE, PatternType.DIAGONAL_RIGHT),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT))));
        chars.put('8', getBannerMeta(DyeColor.WHITE,
                getPatterns(DyeColor.BLACK, PatternType.STRIPE_TOP, PatternType.STRIPE_LEFT, PatternType.STRIPE_RIGHT,
                        PatternType.STRIPE_MIDDLE, PatternType.STRIPE_BOTTOM)));
        chars.put('9', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.HALF_HORIZONTAL_MIRROR),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_TOP),
                new Pattern(DyeColor.BLACK, PatternType.STRIPE_RIGHT))));

        chars.put('.', getBannerMeta(DyeColor.WHITE,
                Collections.singletonList(new Pattern(DyeColor.BLACK, PatternType.SQUARE_BOTTOM_LEFT))));
        chars.put(' ', getBannerMeta(DyeColor.WHITE, new ArrayList<Pattern>()));
        chars.put('-', getBannerMeta(DyeColor.WHITE,
                Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.STRIPE_MIDDLE),
                        new Pattern(DyeColor.WHITE, PatternType.BORDER))));
        chars.put(':', getBannerMeta(DyeColor.BLACK,
                getPatterns(DyeColor.WHITE, PatternType.STRIPE_TOP, PatternType.STRIPE_LEFT, PatternType.STRIPE_RIGHT,
                        PatternType.STRIPE_MIDDLE, PatternType.STRIPE_BOTTOM)));
        chars.put(';', getBannerMeta(DyeColor.BLACK,
                getPatterns(DyeColor.WHITE, PatternType.STRIPE_DOWNRIGHT, PatternType.STRIPE_LEFT,
                        PatternType.STRIPE_TOP, PatternType.STRIPE_MIDDLE, PatternType.BORDER)));

        chars.put('[', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.BORDER),
                new Pattern(DyeColor.BLACK, PatternType.HALF_VERTICAL),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT))));
        chars.put(']', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.BORDER),
                new Pattern(DyeColor.BLACK, PatternType.HALF_VERTICAL_MIRROR),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_LEFT),
                new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT))));
        chars.put('/', getBannerMeta(DyeColor.WHITE, getPatterns(DyeColor.BLACK, PatternType.STRIPE_DOWNLEFT)));
        chars.put('\\', getBannerMeta(DyeColor.WHITE, getPatterns(DyeColor.BLACK, PatternType.STRIPE_DOWNRIGHT)));
        chars.put('*', getBannerMeta(DyeColor.WHITE, Arrays.asList(new Pattern(DyeColor.BLACK, PatternType.CROSS),
                new Pattern(DyeColor.BLACK, PatternType.STRAIGHT_CROSS),
                new Pattern(DyeColor.WHITE, PatternType.BORDER),
                new Pattern(DyeColor.WHITE, PatternType.CURLY_BORDER))));
    }

    /**
     * Prevents the class to be instantiated.
     */
    private TextualBanners() {
    }

    /**
     * Return a list of patterns with the same color.
     *
     * @param color The generic color that will be used
     * @param types The layers to be printed on the banner using the chosen color
     * @return A copy of this banner
     */
    public static List<Pattern> getPatterns(DyeColor color, PatternType... types) {
        List<Pattern> patterns = new ArrayList<>();

        for (PatternType type : types) {
            patterns.add(new Pattern(color, type));
        }

        return patterns;
    }

    /**
     * Returns a banner containing the patterns with the background color.
     *
     * @param color    The background color
     * @param patterns The layers to be printed on the banner
     * @return An {@link ItemStack} containing these parameters
     */
    public static ItemStack getBanner(DyeColor color, List<Pattern> patterns) {
        ItemStack banner = new ItemStack(Material.WHITE_BANNER);
        banner.setItemMeta(getBannerMeta(color, patterns));
        return banner;
    }

    /**
     * Returns a banner meta corresponding to these parameters.
     *
     * @param color    The background color
     * @param patterns The layers to be printed on the banner
     * @return An ItemMeta containing these parameters
     */
    public static BannerMeta getBannerMeta(DyeColor color, List<Pattern> patterns) {
        BannerMeta meta = (BannerMeta) new ItemStack(Material.WHITE_BANNER).getItemMeta();
        meta.setBaseColor(color);
        meta.setPatterns(patterns);
        return meta;
    }

    /**
     * Returns a banner item stack which represents the following character, with the specified
     * color, and possibly a border.
     *
     * @param c      The character to be printed
     * @param color  The font color
     * @param border If set to {@code true}, then a border will be applied to the banner
     * @return A banner {@link ItemStack} with the corresponding character using a basic font system
     * @throws IllegalArgumentException if the character isn't registered.
     */
    public static ItemStack getCharBanner(char c, DyeColor color, boolean border) {
        return getCharBanner(c, getBackgroundColorFor(color), color, border);
    }

    /**
     * Returns a banner item stack which represents the following character, with a specified
     * background and the specified color, and possibly a border. Throws IllegalArgumentException if
     * the character isn't registered.
     *
     * @param c          The character to be printed
     * @param background The background color
     * @param color      The font color
     * @param border     If set to {@code true}, then a border will be applied to the banner
     * @return A banner {@link ItemStack} with the corresponding character using a basic font system
     */
    public static ItemStack getCharBanner(char c, DyeColor background, DyeColor color, boolean border) {
        BannerMeta meta = chars.get(Character.toLowerCase(c));

        if (meta == null) {
            throw new IllegalArgumentException("This character can't be reproduced on a banner !");
        }

        List<Pattern> patterns = meta.getPatterns();

        patterns = clonePatterns(patterns);

        for (int i = 0; i < patterns.size(); i++) {
            DyeColor patternColor = patterns.get(i).getColor().equals(DyeColor.BLACK) ? color : background;
            patterns.set(i, new Pattern(patternColor, patterns.get(i).getPattern()));
        }

        if (border) {
            patterns.add(new Pattern(background, PatternType.BORDER));
        }

        return getBanner(meta.getBaseColor().equals(DyeColor.WHITE) ? background : color, patterns);
    }

    /**
     * Returns a banner item stack which represents the following digit, with the specified color,
     * and possibly a border.
     *
     * @param i      The digit to be printed
     * @param color  The font color
     * @param border If set to {@code true}, then a border will be applied to the banner
     * @return A banner {@link ItemStack} with the corresponding digit using a basic font system
     * @throws IllegalArgumentException if the character isn't registered.
     */
    public static ItemStack getDigitBanner(int i, DyeColor color, boolean border) {
        return getDigitBanner(i, getBackgroundColorFor(color), color, border);
    }

    /**
     * Returns a banner item stack which represents the following digit, with a specified background
     * and the specified color, and possibly a border.
     *
     * @param i          The digit to be printed
     * @param background The background color
     * @param color      The font color
     * @param border     If set to {@code true}, then a border will be applied to the banner
     * @return A banner {@link ItemStack} with the corresponding digit using a basic font system
     * @throws IllegalArgumentException if the character isn't registered.
     */
    public static ItemStack getDigitBanner(int i, DyeColor background, DyeColor color, boolean border) {
        if (i < 0 || i > 9) {
            throw new IllegalArgumentException("i must be a digit !");
        }
        return getCharBanner((char) (i + 48), background, color, border);
    }

    /**
     * Returns a list of item stacks which represents the following string, with the specified
     * color, and possibly a border.
     *
     * @param string The string
     * @param color  The font color
     * @param border If set to {@code true}, then a border will be applied to the banner
     * @return A list of banners {@link ItemStack} representing the corresponding string using a
     *     basic font system.
     * @throws IllegalArgumentException if a character in the string isn't registered.
     */
    public static List<ItemStack> getStringBanners(String string, DyeColor color, boolean border) {
        return getStringBanners(string, getBackgroundColorFor(color), color, border);
    }

    /**
     * Returns a list of item stacks which represents the following string, with a specified
     * background and the specified color, and possibly a border. Throws IllegalArgumentException if
     * a character in the string isn't registered.
     *
     * @param string     The string
     * @param background The background color
     * @param color      The font color
     * @param border     If set to {@code true}, then a border will be applied to the banner
     * @return A list of banners {@link ItemStack} representing the corresponding string using a
     *     basic font system.
     * @throws IllegalArgumentException if a character in the string isn't registered.
     */
    public static List<ItemStack> getStringBanners(String string, DyeColor background, DyeColor color, boolean border) {
        List<ItemStack> items = new ArrayList<>();
        for (char c : string.toCharArray()) {
            try {
                items.add(getCharBanner(c, background, color, border));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("The character '" + c + "' can't be reproduced on a banner !");
            }
        }

        return items;
    }

    @Deprecated
    private static List<Pattern> clonePatterns(List<Pattern> list) {
        List<Pattern> newList = new ArrayList<>();
        for (Pattern pattern : list) {
            newList.add(new Pattern(pattern.getColor(), pattern.getPattern()));
        }
        return newList;
    }

    /**
     * Returns a correct background color for the given foreground color.
     *
     * @param foregroundColor the foreground color
     * @return a background color.
     */
    private static DyeColor getBackgroundColorFor(DyeColor foregroundColor) {
        switch (foregroundColor) {
            case WHITE:
            case YELLOW:
            case PINK:
                return DyeColor.BLACK;

            default:
                return DyeColor.WHITE;
        }
    }
}
