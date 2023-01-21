package com.ravingarinc.manhunt.api;

import com.ravingarinc.manhunt.RavinPlugin;
import com.ravingarinc.manhunt.api.util.I;
import org.jetbrains.annotations.Async;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

public abstract class Module implements Comparable<Module> {
    protected final RavinPlugin plugin;
    protected final Class<? extends Module> clazz;

    protected final List<Class<? extends Module>> dependsOn;
    private boolean isLoaded;

    /**
     * The constructor for a Module, should only ever be called by {@link Module#initialise(RavinPlugin, Class)}.
     * Implementations of Managers should have one public constructor with a EldenRhym object parameter.
     * The implementing constructor CANNOT call {@link RavinPlugin#getModule(Class)} otherwise potential issues
     * may occur. This must be done in {@link this#load()}.
     *
     * @param identifier The class of this manager
     * @param plugin     The owning plugin
     * @param dependsOn  Other managers which are loaded before this manager
     */
    @SafeVarargs
    public Module(final Class<? extends Module> identifier, final RavinPlugin plugin, final Class<? extends Module>... dependsOn) {
        this.plugin = plugin;
        this.clazz = identifier;
        this.dependsOn = new ArrayList<>();
        for (final Class<? extends Module> module : dependsOn) {
            this.dependsOn.add(module);
        }
        this.isLoaded = false;
    }

    public static <T> Optional<T> initialise(final RavinPlugin plugin, final Class<T> identifier) {
        try {
            final Constructor<T> constructor = identifier.getConstructor(RavinPlugin.class);
            return Optional.of(constructor.newInstance(plugin));
        } catch (final NoSuchMethodException e) {
            I.log(Level.SEVERE, "Could not find valid constructor for " + identifier.getName());
        } catch (final InvocationTargetException e) {
            I.log(Level.SEVERE, "Failed to initialise manager '%s'!", e, identifier.getName());
        } catch (InstantiationException | IllegalAccessException e) {
            I.log(Level.SEVERE, "Something went severely wrong creating new instance of manager '%s'!", e, identifier.getName());
        }
        return Optional.empty();
    }

    public Class<? extends Module> getClazz() {
        return clazz;
    }

    /**
     * Load this manager. Will only be called if this manager's dependents are loaded.
     */
    @Async.Execute
    protected abstract void load() throws ModuleLoadException;

    /**
     * Called after initialisaton of all managers but before any managers of which this manager depends on.
     */
    @Async.Execute
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void initialise() throws ModuleLoadException {
        boolean canLoad = true;
        for (final Class<? extends Module> clazz : dependsOn) {
            if (!plugin.getModule(clazz).isLoaded()) {
                canLoad = false;
                break;
            }
        }
        if (!canLoad) {
            throw new ModuleLoadException(this, ModuleLoadException.Reason.DEPENDENCY);
        }
        try {
            load();
        } catch (final ModuleLoadException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw new ModuleLoadException(this, exception);
        }
        I.log(Level.INFO, getName() + " has been loaded");
        isLoaded = true;
    }

    /**
     * This is called by {@link RavinPlugin} during reload or shutdown on all modules in the reverse order that they
     * are loaded. After all modules have had this method called, then isLoaded is set to false before
     * calling {@link #initialise()}
     */
    public abstract void cancel();

    public List<Class<? extends Module>> getDependsOn() {
        return Collections.unmodifiableList(dependsOn);
    }

    public String getName() {
        final String[] split = clazz.getName().split("\\.");
        return split[split.length - 1];
    }

    @Override
    public int compareTo(@NotNull final Module module) {
        final Class<? extends Module> clazz = module.getClazz();
        if (this.getClazz().equals(clazz)) {
            return 0;
        }
        if (dependsOn.isEmpty()) {
            return -2;
        } else {
            return dependsOn.contains(clazz) ? 2 : -1;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Module module = (Module) o;
        return clazz.equals(module.clazz);
    }

    @Override
    public int hashCode() {
        return clazz.hashCode();
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(final boolean loaded) {
        isLoaded = loaded;
    }
}