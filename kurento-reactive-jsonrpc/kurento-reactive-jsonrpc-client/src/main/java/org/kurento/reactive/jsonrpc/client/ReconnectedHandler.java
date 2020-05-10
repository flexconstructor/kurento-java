package org.kurento.reactive.jsonrpc.client;

public interface ReconnectedHandler {
  public void run(boolean sameServer);
}