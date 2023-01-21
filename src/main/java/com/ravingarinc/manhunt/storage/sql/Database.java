package com.ravingarinc.manhunt.storage.sql;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.Module;
import com.ravingarinc.manhunt.api.ModuleLoadException;
import com.ravingarinc.manhunt.api.async.BlockingRunner;
import com.ravingarinc.manhunt.api.util.I;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public abstract class Database extends Module {
    protected final String name;
    protected final String path;
    protected final String url;
    protected final BukkitScheduler scheduler;
    private final String createTable;
    protected BlockingRunner<FutureTask<?>> databaseRunner;

    @SafeVarargs
    public Database(final String name, final String createTable, final Class<? extends Database> identifier, final RavinPlugin plugin, final Class<? extends Module>... dependsOn) {
        super(identifier, plugin, dependsOn);
        this.name = name + ".db";
        this.scheduler = plugin.getServer().getScheduler();
        this.createTable = createTable;
        this.path = plugin.getDataFolder() + "/" + this.name;
        this.url = "jdbc:sqlite:" + this.path;
    }

    @Override
    public void load() throws ModuleLoadException {
        final File file = new File(path);
        try {
            if (!file.exists()) {
                try (Connection connection = DriverManager.getConnection(url)) {
                    I.log(Level.INFO, "Created new database called '%s' with driver '%s'!", name, connection.getMetaData().getDriverName());
                }
            }
            execute(createTable);
        } catch (final SQLException exception) {
            throw new ModuleLoadException(this, ModuleLoadException.Reason.SQL, exception);
        }

        databaseRunner = new BlockingRunner<>(new LinkedBlockingQueue<>());
        databaseRunner.runTask(plugin);
    }

    @Override
    public void cancel() {
        databaseRunner.cancel();
    }

    public FutureTask<?> queue(final Runnable runnable) {
        final FutureTask<?> task = new FutureTask<>(runnable, true);
        databaseRunner.queue(task);
        return task;
    }

    /**
     * Prepare a statement for the database and consume it. This can be used for insert/update of data
     *
     * @param request  The request
     * @param consumer The consumer to apply to the statement
     */
    protected void prepareStatement(final String request, final Consumer<PreparedStatement> consumer) {
        if (isLoaded()) {
            try (Connection connection = DriverManager.getConnection(url);
                 PreparedStatement statement = connection.prepareStatement(request)) {
                consumer.accept(statement);
                statement.executeUpdate();
            } catch (final SQLException exception) {
                I.log(Level.SEVERE, "Encountered issue inserting data into database!", exception);
            }
        }
    }

    /**
     * Execute a query on the database.
     *
     * @param execution The query
     */
    protected void execute(final String execution) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url);
             Statement statement = connection.createStatement()) {
            statement.execute(execution);
        }
    }

    @Nullable
    protected <T> T query(final String query, final Consumer<PreparedStatement> consumer, final Function<ResultSet, T> cursor) {
        if (isLoaded()) {
            try (Connection connection = DriverManager.getConnection(url);
                 PreparedStatement statement = connection.prepareStatement(query)) {
                consumer.accept(statement);
                try (ResultSet result = statement.executeQuery()) {
                    return cursor.apply(result);
                }
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue querying database!", e);
            }
        }
        return null;
    }

    protected <T> Optional<T> query(final String query, final Function<ResultSet, T> cursor) {
        if (isLoaded()) {
            try (Connection connection = DriverManager.getConnection(url);
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet result = statement.executeQuery()) {
                return Optional.ofNullable(cursor.apply(result));
            } catch (final SQLException e) {
                I.log(Level.SEVERE, "Encountered issue querying database!", e);
            }
        }
        return Optional.empty();
    }
}
