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


package fr.zcraft.quartzlib.components.rawtext;

import com.google.common.base.CaseFormat;
import fr.zcraft.quartzlib.components.commands.Command;
import fr.zcraft.quartzlib.components.commands.Commands;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemUtils;
import fr.zcraft.quartzlib.tools.reflection.NMSException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.ChatColor;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.json.simple.JSONArray;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;

public abstract class RawTextPart<T extends RawTextPart<T>> implements Iterable<RawTextPart>, JSONAware {
    private final RawTextPart parent;
    private final ArrayList<RawTextPart> extra = new ArrayList<>();
    private String text;
    private boolean translate = false;
    private ChatColor color;
    //Text styles
    private boolean bold = false;
    private boolean italic = false;
    private boolean underline = false;
    private boolean obfuscated = false;
    private boolean strikethrough = false;
    //Other
    private ActionClick actionClick = null;
    private String actionClickValue = null;
    private ActionHover actionHover = null;
    private Object actionHoverValue = null;
    private String insertion = null;

    RawTextPart() {
        this(null);
    }

    RawTextPart(String text) {
        this(text, null);
    }


    RawTextPart(String text, RawTextPart parent) {
        this.text = text;
        this.parent = parent;
    }

    /**
     * Converts an action to the corresponding JSON.
     *
     * @param action The action to be converted.
     * @param value  The action value.
     * @return A JSON object corresponding to the action.
     */
    private static JSONObject actionToJson(Enum action, Object value) {
        JSONObject obj = new JSONObject();
        obj.put("action", action.name().toLowerCase());
        obj.put("value", value);
        return obj;
    }

    /**
     * Starts a new text component with no predefined text.
     *
     * @return A new raw text component linked to the previous one, usable like a chain.
     */
    public RawTextPart then() {
        return then(null);
    }

    /**
     * Starts a new text component with a predefined text.
     *
     * @return A new raw text component linked to the previous one, usable like a chain.
     */
    public RawTextPart then(String text) {
        RawTextPart root = getRoot();
        RawTextPart newPart = new RawTextSubPart(text, root);

        root.extra.add(newPart);
        return newPart;
    }

    /**
     * Sets the text of this component.
     *
     * @param text The text to set.
     * @return The current raw text component, for method chaining.
     */
    public T text(String text) {
        this.text = text;
        this.translate = false;

        return (T) this;
    }

    /**
     * Sets the text to be a translation key. This have to be a valid key in the Minecraft translation files.
     *
     * @param text The translation key.
     * @return The current raw text component, for method chaining.
     */
    public T translate(String text) {
        this.text = text;
        this.translate = true;

        return (T) this;
    }

    /**
     * Sets the text to be the translated name of an item.
     *
     * @param item The item to take the name from.
     * @return The current raw text component, for method chaining.
     */
    public T translate(ItemStack item) {
        String trName;

        try {
            trName = ItemUtils.getI18nName(item);
        } catch (NMSException ex) {
            PluginLogger.warning("Exception while retreiving item i18n key", ex);
            trName = "item." + item.getType().toString().toLowerCase() + ".name";
        }

        return translate(trName);
    }

    /**
     * Sets the color of this text component.
     *
     * @param color The color. It should be a color (not a style). Can be set only once.
     * @return The current raw text component, for method chaining.
     * @see #style(ChatColor) Method to set a style (bold...).
     */
    public T color(ChatColor color) {
        if (this.color != null) {
            throw new IllegalStateException("Color already set.");
        }
        if (color == null) {
            return (T) this;
        }
        if (!color.isColor()) {
            throw new IllegalArgumentException("Invalid color.");
        }
        this.color = color;

        return (T) this;
    }

    /**
     * Add a style to this text component.
     *
     * @param style The style to add. If it's a color and a color was previously set, an error will be thrown.
     * @return The current raw text component, for method chaining.
     */
    public T style(ChatColor style) {
        if (style == null) {
            return (T) this;
        }
        if (style.isColor()) {
            return this.color(style);
        }

        switch (style) {
            case BOLD:
                bold = true;
                break;
            case ITALIC:
                italic = true;
                break;
            case STRIKETHROUGH:
                strikethrough = true;
                break;
            case UNDERLINE:
                underline = true;
                break;
            case MAGIC:
                obfuscated = true;
                break;
            default:
                throw new IllegalArgumentException("Invalid style: " + style.name());
        }

        return (T) this;
    }

    /**
     * Adds multiple styles at once to this text component. A color color may be in the list,
     *   but only once (else, or if the color was previously set, an error will be thrown).
     *
     * @param styles The styles to add.
     * @return The current raw text component, for method chaining.
     */
    public T style(ChatColor... styles) {
        for (ChatColor style : styles) {
            style(style);
        }
        return (T) this;
    }

