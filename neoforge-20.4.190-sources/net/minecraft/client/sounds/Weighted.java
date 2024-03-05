package net.minecraft.client.sounds;

import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Weighted interface represents an element with a weight in a weighted collection.
 * It is used to provide weighted selection and retrieval of elements.
 *
 * @param <T> The type of the element
 */
@OnlyIn(Dist.CLIENT)
public interface Weighted<T> {
    /**
     * {@return The weight of the element}
     */
    int getWeight();

    /**
     * Retrieves the sound associated with the element.
     * The sound is obtained using the provided random source.
     * <p>
     * @return The sound associated with the element
     *
     * @param pRandomSource the random source used for sound selection
     */
    T getSound(RandomSource pRandomSource);

    /**
     * Preloads the sound if required by the sound engine.
     * This method is called to preload the sound associated with the element into the sound engine, ensuring it is ready for playback.
     *
     * @param pEngine the sound engine used for sound preloading
     */
    void preloadIfRequired(SoundEngine pEngine);
}
