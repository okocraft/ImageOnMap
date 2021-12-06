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

package fr.zcraft.quartzlib.tools.mojang;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 * An utility to retrieve UUID from pseudonyms against the Mojang API.
 *
 * @author ProkopyL and Amaury Carrade (documentation only).
 */
public final class UUIDFetcher {
    /**
     * The maximal amount of usernames to send to mojang per request. This allows not to overload
     * Mojang's service with too many usernames at a time.
     */
    private static final int MOJANG_USERNAMES_PER_REQUEST = 100;

    /**
     * The maximal amount of requests to send to Mojang. The time limit for this amount is
     * MOJANG_MAX_REQUESTS_TIME Read : You can only send MOJANG_MAX_REQUESTS in
     * MOJANG_MAX_REQUESTS_TIME seconds.
     */
    private static final int MOJANG_MAX_REQUESTS = 600;

    /**
     * The timeframe for the Mojang request limit (in seconds).
     */
    private static final int MOJANG_MAX_REQUESTS_TIME = 600;

    /**
     * The minimum time between two requests to Mojang (in milliseconds).
     */
    private static final int TIME_BETWEEN_REQUESTS = 200;

    /**
     * The (approximative) timestamp of the date when Mojang name changing feature was announced to
     * be released.
     */
    private static final int NAME_CHANGE_TIMESTAMP = 1420844400;

    private static final String PROFILE_URL = "https://api.mojang.com/profiles/minecraft";
    private static final String TIMED_PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";


    private UUIDFetcher() {
    }


    /**
     * Fetches the UUIDs of the given player name from the Mojang API.
     *
     * <p><b>WARNING: this method needs to be called from a dedicated thread, as the requests to
     * Mojang are executed directly in the current thread and, due to the Mojang API rate limit, the
     * thread may be frozen to wait a bit between requests if a lot of UUID are requested.</b></p>
     *
     * <p>You can use a {@link fr.zcraft.quartzlib.components.worker.Worker} to retrieve UUIDs.</p>
     *
     * @param name A player name.
     * @return The player's {@link UUID}, or {@code null} if it was not found.
     * @throws IOException          If an exception occurs while contacting the Mojang API.
     * @throws InterruptedException If the thread is interrupted while sleeping when the system wait
     *                              because of the Mojang API rate limit.
     */
    public static UUID fetch(String name) throws IOException, InterruptedException {
        final List<String> nameCollection = Collections.singletonList(name);

        final Map<String, UUID> uuid = fetch(nameCollection);
        if (uuid.isEmpty()) {
            fetchRemaining(nameCollection, uuid);
        }

        return uuid.get(name);
    }

    /**
     * Fetches the UUIDs of the given list of player names from the Mojang API.
     *
     * <p><b>WARNING: this method needs to be called from a dedicated thread, as the requests to
     * Mojang are executed directly in the current thread and, due to the Mojang API rate limit, the
     * thread may be frozen to wait a bit between requests if a lot of UUID are requested.</b></p>
     *
     * <p>You can use a {@link fr.zcraft.quartzlib.components.worker.Worker} to retrieve UUIDs.</p>
     * <p>This method may not be able to retrieve UUIDs for some players with old accounts. For them,
     * use {@link #fetchRemaining(Collection, Map)}.</p>
     *
     * @param names A list of player names.
     * @return A map linking a player name to his Mojang {@link UUID}.
     * @throws IOException          If an exception occurs while contacting the Mojang API.
     * @throws InterruptedException If the thread is interrupted while sleeping when the system wait
     *                              because of the Mojang API rate limit.
     */
    public static Map<String, UUID> fetch(List<String> names) throws IOException, InterruptedException {
        return fetch(names, MOJANG_USERNAMES_PER_REQUEST);
    }

