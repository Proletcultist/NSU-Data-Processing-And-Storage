package ru.nsu.zenin.api.exception;

public class IncompatiblePublicAndPrivateKeysException extends RuntimeException {
  public IncompatiblePublicAndPrivateKeysException(String msg) {
    super(msg);
  }
}
