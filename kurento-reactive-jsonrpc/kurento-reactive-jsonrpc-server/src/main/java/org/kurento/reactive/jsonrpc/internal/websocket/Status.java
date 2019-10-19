package org.kurento.reactive.jsonrpc.internal.websocket;

/**
 * Web-socket close status.
 */
public class Status {

    /**
     * Session closes by ping timeout.
     */
    public static int PING_TIMEOUT_EXCEEDED_CODE = 3001;

    /**
     * Close by ping reason.
     */
    public static String PING_TIMEOUT_EXCEEDED_DESCRIPTION = "Close for not receive ping from client";

}