    /**
     * Fetches the UUIDs of the given list of player names from the Mojang API.
     *
     * <p><b>WARNING: this method needs to be called from a dedicated thread, as the requests to
     * Mojang are executed directly in the current thread and, due to the Mojang API rate limit, the
     * thread may be frozen to wait a bit between requests if a lot of UUID are requested.</b></p>
     *
     * <p>You can use a {@link fr.zcraft.quartzlib.components.worker.Worker} to retrieve UUIDs.</p>
     * <p>This method may not be able to retrieve UUIDs for some players with old accounts. For them,
     * use {@link #fetchRemaining(Collection, Map)}.</p>
     *
     * @param names          A list of player names.
     * @param limitByRequest The maximal number of UUID to retrieve per request.
     * @return A map linking a player name to his Mojang {@link UUID}.
     * @throws IOException          If an exception occurs while contacting the Mojang API.
     * @throws InterruptedException If the thread is interrupted while sleeping when the system wait
     *                              because of the Mojang API rate limit.
     */
    public static Map<String, UUID> fetch(List<String> names, int limitByRequest)
            throws IOException, InterruptedException {
        Map<String, UUID> uuids = new HashMap<>();
        int requests = (names.size() / limitByRequest) + 1;

        List<String> tempNames;
        Map<String, UUID> tempUuids;

        for (int i = 0; i < requests; i++) {
            tempNames = names.subList(limitByRequest * i, Math.min((limitByRequest * (i + 1)) - 1, names.size()));
            tempUuids = rawFetch(tempNames);
            uuids.putAll(tempUuids);
            Thread.sleep(TIME_BETWEEN_REQUESTS);
        }

        return uuids;
    }

    /**
     * Fetches the UUIDs of the given list of player names from the Mojang API with one request for
     * them all.
     *
     * <p><b>WARNING: this method needs to be called from a dedicated thread, as the requests to
     * Mojang are executed directly in the current thread and, due to the Mojang API rate limit, the
     * thread may be frozen to wait a bit between requests if a lot of UUID are requested.</b></p>
     * <p>This method may not be able to retrieve UUIDs for some players with old accounts. For them,
     * use {@link #fetchRemaining(Collection, Map)}.</p>
     *
     * @param names A list of player names.
     * @return A map linking a player name to his Mojang {@link UUID}.
     * @throws IOException If an exception occurs while contacting the Mojang API.
     */
    private static Map<String, UUID> rawFetch(List<String> names) throws IOException {
        Map<String, UUID> uuidMap = new HashMap<>();
        HttpURLConnection connection = getPostConnection(PROFILE_URL);

        writeBody(connection, names);
        JSONArray array;
        try {
            array = (JSONArray) readResponse(connection);
        } catch (ParseException ex) {
            throw new IOException("Invalid response from server, unable to parse received JSON : " + ex);
        }

        if (array == null) {
            return uuidMap;
        }

        List<String> remainingNames = new ArrayList<String>();
        remainingNames.addAll(names);

        for (Object profile : array) {
            JSONObject jsonProfile = (JSONObject) profile;
            String foundName = (String) jsonProfile.get("name");
            String name = null;
            for (String requestedName : remainingNames) {
                if (requestedName.equalsIgnoreCase(foundName)) {
                    name = requestedName;
                    break;
                }
            }

            if (name == null) {
                name = foundName;
            } else {
                remainingNames.remove(name);
            }

            String id = (String) jsonProfile.get("id");
            uuidMap.put(name, fromMojangUuid(id));
        }

        return uuidMap;
    }

    /**
     * Terminates the process of UUID fetching against another Mojang API.
     * <p>The above methods may not be able to retrieve UUIDs for some players with old accounts. This
     * one will fetch them from another API. But please note that <i>the API used here is way slower
     * than the other one</i>, as only one UUID can be fetched per request, instead of 100 for the
     * other one, and the Mojang servers limit requests rate at one per second. Use it only if
     * needed.</p>
     *
     * <p><b>WARNING: this method needs to be called from a dedicated thread, as the requests to
     * Mojang are executed directly in the current thread and, due to the Mojang API rate limit, the
     * thread may be frozen to wait a bit between requests if more than one UUID is
     * requested.</b></p>
     *
     * @param names The complete list of names to retrieve.
     * @param uuids The UUIDs already retrieved by the {@link #fetch(List)} or {@link #fetch(List,
     *              int)} methods. This map will be completed with the missing UUIDs.
     * @throws IOException          If an exception occurs while contacting the Mojang API.
     * @throws InterruptedException If the thread is interrupted while sleeping when the system wait
     *                              because of the Mojang API rate limit.
     */
    public static void fetchRemaining(Collection<String> names, Map<String, UUID> uuids)
            throws IOException, InterruptedException {
        ArrayList<String> remainingNames = new ArrayList<>();

        for (String name : names) {
            if (!uuids.containsKey(name)) {
                remainingNames.add(name);
            }
        }

        int timeBetweenRequests;
        if (remainingNames.size() > MOJANG_MAX_REQUESTS) {
            timeBetweenRequests = (MOJANG_MAX_REQUESTS / MOJANG_MAX_REQUESTS_TIME) * 1000;
        } else {
            timeBetweenRequests = TIME_BETWEEN_REQUESTS;
        }

        User user;
        for (String name : remainingNames) {
            user = fetchOriginalUuid(name);
            if (user == null) {
                continue;
            }
            uuids.put(name, user.uuid);
            Thread.sleep(timeBetweenRequests);
        }
    }

