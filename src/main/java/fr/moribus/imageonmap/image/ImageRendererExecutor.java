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

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.zcraft.quartzlib.components.i18n.I;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.bukkit.Bukkit;

public class ImageRendererExecutor {

    private static final ExecutorService executor = Executors.newFixedThreadPool(
        Math.min(Runtime.getRuntime().availableProcessors(), 4), new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Image Renderer - #%d")
                .build()
    );

    public static Executor getMainThread() {
        return Bukkit.getScheduler().getMainThreadExecutor(ImageOnMap.getPlugin());
    }

    @FunctionalInterface interface ExceptionalSupplier<T> { public T supply() throws Throwable; }

    private static <T> CompletableFuture<T> supply(ExceptionalSupplier<T> supplier, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return supplier.supply();
            } catch (Throwable t) {
                throw new IllegalArgumentException(t);
            }
        }, executor);
    }

    private static URLConnection connecting(URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        connection.connect();

        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            final int httpCode = httpConnection.getResponseCode();
            if ((httpCode / 100) != 2) {
                throw new IOException(I.t("HTTP error: {0} {1}", httpCode, httpConnection.getResponseMessage()));
            }
        }
        return connection;
    }

    private static void checkSizeLimit(final UUID playerUUID, final BufferedImage image) throws IOException {
        if ((PluginConfiguration.LIMIT_SIZE_X.get() > 0 || PluginConfiguration.LIMIT_SIZE_Y.get() > 0)
                && !Permissions.BYPASS_SIZE.grantedTo(Bukkit.getPlayer(playerUUID))) {
            if (PluginConfiguration.LIMIT_SIZE_X.get() > 0) {
                if (image.getWidth() > PluginConfiguration.LIMIT_SIZE_X.get()) {
                    throw new IOException(I.t("The image is too wide!"));
                }
            }
            if (PluginConfiguration.LIMIT_SIZE_Y.get() > 0) {
                if (image.getHeight() > PluginConfiguration.LIMIT_SIZE_Y.get()) {
                    throw new IOException(I.t("The image is too tall!"));
                }
            }
        }
    }

    public static CompletableFuture<ImageMap> render(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                              final int width, final int height) {
        return supply(() -> {
            BufferedImage image = null;
            //If the link is an imgur one
            if (url.toString().toLowerCase().startsWith("https://imgur.com/")) {

                //Not handled, can't with the hash only access the image in i.imgur.com/<hash>.<extension>

                if (url.toString().contains("gallery/")) {
                    throw new IOException(
                            "We do not support imgur gallery yet, please use direct link to image instead."
                                    + " Right click on the picture you want "
                                    + "to use then select copy picture link:) ");
                }

                for (Extension ext : Extension.values()) {
                    String newLink = "https://i.imgur.com/" + url.toString().split("https://imgur.com/")[1]
                            + "."
                            + ext.toString();
                    URL url2 = new URL(newLink);

                    //Try connecting
                    URLConnection connection = connecting(url2);

                    final InputStream stream = connection.getInputStream();

                    image = ImageIO.read(stream);

                    //valid image
                    if (image != null) {
                        break;
                    }

                }

            } else {
                //Try connecting
                URLConnection connection = connecting(url);

                final InputStream stream = connection.getInputStream();

                image = ImageIO.read(stream);
            }
            if (image == null) {
                throw new IOException(I.t("The given URL is not a valid image"));
            }
            // Limits are in place and the player does NOT have rights to avoid them.
            checkSizeLimit(playerUUID, image);
            if (scaling != ImageUtils.ScalingType.NONE && height <= 1 && width <= 1) {
                ImageMap ret = renderSingle(scaling.resize(image, ImageMap.WIDTH, ImageMap.HEIGHT), playerUUID);
                image.flush();//Safe to free
                return ret;
            }
            final BufferedImage resizedImage =
                    scaling.resize(image, ImageMap.WIDTH * width, ImageMap.HEIGHT * height);
            image.flush();//Safe to free
            return renderPoster(resizedImage, playerUUID);
        }, executor);
    }

    public static CompletableFuture<ImageMap> update(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                              final ImageMap map, final int width, final int height) {
        return supply(() -> {
            final URLConnection connection = connecting(url);

            final InputStream stream = connection.getInputStream();
            final BufferedImage image = ImageIO.read(stream);
            stream.close();

            if (image == null) {
                throw new IOException(I.t("The given URL is not a valid image"));
            }

            // Limits are in place and the player does NOT have rights to avoid them.
            checkSizeLimit(playerUUID, image);

            updateMap(scaling.resize(image, width * 128, height * 128), map.getMapsIDs());
            return map;
        }, executor);
    }

    private static void updateMap(final BufferedImage image, int[] mapsIDs) {

        final PosterImage poster = new PosterImage(image);
        poster.splitImages();

        ImageIOExecutor.saveImage(mapsIDs, poster);

        if (PluginConfiguration.SAVE_FULL_IMAGE.get()) {
            ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), image);
        }

        supply(() -> {
            Renderer.installRenderer(poster, mapsIDs);
            return null;
        }, getMainThread());
    }

    private static ImageMap renderSingle(final BufferedImage image, final UUID playerUUID) throws Throwable {
        MapManager.checkMapLimit(1, playerUUID);
        final CompletableFuture<Integer> futureMapID = supply(() ->  MapManager.getNewMapsIds(1)[0], getMainThread());

        final int mapID = futureMapID.get();
        ImageIOExecutor.saveImage(mapID, image);
        supply(() -> {
            Renderer.installRenderer(image, mapID);
            return null;
        }, getMainThread());
        return MapManager.createMap(playerUUID, mapID);
    }

    private static ImageMap renderPoster(final BufferedImage image, final UUID playerUUID) throws Throwable {
        final PosterImage poster = new PosterImage(image);
        final int mapCount = poster.getImagesCount();
        MapManager.checkMapLimit(mapCount, playerUUID);
        
        final CompletableFuture<int[]> futureMapsIds = supply(() ->  MapManager.getNewMapsIds(mapCount), getMainThread());
        poster.splitImages();
        final int[] mapsIDs = futureMapsIds.get();

        ImageIOExecutor.saveImage(mapsIDs, poster);
        if (PluginConfiguration.SAVE_FULL_IMAGE.get()) {
            ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), image);
        }
        supply(() -> {
            Renderer.installRenderer(poster, mapsIDs);
            return null;
        }, getMainThread());

        poster.getImage().flush();//Safe to free
        return MapManager.createMap(poster, playerUUID, mapsIDs);
    }

    private enum Extension {
        png, jpg, jpeg, gif
    }

}
