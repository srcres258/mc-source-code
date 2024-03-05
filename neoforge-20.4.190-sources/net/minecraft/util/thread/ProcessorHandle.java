package net.minecraft.util.thread;

import com.mojang.datafixers.util.Either;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ProcessorHandle<Msg> extends AutoCloseable {
    String name();

    void tell(Msg pTask);

    @Override
    default void close() {
    }

    default <Source> CompletableFuture<Source> ask(Function<? super ProcessorHandle<Source>, ? extends Msg> pTask) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<>();
        Msg msg = pTask.apply(of("ask future procesor handle", completablefuture::complete));
        this.tell(msg);
        return completablefuture;
    }

    default <Source> CompletableFuture<Source> askEither(Function<? super ProcessorHandle<Either<Source, Exception>>, ? extends Msg> pTask) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<>();
        Msg msg = pTask.apply(of("ask future procesor handle", p_18719_ -> {
            p_18719_.ifLeft(completablefuture::complete);
            p_18719_.ifRight(completablefuture::completeExceptionally);
        }));
        this.tell(msg);
        return completablefuture;
    }

    static <Msg> ProcessorHandle<Msg> of(final String pName, final Consumer<Msg> pTask) {
        return new ProcessorHandle<Msg>() {
            @Override
            public String name() {
                return pName;
            }

            @Override
            public void tell(Msg p_18731_) {
                pTask.accept(p_18731_);
            }

            @Override
            public String toString() {
                return pName;
            }
        };
    }
}
