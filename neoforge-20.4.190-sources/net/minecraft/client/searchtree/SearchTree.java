package net.minecraft.client.searchtree;

import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SearchTree<T> {
    /**
     * Searches this search tree for the given text.
     * <p>
     * If the query does not contain a {@code :}, then only {@link #byName} is searched. If it does contain a colon, both {@link #byName} and {@link #byId} are searched and the results are merged using a {@link MergingIterator}.
     * @return A list of all matching items in this search tree.
     */
    List<T> search(String pQuery);
}
