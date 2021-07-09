package javafx.beans.value;

import java.util.function.Function;

/**
 * Support class which supplies varies types of bindings.
 */
class Bindings {

  public static <T, U> ObservableValue<U> mapping(ObservableValue<T> source, Function<? super T, ? extends U> mapper) {
    return nullableMapping(source, v -> v == null ? null : mapper.apply(v));
  }

  public static <T> ObservableValue<T> conditional(ObservableValue<T> source, ObservableValue<Boolean> condition) {
    return new ConditionalBinding<>(condition, source);
  }

  public static <T, U> ObservableValue<U> flatMapping(ObservableValue<T> source, Function<? super T, ? extends ObservableValue<? extends U>> mapper) {
    return new FlatMapBinding<>(source, mapper);
  }

  public static <T, U> ObservableValue<U> nullableMapping(ObservableValue<T> source, Function<? super T, ? extends U> mapper) {
    return new LazyObjectBinding<>() {
      @Override
      protected Subscription observeInputs() {
        return source.subscribeInvalidations(() -> invalidate());  // start observing source
      }

      @Override
      protected U computeValue() {
        return mapper.apply(source.getValue());
      }
    };
  }
}