    /**
     * Fetches the original UUID of the given name.
     * <p>As example, let's say we have a Foo account with the UUID {@code 00000} renamed to Bar, and
     * another account with UUID {@code 11111} was renamed later to Foo. Calling this method with
     * {@code "Foo"} will return {@code 00000}.</p>
     *
     * @param name The player's name.
     * @return An internal object containing the current player name and his UUID.
     * @throws IOException If an exception occurs while contacting the Mojang API.
     */
    private static User fetchOriginalUuid(String name) throws IOException {
        HttpURLConnection connection = getGetConnection(TIMED_PROFILE_URL + name + "?at=" + NAME_CHANGE_TIMESTAMP);

        JSONObject object;

        try {
            object = (JSONObject) readResponse(connection);
        } catch (ParseException ex) {
            throw new IOException("Invalid response from server, unable to parse received JSON : " + ex);
        }

        if (object == null) {
            return null;
        }

        User user = new User();
        user.name = name;
        user.uuid = fromMojangUuid((String) object.get("id"));
        return user;
    }

    /**
     * Opens a POST connection.
     *
     * @param url The URL to connect to.
     * @return A POST connection to this URL.
     * @throws IOException If an exception occurred while contacting the server.
     */
    private static HttpURLConnection getPostConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        return connection;
    }

    /**
     * Opens a GET connection.
     *
     * @param url The URL to connect to.
     * @return A GET connection to this URL.
     * @throws IOException If an exception occurred while contacting the server.
     */
    private static HttpURLConnection getGetConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);

        return connection;
    }

    /**
     * Writes a JSON body in this connection from the given list.
     *
     * @param connection The connection.
     * @param names      The list to write as a JSON object.
     * @throws IOException If an exception occurred while contacting the server.
     */
    private static void writeBody(HttpURLConnection connection, List<String> names) throws IOException {
        OutputStream stream = connection.getOutputStream();
        String body = JSONArray.toJSONString(names);
        stream.write(body.getBytes());
        stream.flush();
        stream.close();
    }

    /**
     * Reads the response as a JSON object.
     *
     * @param connection The connection.
     * @return An object returned by the GSON's {@link JSONParser#parse(Reader)}.
     * @throws IOException    If an exception occurred while contacting the server.
     * @throws ParseException If the response cannot be parsed as a JSON object.
     */
    private static Object readResponse(HttpURLConnection connection) throws IOException, ParseException {
        if (connection.getResponseCode() == 204) {
            return null;
        }

        return new JSONParser().parse(new InputStreamReader(connection.getInputStream()));
    }

    /**
     * This method converts an UUID sent by Mojang to an {@link UUID} object.
     * <p>{@link UUID#fromString(String)} cannot be used because Mojang sends string UUIDs without dashes...</p>
     *
     * @param id The raw UUID sent by Mojang, without dashes.
     * @return The {@link UUID}.
     */
    private static UUID fromMojangUuid(String id) {
        return UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-"
                + id.substring(12, 16) + "-" + id.substring(16, 20) + "-"
                + id.substring(20, 32));
    }

    /**
     * Internal representation of an user with a (current) player name and an UUID.
     */
    private static class User {
        public String name;
        public UUID uuid;
    }
}
