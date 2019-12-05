package ru.bdm.reflection;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.collect.Lists.reverse;
import static java.util.function.Function.identity;
import static ru.bdm.reflection.PathExtractor.Path;
import static ru.bdm.reflection.PathExtractor.createProxy;

/**
 * User: D.Brusentsov
 * Date: 20.05.14
 * Time: 2:55
 */
public class PathExtractorJava8<Curr> {

    private final List<Function> calls = new ArrayList<>();
    private final Class type;

    private PathExtractorJava8(@Nonnull Class type) {
        this(type, ImmutableList.<Function>of());
    }

    private PathExtractorJava8(@Nonnull Class type, @Nonnull List<Function> calls) {
        this.type = type;
        this.calls.addAll(calls);
    }

    public static <F, T> PathExtractorJava8<T> start(@Nonnull Class<F> type, @Nonnull Function<F, T> call) {
        return new PathExtractorJava8<F>(type).then(call);
    }

    public static <T> String path(@Nonnull Class<T> type, @Nonnull Consumer<T> call) {
        return new PathExtractorJava8<T>(type).end(call);
    }

    public <T> PathExtractorJava8<T> then(@Nonnull Function<Curr, T> function) {
        PathExtractorJava8<T> res = new PathExtractorJava8<>(type, calls);
        res.calls.add(function);
        return res;
    }

    public <T, C extends Iterable<T>> PathExtractorJava8<T> thenMask(@Nonnull Function<Curr, C> function) {
        PathExtractorJava8<T> res = new PathExtractorJava8<>(type, calls);
        res.calls.add(function);
        res.calls.add((Function<Collection<T>, T>) PathExtractor::mask);
        return res;
    }

    public String end() {
        if (calls.isEmpty()) {
            throw new IllegalStateException();
        }
        final Path path = new Path();
        reverse(calls).stream().reduce(identity(), Function::compose).apply(createProxy(type, path));
        return path.getValue();
    }

    public String end(@Nonnull final Consumer<Curr> consumer) {
        final Function<Curr, Void> e = o -> {
            consumer.accept(o);
            return null;
        };
        calls.add(e);
        return end();
    }
}
