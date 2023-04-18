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
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Catalin Sanda - Initial contribution
 */
public class SolarmanLoggerConnector {
    private final SolarmanLoggerConfiguration solarmanLoggerConfiguration;
    public SolarmanLoggerConnector(SolarmanLoggerConfiguration solarmanLoggerConfiguration) {
        this.solarmanLoggerConfiguration = solarmanLoggerConfiguration;
    }

    public SolarmanLoggerConnection createConnection() {
        return new SolarmanLoggerConnection(solarmanLoggerConfiguration.getHostname(),
                solarmanLoggerConfiguration.getPort());
    }

}
