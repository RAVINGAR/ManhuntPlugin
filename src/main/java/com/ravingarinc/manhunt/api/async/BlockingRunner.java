package com.ravingarinc.manhunt.api.async;

import com.ravingarinc.manhunt.api.util.I;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

public class BlockingRunner<T extends Future<?> & Runnable> extends BukkitRunnable {
    private final BlockingQueue<T> queue;

    public BlockingRunner(final BlockingQueue<T> queue) {
        this.queue = queue;
    }

    public void queue(final T task) {
        this.queue.add(task);
    }

    public Collection<T> getRemaining() {
        return new HashSet<>(this.queue);
    }

    public void queueAll(final Collection<T> collection) {
        this.queue.addAll(collection);
    }

    @Override
    public void run() {
        while (!isCancelled()) {
            try {
                queue.take().run();
            } catch (final InterruptedException e) {
                I.logIfDebug(() -> "BlockingRunner task was interrupted! This may be expected!", e);
            }
        }
    }


    @Override
    public synchronized void cancel() throws IllegalStateException {
        this.cancel(false);
    }

    /**
     * Immediately cancel and interrupt this runner. This will interrupt any currently running tasks if specified
     */
    public synchronized void cancel(final boolean mayInterruptIfRunning) throws IllegalStateException {
        super.cancel();
        getRemaining().forEach(task -> task.cancel(mayInterruptIfRunning));
    }
}