    /**
     * Adds multiple styles at once to this text component. A color color may be in the list,
     *     but only once (else, or if the color was previously set, an error will be thrown).
     *
     * @param styles The styles to add.
     */
    public void style(Iterable<ChatColor> styles) {
        for (ChatColor style : styles) {
            style(style);
        }
    }

    /**
     * Adds an hover event to this text component. (Internal use.)
     *
     * @param action The hover action to use.
     * @param object The argument given to the hover action.
     * @return The current raw text component, for method chaining.
     */
    private T hover(ActionHover action, Object object) {
        if (actionHover != null) {
            throw new IllegalStateException("Hover action " + actionHover.name() + " has already been set.");
        }
        actionHover = action;
        actionHoverValue = object;
        return (T) this;
    }

    /**
     * Adds an hover text to this component.
     *
     * @param hoverText The text to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(String hoverText) {
        return hover(new RawText(hoverText));
    }

    /**
     * Adds an hover text to this component.
     *
     * @param hoverText The text component to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(RawTextPart hoverText) {
        return hover(ActionHover.SHOW_TEXT, hoverText.build());
    }

    /**
     * Adds an hover advancement to this component.
     *
     * @param advancement The advancement to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(Advancement advancement) {
        return hover(ActionHover.SHOW_ADVANCEMENT, "advancement." + advancement.getKey());
    }

    /**
     * Adds an hover statistic to this component.
     *
     * @param statistic The statistic to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(Statistic statistic) {
        return hover(ActionHover.SHOW_ADVANCEMENT, "stat." + enumCamel(statistic));
    }

    /**
     * Adds an hover item to this component.
     *
     * @param item The item to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(ItemStack item) {
        return hover(ActionHover.SHOW_ITEM, RawText.toJSONString(item));
    }

    /**
     * Adds an hover entity to this component.
     *
     * @param entity The entity to display on hover.
     * @return The current raw text component, for method chaining.
     */
    public T hover(Entity entity) {
        return hover(ActionHover.SHOW_ENTITY, RawText.toJSON(entity).toJSONString());
    }

    /**
     * Adds a click event to this component. (Internal use.)
     *
     * @param action The click action to add.
     * @param value  The argument given to the click action.
     * @return The current raw text component, for method chaining.
     */
    private T click(ActionClick action, String value) {
        if (actionClick != null) {
            throw new IllegalStateException("Hover action " + actionClick.name() + " has already been set.");
        }
        actionClick = action;
        actionClickValue = value;
        return (T) this;
    }

    /**
     * Adds a command executed when this text component is clicked.
     *
     * @param command The command to execute on click.
     * @return The current raw text component, for method chaining.
     */
    public T command(String command) {
        return click(ActionClick.RUN_COMMAND, command);
    }

    /**
     * Adds a command executed when this text component is clicked.
     *
     * @param command The command class to execute on click.
     * @param args    The arguments to pass to the command.
     * @return The current raw text component, for method chaining.
     */
    public T command(Class<? extends Command> command, String... args) {
        Command commandInfo = Commands.getCommandInfo(command);
        if (commandInfo == null) {
            throw new IllegalArgumentException("Unknown command");
        }
        return command(commandInfo.build(args));
    }

    /**
     * Adds an URI to be opened when this text component is clicked.
     *
     * @param uri The URI to open on click.
     * @return The current raw text component, for method chaining.
     * @throws URISyntaxException If the URI is invalid.
     */
    public T uri(String uri) throws URISyntaxException {
        return uri(new URI(uri));
    }

    /**
     * Adds an URI to be opened when this text component is clicked.
     *
     * @param uri The URI to open on click.
     * @return The current raw text component, for method chaining.
     */
    public T uri(URI uri) {
        return click(ActionClick.OPEN_URL, uri.toString());
    }

    /**
     * Adds a text to be suggested on click, i.e. the text will be placed into the player's chat when clicked.
     *
     * @param suggestion The text to be suggested (e.g. a command) on click.
     * @return The current raw text component, for method chaining.
     */
    public T suggest(String suggestion) {
        return click(ActionClick.SUGGEST_COMMAND, suggestion);
    }

    /**
     * Adds a command to be suggested on click, i.e. the command will be placed
     * into the player's chat when clicked.
     *
     * @param command The command class to execute on click.
     * @param args    The arguments to pass to the command.
     * @return The current raw text component, for method chaining.
     */
    public T suggest(Class<? extends Command> command, String... args) {
        Command commandInfo = Commands.getCommandInfo(command);
        if (commandInfo == null) {
            throw new IllegalArgumentException("Unknown command");
        }
        return click(ActionClick.SUGGEST_COMMAND, commandInfo.build(args));
    }

    /**
     * Adds a text to be inserted on shift-click, i.e. the text will be appended
     * to the player's chat when shift-clicked.
     *
     * @param insertion The text to be inserted (e.g. a command) on
     *                  shift-click.
     * @return The current raw text component, for method chaining.
     */
    public T insert(String insertion) {
        this.insertion = insertion;
        return (T) this;
    }

