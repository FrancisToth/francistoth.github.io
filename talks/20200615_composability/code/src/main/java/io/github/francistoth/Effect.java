package io.github.francistoth;

import static io.github.francistoth.Effect.Stream.cons;
import java.util.function.Consumer;
import java.util.function.Supplier;
import io.vavr.control.Either;
import io.vavr.control.Option;

interface Effect {

  interface Stream<A> {
    class Cons<A> implements Stream<A> {
      private final A head;
      private final Supplier<Stream<A>> tail;

      private Cons(final A head, final Supplier<Stream<A>> tail) {
        this.head = head;
        this.tail = tail;
      }

      // @Override
      // public <E> Either<E, Option<A>> fold() {
      //   return 
      // }
    }

    static Stream<Void> Nil = new Stream<Void>(){};

    @SuppressWarnings("unchecked")
    static <A> Stream<A> nil() {
      return (Stream<A>) Nil;
    }

    static <A> Stream<A> cons(final A a) {
      return new Cons<A>(a, () -> nil());
    }

   default  <B, E> Either<E, Option<B>> forEach(Consumer<A> f) {
      if(Nil.equals(this)) {
        return Either.right(Option.none());
      } else {
        Cons<A> self = (Cons<A>) this;
        f.accept(self.head);
        return self.tail.get().forEach(f);
      }
    }
  }
}

class StreamMain {
  public static void main(final String[] args) {
    cons(42);
  }
}