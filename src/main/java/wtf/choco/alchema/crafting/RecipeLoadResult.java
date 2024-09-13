package wtf.choco.alchema.crafting;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import wtf.choco.alchema.Alchema;

import java.io.File;
import java.util.List;

/**
 * Represents a result of the {@link CauldronRecipeRegistry#loadCauldronRecipes(Alchema, File)}
 * asynchronous recipe loading.
 */
@NonExtendable
public interface RecipeLoadResult {

    /**
     * Get the amount of recipes loaded natively from Alchema's file system.
     *
     * @return the amount of native recipes loaded
     */
    int getNative();

    /**
     * Get the amount of recipes loaded from third-party plugins.
     *
     * @return the amount of third-party recipes loaded
     */
    int getThirdParty();

    /**
     * Get the amount of total recipes loaded.
     *
     * @return the total recipe count
     */
    default int getTotal() {
        return getNative() + getThirdParty();
    }

    /**
     * Get an immutable List of all recipes that failed to load, if any.
     *
     * @return the failures. If none, an empty list is returned
     */
    @NotNull
    @Unmodifiable
    List<@NotNull RecipeLoadFailureReport> getFailures();

    /**
     * Get the time, in milliseconds, it took for this process to complete.
     *
     * @return the completion time
     */
    long getTimeToComplete();

}
