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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

class WorkerMainThreadExecutor implements Runnable {
    private static final int WATCH_LOOP_DELAY = 1;

    private final String name;
    private final ArrayDeque<WorkerFuture> mainThreadQueue = new ArrayDeque<>();
    private BukkitTask mainThreadTask;

    public WorkerMainThreadExecutor(String name) {
        this.name = name;
    }

    public void init() {
        mainThreadTask = Bukkit.getScheduler().runTaskTimer(QuartzLib.getPlugin(), this, 0, WATCH_LOOP_DELAY);
    }

    public void exit() {
        mainThreadTask.cancel();
        mainThreadTask = null;
    }

    public <T> Future<T> submit(Callable<T> callable) {
        WorkerFuture<T> future = new WorkerFuture<T>(callable);
        synchronized (mainThreadQueue) {
            mainThreadQueue.add(future);
        }
        return future;
    }

    @Override
    public void run() {
        WorkerFuture currentFuture;
        synchronized (mainThreadQueue) {
            if (mainThreadQueue.isEmpty()) {
                return;
            }
            currentFuture = mainThreadQueue.pop();
        }

        currentFuture.runCallable();
    }

    private class WorkerFuture<T> implements Future<T> {
        private final Callable<T> callable;
        private boolean isCancelled;
        private boolean isDone;
        private Exception executionException;
        private T value;

        public WorkerFuture(Callable<T> callable) {
            this.callable = callable;
        }

        public void runCallable() {
            try {
                value = callable.call();
            } catch (Exception ex) {
                executionException = ex;
            } finally {
                isDone = true;
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (this.isCancelled || this.isDone) {
                return false;
            }
            this.isCancelled = true;
            this.isDone = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return this.isCancelled;
        }

        @Override
        public boolean isDone() {
            return this.isDone;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            waitForCompletion();
            if (executionException != null) {
                throw new ExecutionException(executionException);
            }
            return value;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            waitForCompletion(timeout, unit);
            if (executionException != null) {
                throw new ExecutionException(executionException);
            }
            return value;
        }

        private void waitForCompletion(long timeout) throws InterruptedException, TimeoutException {
            synchronized (this) {
                long remainingTime;
                long timeoutTime = System.currentTimeMillis() + timeout;
                while (!isDone) {
                    remainingTime = timeoutTime - System.currentTimeMillis();
                    if (remainingTime <= 0) {
                        throw new TimeoutException();
                    }
                    this.wait(remainingTime);
                }
            }
        }

        private void waitForCompletion() throws InterruptedException {
            synchronized (this) {
                while (!isDone) {
                    this.wait();
                }
            }
        }

        private void waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            long millis = 0;
            switch (unit) {
                case NANOSECONDS:
                    millis = timeout / 10 ^ 6;
                    break;
                case MICROSECONDS:
                    millis = timeout / 10 ^ 3;
                    break;
                case MILLISECONDS:
                    millis = timeout;
                    break;
                case SECONDS:
                    millis = timeout * 10 ^ 3;
                    break;
                case MINUTES:
                    millis = timeout * 10 ^ 3 * 60;
                    break;
                case HOURS:
                    millis = timeout * 10 ^ 3 * 3600;
                    break;
                case DAYS:
                    millis = timeout * 10 ^ 3 * 3600 * 24;
                    break;
                default:
                    break;
            }
            waitForCompletion(millis);
        }


    }
}
