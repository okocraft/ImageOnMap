/*
 * Plugin UHCReloaded : Alliances
 *
 * Copyright ou © ou Copr. Amaury Carrade (2016)
 * Idées et réflexions : Alexandre Prokopowicz, Amaury Carrade, "Vayan".
 *
 * Ce logiciel est régi par la licence CeCILL soumise au droit français et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL telle que diffusée par le CEA, le CNRS et l'INRIA
 * sur le site "http://www.cecill.info".
 *
 * En contrepartie de l'accessibilité au code source et des droits de copie,
 * de modification et de redistribution accordés par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limitée.  Pour les mêmes raisons,
 * seule une responsabilité restreinte pèse sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les concédants successifs.
 *
 * A cet égard  l'attention de l'utilisateur est attirée sur les risques
 * associés au chargement,  à l'utilisation,  à la modification et/ou au
 * développement et à la reproduction du logiciel par l'utilisateur étant
 * donné sa spécificité de logiciel libre, qui peut le rendre complexe à
 * manipuler et qui le réserve donc à des développeurs et des professionnels
 * avertis possédant  des  connaissances  informatiques approfondies.  Les
 * utilisateurs sont donc invités à charger  et  tester  l'adéquation  du
 * logiciel à leurs besoins dans des conditions permettant d'assurer la
 * sécurité de leurs systèmes et ou de leurs données et, plus généralement,
 * à l'utiliser et l'exploiter dans les mêmes conditions de sécurité.
 *
 * Le fait que vous puissiez accéder à cet en-tête signifie que vous avez
 * pris connaissance de la licence CeCILL, et que vous en avez accepté les
 * termes.
 */

package fr.zcraft.quartzlib.tools.mojang;

import com.google.common.base.CaseFormat;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

/**
 * Easier access to Mojang's “Marc's Head Format” Heads, e.g. for use in GUIs.
 *
 * <p>See https://minecraft.gamepedia.com/Head#Mojang_Studios_skins for the complete list.</p>
 */
public enum MojangHead {
    /* Mobs */
    ALEX,
    BLAZE,
    CAVE_SPIDER,
    CHICKEN,
    COW,
    CREEPER,
    ENDERMAN,
    GHAST,
    GOLEM,
    HEROBRINE,
    LAVA_SLIME,
    MUSHROOM_COW,
    OCELOT,
    PIG,
    PIG_ZOMBIE,
    SHEEP,
    SKELETON,
    SLIME,
    SPIDER,
    SQUID,
    STEVE,
    VILLAGER,
    WITHER_SKELETON("MHF_WSkeleton"),
    ZOMBIE,

    /* Blocks */
    CACTUS,
    CAKE,
    CHEST,
    COCONUT_BROWN("MHF_CoconutB"),
    COCONUT_GREEN("MHF_CoconutG"),
    MELON,
    OAK_LOG,
    PRESENT("MHF_Present1"),
    PRESENT_2,
    PUMPKIN,
    TNT("MHF_TNT"),
    TNT_2("MHF_TNT2"),

    /* Bonus */
    ARROW_UP,
    ARROW_DOWN,
    ARROW_LEFT,
    ARROW_RIGHT,
    EXCLAMATION,
    QUESTION;


    private final String headName;

    MojangHead() {
        this.headName = "MHF_" + CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
    }

    MojangHead(final String headName) {
        this.headName = headName;
    }

    /**
     * Returns the head's name.
     * @return The Mojang head's name, to be used as skull owner.
     */
    public String getHeadName() {
        return headName;
    }

    /**
     * Returns the head as an ItemStack.
     * @return The head as an ItemStack (of type {@link Material#PLAYER_HEAD}).
     */
    public ItemStack asItem() {
        return asItemBuilder().item();
    }

    /**
     * Returns the head as an ItemStackBuilder.
     * @return The head as an {@link ItemStackBuilder}, ready to be completed.
     */
    // Silence the setOwner deprecation warning, because a string name is the only clean way to get a MHF head.
    @SuppressWarnings("deprecation")
    public ItemStackBuilder asItemBuilder() {
        return new ItemStackBuilder(Material.PLAYER_HEAD)
                .withMeta((SkullMeta s) -> s.setOwner(headName));
    }
}
