package com.zimory.ldapunit.core.it;

import java.io.IOException;
import java.net.ServerSocket;

public final class Ports {

    private Ports() {
        throw new UnsupportedOperationException("Non-instantiable");
    }

    /**
     * Provides a quick way to get a random, unused port by opening a
     * {@link java.net.ServerSocket} and
     * getting the locally assigned port for the server socket.
     *
     * @return a random, unused port.
     */
    public static int getRandomUnusedPort() {
        try {
            ServerSocket socket = null;

            try {
                socket = new ServerSocket(0);
                return socket.getLocalPort();
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

}