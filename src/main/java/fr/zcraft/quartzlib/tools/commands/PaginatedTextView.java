/*
 * Copyright or © or Copr. AmauryCarrade (2015)
 *
 * http://amaury.carrade.eu
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

package fr.zcraft.quartzlib.tools.commands;

import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.components.rawtext.RawTextPart;
import fr.zcraft.quartzlib.tools.text.RawMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


/**
 * An utility to send paginated chat views to players, mainly for commands.
 *
 * @param <T> Data type to display.
 */
public abstract class PaginatedTextView<T> {
    public static final int DEFAULT_LINES_IN_NON_EXPANDED_CHAT_VIEW = 10;
    public static final int DEFAULT_LINES_IN_EXPANDED_CHAT_VIEW = 20;

    private T[] data;
    // items minus one header line minus pagination links
    private int itemsPerPage = DEFAULT_LINES_IN_NON_EXPANDED_CHAT_VIEW - 2;

    private int currentPage;
    private int pagesCount;

    private boolean doNotPaginateForConsole = true;



    /* ========== User configuration ========== */


    /**
     * Sets the data to display.
     *
     * @param data The data.
     * @return Instance for chaining.
     */
    public PaginatedTextView<T> setData(final T[] data) {
        this.data = data;
        recalculatePagination();

        return this;
    }

    /**
     * Sets the amount of items per page.
     * <p>The default is the number of lines visible in a non-expanded chat window, by default, minus
     * 2 (for header and footer).</p>
     *
     * @param itemsPerPage The amount of items displayed per page.
     * @return Instance for chaining.
     */
    public PaginatedTextView<T> setItemsPerPage(final int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        recalculatePagination();

        return this;
    }

    /**
     * Sets the current page to be displayed.
     *
     * @param page The page.
     * @return Instance for chaining.
     */
    public PaginatedTextView<T> setCurrentPage(final int page) {
        if (page < 1) {
            currentPage = 1;
        } else {
            currentPage = Math.min(page, pagesCount);
        }

        return this;
    }

    /**
     * Sets if the console should receive a paginated view too. Defaults to true (displays the whole list at once).
     *
     * @param noPaginationForConsole {@code true} to disable pagination for console.
     * @return Instance for chaining.
     */
    public PaginatedTextView<T> setDoNotPaginateForConsole(boolean noPaginationForConsole) {
        this.doNotPaginateForConsole = noPaginationForConsole;
        return this;
    }

    /**
     * Displays the paginated view page, as configured.
     *
     * @param receiver The receiver of the text view.
     */
    public void display(CommandSender receiver) {
        int from;
        int to;

        if (!doNotPaginateForConsole || receiver instanceof Player) {
            from = ((currentPage - 1) * itemsPerPage);
            to = Math.min(from + itemsPerPage, data.length);
        } else {
            from = 0;
            to = data.length;
        }

        displayHeader(receiver);

        for (int i = from; i < to; i++) {
            displayItem(receiver, data[i]);
        }

        displayFooter(receiver);
    }



    /* ========== Overrider & internal utilities ========== */


    /**
     * Gets the data.
     * @return The data, for use in the overridden methods.
     */
    protected T[] data() {
        return data;
    }

    /**
     * Gets the amount of items per page.
     * @return The amount of items per page, for use in the overridden methods.
     */
    protected int itemsPerPage() {
        return itemsPerPage;
    }

    /**
     * Gets the current page.
     * @return The current page, for use in the overridden methods.
     */
    protected int currentPage() {
        return currentPage;
    }

    /**
     * Gets the pages count.
     * @return The pages count, for use in the overridden methods.
     */
    protected int pagesCount() {
        return pagesCount;
    }

    /**
     * Recalculates the page count based on the data length and the items per page.
     */
    private void recalculatePagination() {
        pagesCount = (int) Math.ceil(((double) data.length) / ((double) itemsPerPage));
    }



    /* ========== Methods to override ========== */


    /**
     * Displays a header.
     * <p>This method is called on every page before the items are displayed.
     * You can access the view's properties through the protected methods like
     * {@link #currentPage()} or {@link #pagesCount()}.</p>
     * <p>If this method is not overridden, no header will be displayed.</p>
     *
     * @param receiver The receiver of the paginated view.
     */
    protected void displayHeader(CommandSender receiver) {
    }

    /**
     * Displays an item.
     * <p>This method will be called for each displayed item.</p>
     *
     * @param receiver The receiver of the paginated view.
     * @param item     The item to be displayed.
     */
    protected abstract void displayItem(CommandSender receiver, T item);

    /**
     * Displays a footer.
     * <p>This method is called on every page when the items are displayed, so anything displayed inside will be shown
     * after. You can access the view's properties through the protected methods like {@link #currentPage()} or
     * {@link #pagesCount()}.</p>
     * <p>If this method is not overridden, the default implementation will print pagination links: a “previous” link,
     * if we're not in the first page, the current page, and a “next” link if we're not on the last page.</p>
     *
     * @param receiver The receiver of the paginated view.
     * @see #getCommandToPage(int) Method to override to fully use the default footer.
     */
    protected void displayFooter(CommandSender receiver) {
        if (pagesCount <= 1 || (doNotPaginateForConsole && !(receiver instanceof Player))) {
            return;
        }

        RawTextPart<?> footer = new RawText("");

        if (currentPage > 1) {
            String command = getCommandToPage(currentPage - 1);
            if (command != null) {
                footer = footer.then("« Previous")
                        .color(ChatColor.GRAY)
                        .command(command)
                        .hover(new RawText("Go to page " + (currentPage - 1)))

                        .then(" ⋅ ").color(ChatColor.GRAY);
            }
        }

        footer.then("Page " + currentPage + " of " + pagesCount).color(ChatColor.GRAY).style(ChatColor.BOLD);

        if (currentPage < pagesCount) {
            String command = getCommandToPage(currentPage + 1);
            if (command != null) {
                footer = footer.then(" ⋅ ").color(ChatColor.GRAY)

                        .then("Next »")
                        .color(ChatColor.GRAY)
                        .command(command)
                        .hover(new RawText("Go to page " + (currentPage + 1)));
            }
        }

        RawMessage.send(receiver, footer.build());
    }

    /**
     * Returns the command to be executed by the player to access the nth page, or {@code null} if not applicable.
     * <p>If you use the default footer, you should override this method. If this returns {@code null},
     * links to previous and next pages will not be displayed.</p>
     *
     * @param page The page.
     * @return The command to be executed to display the page.
     */
    protected String getCommandToPage(int page) {
        return null;
    }
}
