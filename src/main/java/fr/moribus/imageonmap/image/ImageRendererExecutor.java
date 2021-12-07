/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2021)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2021)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
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
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap.image;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.util.ExceptionCatcher;
import fr.zcraft.quartzlib.components.i18n.I;
import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageRendererExecutor {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4),
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("Image Renderer - #%d")
                    .setUncaughtExceptionHandler(ExceptionCatcher::catchException)
                    .build()
    );

    public static Executor getMainThread() {
        return Bukkit.getScheduler().getMainThreadExecutor(ImageOnMap.getPlugin());
    }

    @FunctionalInterface
    interface ExceptionalSupplier<T> {
        T supply() throws Throwable;
    }

    private static <T> CompletableFuture<T> supply(ExceptionalSupplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.supply();
            } catch (Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }, EXECUTOR);
    }

    private static void checkSizeLimit(final UUID playerUUID, final BufferedImage image) throws IOException {
        var player = Bukkit.getPlayer(playerUUID);

        if ((PluginConfiguration.LIMIT_SIZE_X.get() > 0 || PluginConfiguration.LIMIT_SIZE_Y.get() > 0)
                && !(player != null && Permissions.BYPASS_SIZE.grantedTo(player))) {
            if (PluginConfiguration.LIMIT_SIZE_X.get() > 0
                    && image.getWidth() > PluginConfiguration.LIMIT_SIZE_X.get()) {
                throw new IOException(I.t("The image is too wide!"));
            }

            if (PluginConfiguration.LIMIT_SIZE_Y.get() > 0 &&
                    image.getHeight() > PluginConfiguration.LIMIT_SIZE_Y.get()) {
                throw new IOException(I.t("The image is too tall!"));
            }
        }
    }

    public static CompletableFuture<ImageMap> render(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                                                     final int width, final int height) {
        return supply(() -> {
            BufferedImage image = null;
            var strUrl = url.toString();
            //If the link is an imgur one
            if (strUrl.toLowerCase().startsWith("https://imgur.com/")) {
                //Not handled, can't with the hash only access the image in i.imgur.com/<hash>.<extension>
                if (strUrl.contains("gallery/")) {
                    throw new IOException(
                            "We do not support imgur gallery yet, please use direct link to image instead."
                                    + " Right click on the picture you want "
                                    + "to use then select copy picture link:) ");
                }

                for (Extension ext : Extension.values()) {
                    var newLink = "https://i.imgur.com/" + strUrl.substring(18) + "." + ext.toString();

                    try (var stream = new URL(newLink).openStream()) {
                        image = ImageIO.read(stream);
                    }

                    //valid image
                    if (image != null) {
                        break;
                    }
                }
            } else {
                try (var stream = url.openStream()) {
                    image = ImageIO.read(stream);
                }
            }

            if (image == null) {
                throw new IOException(I.t("The given URL is not a valid image"));
            }

            // Limits are in place and the player does NOT have rights to avoid them.
            checkSizeLimit(playerUUID, image);

            if (scaling != ImageUtils.ScalingType.NONE && height <= 1 && width <= 1) {
                ImageMap ret = renderSingle(scaling.resize(image, ImageMap.WIDTH, ImageMap.HEIGHT), playerUUID);
                image.flush();
                return ret;
            } else {
                var resizedImage = scaling.resize(image, ImageMap.WIDTH * width, ImageMap.HEIGHT * height);
                image.flush();

                return renderPoster(resizedImage, playerUUID);
            }
        });
    }

    public static CompletableFuture<ImageMap> update(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                                                     final ImageMap map, final int width, final int height) {
        return supply(() -> {
            BufferedImage image;

            try (var stream = url.openStream()) {
                image = ImageIO.read(stream);
            }

            if (image == null) {
                throw new IOException(I.t("The given URL is not a valid image"));
            }

            // Limits are in place and the player does NOT have rights to avoid them.
            checkSizeLimit(playerUUID, image);

            var resizedImage = scaling.resize(image, width * 128, height * 128);

            updateMap(new PosterImage(resizedImage), map.getMapsIDs());

            return map;
        });
    }

    private static void updateMap(PosterImage poster, int[] mapsIDs) {
        poster.splitImages();

        ImageIOExecutor.saveImage(mapsIDs, poster);

        if (PluginConfiguration.SAVE_FULL_IMAGE.get()) {
            ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), poster.getImage());
        }

        getMainThread().execute(() -> Renderer.installRenderer(poster, mapsIDs));
    }

    private static ImageMap renderSingle(final BufferedImage image, final UUID playerUUID) throws Throwable {
        MapManager.checkMapLimit(1, playerUUID);

        int mapID = CompletableFuture.supplyAsync(() -> MapManager.getNewMapsIds(1)[0], getMainThread()).join();

        ImageIOExecutor.saveImage(mapID, image);

        getMainThread().execute(() -> Renderer.installRenderer(image, mapID));

        return MapManager.createMap(playerUUID, mapID);
    }

    private static ImageMap renderPoster(final BufferedImage image, final UUID playerUUID) throws Throwable {
        PosterImage poster = new PosterImage(image);

        int mapCount = poster.getImagesCount();
        MapManager.checkMapLimit(mapCount, playerUUID);

        int[] mapsIDs = CompletableFuture.supplyAsync(() -> MapManager.getNewMapsIds(mapCount), getMainThread()).join();

        updateMap(poster, mapsIDs);

        poster.getImage().flush();

        return MapManager.createMap(poster, playerUUID, mapsIDs);
    }

    private enum Extension {
        png, jpg, jpeg, gif
    }
}