    /**
     * Adds a command to be inserted on shift-click, i.e. the command will be
     * appended to the player's chat when shift-clicked.
     *
     * @param command The command class to execute on click.
     * @param args    The arguments to pass to the command.
     * @return The current raw text component, for method chaining.
     */
    public T insert(Class<? extends Command> command, String... args) {
        Command commandInfo = Commands.getCommandInfo(command);
        if (commandInfo == null) {
            throw new IllegalArgumentException("Unknown command");
        }
        return insert(commandInfo.build(args));
    }

    /**
     * Builds this chain of components into a {@link RawText} ready to be used.
     *
     * @return A {@link RawText} component containing all components in the chain.
     */
    public RawText build() {
        if (parent != null) {
            return parent.build();
        }
        if (this instanceof RawText) {
            return (RawText) this;
        }
        throw new RuntimeException("Dangling non-root text part");
    }

    /**
     * @return The root component of the chain.
     */
    private RawTextPart getRoot() {
        if (parent != null) {
            return parent.getRoot();
        }
        return this;
    }

    /**
     * Converts the current raw text component to JSON.
     *
     * @return A JSON object corresponding to this raw text component.
     */
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        if (translate) {
            obj.put("translate", text);
        } else {
            obj.put("text", text);
        }

        if (!extra.isEmpty()) {
            JSONArray extraArray = new JSONArray();
            for (RawTextPart childPart : this) {
                extraArray.add(childPart.toJSON());
            }
            obj.put("extra", extraArray);
        }

        if (color != null) {
            obj.put("color", RawText.toStyleName(color));
        }

        if (bold) {
            obj.put("bold", true);
        }
        if (italic) {
            obj.put("italic", true);
        }
        if (underline) {
            obj.put("underlined", true);
        }
        if (strikethrough) {
            obj.put("strikethrough", true);
        }
        if (obfuscated) {
            obj.put("obfuscated", true);
        }

        if (actionClick != null && actionClickValue != null) {
            obj.put("clickEvent", actionToJson(actionClick, actionClickValue));
        }

        if (actionHover != null && actionHoverValue != null) {
            obj.put("hoverEvent", actionToJson(actionHover, actionHoverValue));
        }

        if (insertion != null) {
            obj.put("insertion", insertion);

            // Fix for MC-82425 (MC <= 1.9.2; fixed in 16w21a)
            // @see https://bugs.mojang.com/browse/MC-82425
            // If insertion is the only one in its component, we need to add another one to have it taken into account.
            if (!bold && !italic && !underline && !strikethrough && !obfuscated && color == null
                    && actionClick == null && actionHover == null) {
                obj.put("bold", false);
            }
        }

        return obj;
    }

    /**
     * Returns a  JSON representation of this raw text component.
     * @return A JSON representation of this raw text component.
     */
    @Override
    public String toJSONString() {
        return toJSON().toJSONString();
    }

    /**
     * Returns this text component but as plain text, with all styles and events stripped.
     * @return This text component but as plain text, with all styles and events stripped.
     */
    public String toPlainText() {
        StringBuilder buf = new StringBuilder();
        writePlainText(buf);
        return buf.toString();
    }

    private void writePlainText(StringBuilder buf) {
        buf.append(text);

        for (RawTextPart subPart : this) {
            subPart.writePlainText(buf);
        }
    }

    /**
     * Converts an enum name to lower camel case.
     *
     * @param enumValue The enum value to convert.
     * @return The converted name.
     */
    private String enumCamel(Enum enumValue) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, enumValue.toString());
    }

    /**
     * Returns this text component as formatted text, with styles converted to Minecraft formatting marks,
     *     and all other events stripped.
     * @return This text component as formatted text.
     */
    public String toFormattedText() {
        StringBuilder buf = new StringBuilder();
        writeFormattedText(buf);
        return buf.toString();
    }

    private void writeFormattedText(StringBuilder buf) {
        if (color != null) {
            buf.append(color);
        }
        if (bold) {
            buf.append(ChatColor.BOLD);
        }
        if (italic) {
            buf.append(ChatColor.ITALIC);
        }
        if (underline) {
            buf.append(ChatColor.UNDERLINE);
        }
        if (strikethrough) {
            buf.append(ChatColor.STRIKETHROUGH);
        }
        if (obfuscated) {
            buf.append(ChatColor.MAGIC);
        }

        buf.append(text);
        buf.append(ChatColor.RESET);

        for (RawTextPart subPart : this) {
            subPart.writeFormattedText(buf);
        }

    }

    @Override
    public Iterator<RawTextPart> iterator() {
        return extra.iterator();
    }

    private enum ActionClick {
        OPEN_URL,
        RUN_COMMAND,
        SUGGEST_COMMAND
    }

    private enum ActionHover {
        SHOW_TEXT,
        SHOW_ADVANCEMENT,
        SHOW_ITEM,
        SHOW_ENTITY
    }
}
