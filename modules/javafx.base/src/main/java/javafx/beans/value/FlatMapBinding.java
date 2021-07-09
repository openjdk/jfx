package javafx.beans.value;

import java.util.Objects;
import java.util.function.Function;

public class FlatMapBinding<S, T> extends LazyObjectBinding<T> {
  private final ObservableValue<S> source;
  private final Function<? super S, ? extends ObservableValue<? extends T>> mapper;

  private Subscription mappedSubscription = Subscription.EMPTY;

  public FlatMapBinding(
      ObservableValue<S> source,
      Function<? super S, ? extends ObservableValue<? extends T>> mapper
  ) {
    this.source = Objects.requireNonNull(source);
    this.mapper = Objects.requireNonNull(mapper);
  }

  @Override
  protected T computeValue() {
    S value = source.getValue();
    ObservableValue<? extends T> mapped = value == null ? null : mapper.apply(value);

    if(isObserved()) {
      mappedSubscription.unsubscribe();
      mappedSubscription = mapped == null ? Subscription.EMPTY : mapped.subscribeInvalidations(this::invalidate);
    }

    return mapped == null ? null : mapped.getValue();
  }

  @Override
  protected Subscription observeInputs() {
    Subscription subscription = source.subscribeInvalidations(this::invalidate);

    return () -> {
      subscription.unsubscribe();
      mappedSubscription.unsubscribe();
      mappedSubscription = Subscription.EMPTY;
    };
  }
}
