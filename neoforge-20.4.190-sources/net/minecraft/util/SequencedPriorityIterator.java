package net.minecraft.util;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import java.util.Comparator;
import java.util.Deque;
import java.util.Optional;
import javax.annotation.Nullable;

public final class SequencedPriorityIterator<T> extends AbstractIterator<T> {
    private final Int2ObjectMap<Deque<T>> valuesByPriority = new Int2ObjectOpenHashMap<>();

    public void add(T pValue, int pPriority) {
        this.valuesByPriority.computeIfAbsent(pPriority, p_308896_ -> Queues.newArrayDeque()).addLast(pValue);
    }

    @Nullable
    @Override
    protected T computeNext() {
        Optional<Deque<T>> optional = this.valuesByPriority
            .int2ObjectEntrySet()
            .stream()
            .filter(p_309082_ -> !p_309082_.getValue().isEmpty())
            .max(Comparator.comparingInt(java.util.Map.Entry::getKey))
            .map(java.util.Map.Entry::getValue);
        return optional.map(Deque::removeFirst).orElseGet(() -> (T)this.endOfData());
    }
}
