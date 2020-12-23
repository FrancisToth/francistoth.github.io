package io.github.francistoth;

import lombok.Getter;
import static io.github.francistoth.Domain.Address.make;
import static io.github.francistoth.EmailFilter.*;

interface EmailFilter {

  // Primitives ############################################
  static EmailFilter ALWAYS = new EmailFilter() {
  };

  @Getter
  static class Not implements EmailFilter {
    private final EmailFilter emailFilter;

    public Not(EmailFilter emailFilter) {
      this.emailFilter = emailFilter;
    }
  }

  @Getter
  static class And implements EmailFilter {
    private final EmailFilter left;
    private final EmailFilter right;

    public And(EmailFilter left, EmailFilter right) {
      this.left = left;
      this.right = right;
    }

  }

  @Getter
  static class SenderEquals implements EmailFilter {
    private final Domain.Address target;

    public SenderEquals(Domain.Address target) {
      this.target = target;
    }
  }

  @Getter
  static class RecipientEquals implements EmailFilter {
    private final Domain.Address target;

    public RecipientEquals(Domain.Address target) {
      this.target = target;
    }
  }

  @Getter
  static class BodyEquals implements EmailFilter {
    private final String body;

    public BodyEquals(String body) {
      this.body = body;
    }
  }

  // Constructors ###########################################
  static EmailFilter senderEquals(Domain.Address target) {
    return new SenderEquals(target);
  }

  static EmailFilter recipientEquals(Domain.Address target) {
    return new RecipientEquals(target);
  }

  static EmailFilter bodyEquals(String body) {
    return new BodyEquals(body);
  }

  // Operators ##############################################
  default EmailFilter and(EmailFilter that) {
    return new And(this, that);
  }

  default EmailFilter negate() {
    return new Not(this);
  }

  // a || b == !(!a && !b)
  default EmailFilter or(EmailFilter that) {
    return negate().and(that.negate()).negate();
  }
}

// Some example of how to use the DSL
class MainEmailFilter {
  public static void main(String[] args) {
    EmailFilter johnAndPaul = 
      senderEquals(make("john@beatles.com"))
        .or(senderEquals(make("paul@beatles.com")));

    EmailFilter filter = 
      bodyEquals("Some email body")
        .and(johnAndPaul).negate();

    // ...
   }
 }