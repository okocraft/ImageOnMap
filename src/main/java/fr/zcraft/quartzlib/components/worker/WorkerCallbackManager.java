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

package fr.zcraft.quartzlib.components.worker;

import fr.zcraft.quartzlib.core.QuartzLib;
import java.util.ArrayDeque;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

class WorkerCallbackManager implements Runnable {
    private static final int WATCH_LOOP_DELAY = 5;

    private final HashMap<WorkerRunnable, WorkerRunnableInfo> callbacks;
    private final ArrayDeque<WorkerRunnableInfo> callbackQueue;

    private final String name;

    private BukkitTask selfTask;

    public WorkerCallbackManager(String name) {
        callbacks = new HashMap<>();
        callbackQueue = new ArrayDeque<>();
        this.name = name;
    }

    public void init() {
        selfTask = Bukkit.getScheduler().runTaskTimer(QuartzLib.getPlugin(), this, 0, WATCH_LOOP_DELAY);
    }

    public void setupCallback(WorkerRunnable runnable, WorkerCallback callback) {
        synchronized (callbacks) {
            callbacks.put(runnable, new WorkerRunnableInfo(callback));
        }
    }

    public <T> void callback(WorkerRunnable<T> runnable, T result) {
        callback(runnable, result, null);
    }

    public <T> void callback(WorkerRunnable<T> runnable, T result, Throwable exception) {
        WorkerRunnableInfo<T> runnableInfo;
        synchronized (callbacks) {
            runnableInfo = callbacks.get(runnable);
        }
        if (runnableInfo == null) {
            return;
        }
        runnableInfo.setRunnableException(exception);
        runnableInfo.setResult(result);

        enqueueCallback(runnableInfo);
    }

    public void exit() {
        if (selfTask != null) {
            selfTask.cancel();
        }
    }

    private void enqueueCallback(WorkerRunnableInfo runnableInfo) {
        synchronized (callbackQueue) {
            callbackQueue.add(runnableInfo);
        }
    }

    @Override
    public void run() {
        WorkerRunnableInfo currentRunnableInfo;
        synchronized (callbackQueue) {
            if (callbackQueue.isEmpty()) {
                return;
            }
            currentRunnableInfo = callbackQueue.pop();
        }

        currentRunnableInfo.runCallback();
    }

    private class WorkerRunnableInfo<T> {
        private final WorkerCallback<T> callback;
        private T result;
        private Throwable runnableException;

        public WorkerRunnableInfo(WorkerCallback callback) {
            this.callback = callback;
            this.runnableException = null;
        }

        public void runCallback() {
            if (runnableCrashed()) {
                callback.errored(runnableException);
            } else {
                callback.finished(result);
            }
        }

        public void setResult(T result) {
            this.result = result;
        }

        public void setRunnableException(Throwable runnableException) {
            this.runnableException = runnableException;
        }

        public boolean runnableCrashed() {
            return this.runnableException != null;
        }
    }
}
