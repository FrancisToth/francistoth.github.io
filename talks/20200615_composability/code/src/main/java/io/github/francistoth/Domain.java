package io.github.francistoth;

import lombok.Getter;

public class Domain {

  public static Sender sender(String address) {
    return new Sender(address);
  }

  public static Recipient recipient(String address) {
    return new Recipient(address);
  }

  @Getter
  public static class Recipient {
    private final String address;

    private Recipient(String address) {
      this.address = address;
    }
  }

  @Getter
  public static class Sender {
    private final String address;

    private Sender(String address) {
      this.address = address;
    }
  }

  @Getter
  public static class Address {
    private final String target;

    private Address(String target) {
      this.target = target;
    }

    public static Address make(String address) {
      return new Address(address);
    }
  }
}