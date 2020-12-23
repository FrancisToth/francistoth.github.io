package io.github.francistoth;

import static io.github.francistoth.Console.getStrLn;
import static io.github.francistoth.Console.putStrLn;
import static io.github.francistoth.Console.Visitor;
import java.util.function.Function;
import java.util.function.Supplier;
import io.vavr.control.Option;

public interface Console {

  class PutStrLn implements Console {
    private final String content;
    private final Supplier<Console> next;

    public PutStrLn(String content, Supplier<Console> next) {
      this.content = content;
      this.next = next;
    }

    public PutStrLn(String content) {
      this.content = content;
      this.next = () -> END;
    }

    public Option<Console> unsafeRun(Visitor visitor) {
      visitor.putStrLn(content);
      return Option.of(next.get());
    }
  }


  class GetStrLn implements Console {
    private final Function<String, Console> next;

    public GetStrLn(Function<String, Console> next) {
      this.next = next;
    }

    public Option<Console> unsafeRun(Visitor visitor) {
      String content = visitor.getStrLn();
      return Option.of(next.apply(content));
    }
  }

  Option<Console> unsafeRun(Visitor visitor);

  interface Visitor {
    void putStrLn(String content);

    String getStrLn();
  }

  static Console END = new Console() {
    public Option<Console> unsafeRun(Visitor visitor) {
      return Option.none();
    }
  };

  public static Console repeat(String content, int n) {
    return n <= 1 ? 
      putStrLn(content) : 
      putStrLn(content, () -> repeat(content, n - 1));
  }

  public static Console putStrLn(String content, Console next) {
    return new PutStrLn(content, () -> next);
  }

  public static Console putStrLn(String content, Supplier<Console> next) {
    return new PutStrLn(content, next);
  }

  public static Console putStrLn(String content) {
    return new PutStrLn(content);
  }

  public static Console getStrLn(Function<String, Console> next) {
    return new GetStrLn(next);
  }
}


class Main {

  public static void main(String[] args) {
    Console program =
        putStrLn("Please enter your name.", 
          getStrLn(name -> 
            putStrLn("Hi " + name + "!")
          )
        );

    unsafeRun(program);
  }

  private static void unsafeRun(Console console) {
    Option<Console> next = console.unsafeRun(interpreter);
    if (next.isDefined()) {
      unsafeRun(next.get());
    }
  }

  private static Visitor interpreter = new Visitor() {
    @Override
    public void putStrLn(String content) {
      System.out.println(content);
    }

    @Override
    public String getStrLn() {
      return System.console().readLine();
    }
  };
}
