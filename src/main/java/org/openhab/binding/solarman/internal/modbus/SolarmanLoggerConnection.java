package org.openhab.binding.solarman.internal.modbus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SolarmanLoggerConnection implements AutoCloseable {
    private final static Logger LOGGER = LoggerFactory.getLogger(SolarmanLoggerConnection.class);

    private SocketAddress sockaddr;
    private Socket socket;

    public SolarmanLoggerConnection(String hostName, int port) {
        sockaddr = new InetSocketAddress(hostName, port);
    }

    public byte[] sendRequest(byte[] reqFrame, Boolean allowLogging) {
        // Will not be used by multiple threads, so not bothering making it thread safe for now
        if (socket == null) {
            if ((socket = connectSocket(allowLogging)) == null) {
                if (allowLogging)
                    LOGGER.info("Error creating socket");
                return new byte[0];
            }
        }

        try {
            LOGGER.debug("Request frame: " + bytesToHex(reqFrame));
            socket.getOutputStream().write(reqFrame);
        } catch (IOException e) {
            if (allowLogging)
                LOGGER.info("Unable to send frame to logger", e);
            return new byte[0];
        }


        byte[] buffer = new byte[1024];
        int attempts = 5;

        while (attempts > 0) {
            attempts--;
            try {
                int bytesRead = socket.getInputStream().read(buffer);
                if (bytesRead < 0) {
                    if (allowLogging)
                        LOGGER.info("No data received");
                } else {
                    byte[] data = Arrays.copyOfRange(buffer, 0, bytesRead);
                    if (LOGGER.isDebugEnabled())
                        LOGGER.debug("Response frame: " + bytesToHex(data));
                    return data;
                }
            } catch (SocketTimeoutException e) {
                LOGGER.debug("Connection timeout", e);
                if (attempts == 0 && allowLogging) {
                    LOGGER.info("Too many connection timeouts");
                }
            } catch (IOException e) {
                if (allowLogging)
                    LOGGER.info("Connection error", e);
            }
        }

        return new byte[0];
    }

    private static String bytesToHex(byte[] bytes) {
        return IntStream.range(0, bytes.length).mapToObj(i -> String.format("%02X", bytes[i]))
                .collect(Collectors.joining());
    }

    private Socket connectSocket(Boolean allowLogging) {
        try {
            Socket clientSocket = new Socket();

            clientSocket.setSoTimeout(10_000);
            clientSocket.connect(sockaddr, 10_000);

            return clientSocket;
        } catch (IOException e) {
            if (allowLogging)
                LOGGER.error("Could not open socket on IP " + sockaddr.toString(), e);
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
