package io.github.francistoth;

import java.util.function.BiFunction;
import java.util.function.Function;
import io.vavr.Tuple2;
import io.vavr.control.Either;

// Another example of a composable DSL
// Defines the equality between two elements of the same type
interface Equal<A> {
  boolean equal(A a0, A a1);

  static <A> Equal<A> derive() { 
    return Equal.make((a0, a1) -> a0.equals(a1));
  }

  static <A> Equal<A> make(BiFunction<A, A, Boolean> f) {
    return new Equal<A>() {
      public boolean equal(A a0, A a1) {
        return a0.equals(a1);
      }
    };
  }

  // product composition
  // (Equal[A], Equal[B]) => Equal[(A, B)]
  default <B> Equal<Tuple2<A, B>> both(Equal<B> eb) {
    return make((t0, t1) ->
      equal(t0._1, t1._1) && eb.equal(t0._2, t1._2)
    );
  }

  // sum composition
  // (Equal[A], Equal[B]) => Equal[Either[(A, B)]]
  default <B> Equal<Either<A, B>> either(Equal<B> eb) {
    return make((e0, e1) ->
      e0.fold(
        a0 -> e1.fold(a1 -> equal(a0, a1), b1 -> false), 
        b0 -> e0.fold(a0 -> false, b1 -> eb.equal(b0, b1))
      )
    );
  }

  // Constructs an Equal<B> given an Equal<A> and a function
  // mapping a B to an A. 
  default <B> Equal<B> contramap(Function<B, A> f) {
    return make((b0, b1) -> equal(f.apply(b0), f.apply(b1)));
  }
}

class EqualMain {
  public static void main(String[] args) {
    Equal<Integer> integerEquality = Equal.derive();
    
    integerEquality.equal(1, 2); // false
    integerEquality.equal(1, 1); // true
  }
}
