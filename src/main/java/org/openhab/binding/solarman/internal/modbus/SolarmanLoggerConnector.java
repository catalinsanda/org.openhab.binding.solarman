package org.openhab.binding.solarman.internal.modbus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.openhab.binding.solarman.internal.SolarmanLoggerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Catalin Sanda - Initial contribution
 */
public class SolarmanLoggerConnector {
    private final static Logger LOGGER = LoggerFactory.getLogger(SolarmanLoggerConnector.class);
    private final SolarmanLoggerConfiguration solarmanLoggerConfiguration;

    public SolarmanLoggerConnector(SolarmanLoggerConfiguration solarmanLoggerConfiguration) {
        this.solarmanLoggerConfiguration = solarmanLoggerConfiguration;
    }

    public byte[] sendRequest(byte[] reqFrame) {
        SocketAddress sockaddr = new InetSocketAddress(solarmanLoggerConfiguration.getHostname(),
                solarmanLoggerConfiguration.getPort());

        try (Socket clientSocket = new Socket()) {
            clientSocket.setSoTimeout(10_000);
            clientSocket.connect(sockaddr, 10_000);
            LOGGER.debug("Request frame: " + bytesToHex(reqFrame));
            clientSocket.getOutputStream().write(reqFrame);
            byte[] buffer = new byte[1024];
            int attempts = 5;

            while (attempts > 0) {
                attempts--;
                try {
                    int bytesRead = clientSocket.getInputStream().read(buffer);
                    if (bytesRead < 0) {
                        LOGGER.debug("No data received");
                    } else {
                        byte[] data = Arrays.copyOfRange(buffer, 0, bytesRead);
                        if (LOGGER.isDebugEnabled())
                            LOGGER.debug("Response frame: " + bytesToHex(data));
                        return data;
                    }
                } catch (SocketTimeoutException e) {
                    LOGGER.debug("Connection timeout");
                    if (attempts == 0) {
                        LOGGER.debug("Too many connection timeouts");
                    }
                } catch (IOException e) {
                    LOGGER.debug("Connection error", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not open socket on IP " + solarmanLoggerConfiguration.getHostname());
        }

        return new byte[0];
    }

    private static String bytesToHex(byte[] bytes) {
        return IntStream.range(0, bytes.length).mapToObj(i -> String.format("%02X", bytes[i]))
                .collect(Collectors.joining());
    }
}
