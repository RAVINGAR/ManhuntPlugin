package com.ravingarinc.manhunt.api.async;

import com.ravingarinc.manhunt.api.util.I;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

@ThreadSafe
public class TaskCallback<V> implements Runnable {
    private final Callable<V> callback;
    private final Semaphore semaphore;
    private V value;
    private Optional<Exception> exception;

    /**
     * As soon as this callback is constructed {@link this#get()} becomes locked until the BukkitTask
     * has run.
     *
     * @param callback to provide the given value
     */
    public TaskCallback(final Callable<V> callback) throws AsynchronousException {
        this.callback = callback;
        this.value = null;

        this.exception = Optional.empty();
        this.semaphore = new Semaphore(1);
        try {
            semaphore.acquire();
        } catch (final InterruptedException e) {
            throw new AsynchronousException("Acquiring semaphore was interrupted in task callback!", e);
        }
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void run() {
        try {
            value = callback.call();
        } catch (final Exception e) {
            exception = Optional.of(e);
        } finally {
            semaphore.release();
        }
    }

    @NotNull
    @Async.Execute
    @Blocking
    public V get() throws AsynchronousException {
        try {
            if (!semaphore.tryAcquire(500, TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Getting value from task callback timed out!");
            }
        } catch (final InterruptedException | TimeoutException e) {
            if (exception.isEmpty()) {
                exception = Optional.of(e);
            } else {
                I.log(Level.SEVERE, "Encountered multiple exceptions in task callback!", e);
            }
        } finally {
            semaphore.release();
        }
        if (exception.isPresent()) {
            throw new AsynchronousException("Exception thrown during task callback!", exception.get());
        }
        if (value == null) {
            throw new AsynchronousException("Value for task callback was not computed!");
        }
        return value;
    }
}
