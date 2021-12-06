package fr.zcraft.quartzlib.components.commands;

import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.worker.Worker;
import fr.zcraft.quartzlib.components.worker.WorkerAttributes;
import fr.zcraft.quartzlib.components.worker.WorkerCallback;
import fr.zcraft.quartzlib.components.worker.WorkerRunnable;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.mojang.UUIDFetcher;
import java.util.UUID;
import java.util.function.Consumer;

@WorkerAttributes(name = "Command's worker", queriesMainThread = true)
public class CommandWorkers extends Worker {

    /**
     * Fetches an offline player's UUID by name.
     */
    public void offlineNameFetch(final String playerName, final Consumer<UUID> callback) {
        final WorkerCallback wCallback = new WorkerCallback<UUID>() {
            @Override
            public void finished(UUID result) {
                callback.accept(result);  // Si tout va bien on passe l'UUID au callback
            }

            @Override
            public void errored(Throwable exception) {
                PluginLogger.warning(I.t("Error while getting player UUID"));
                callback.accept(null);  // En cas d'erreur on envoie `null` au callback
            }
        };
        WorkerRunnable wr = new WorkerRunnable<UUID>() {
            @Override
            public UUID run() throws Throwable {
                return UUIDFetcher.fetch(playerName);
            }
        };
        submitQuery(wr, wCallback);
    }

}
