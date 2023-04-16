/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 * <p>
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 * <p>
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 * <p>
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.solarman.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.measure.Unit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarman.internal.defmodel.InverterDefinition;
import org.openhab.binding.solarman.internal.defmodel.ParameterItem;
import org.openhab.binding.solarman.internal.defmodel.Validation;
import org.openhab.binding.solarman.internal.modbus.SolarmanLoggerConnector;
import org.openhab.binding.solarman.internal.modbus.SolarmanV5Protocol;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanChannelGroupTypeProvider;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanChannelTypeProvider;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanThingTypeProvider;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.units.indriya.format.SimpleUnitFormat;

/**
 * The {@link SolarmanLoggerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Catalin Sanda - Initial contribution
 */
@NonNullByDefault
public class SolarmanLoggerHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(SolarmanLoggerHandler.class);

    private @Nullable SolarmanLoggerConfiguration config;
    private DefinitionParser definitionParser;
    private SolarmanChannelTypeProvider channelTypeProvider;
    private SolarmanChannelGroupTypeProvider channelGroupTypeProvider;
    private SolarmanThingTypeProvider thingTypeProvider;

    public SolarmanLoggerHandler(Thing thing, SolarmanChannelTypeProvider channelTypeProvider,
            SolarmanChannelGroupTypeProvider channelGroupTypeProvider, SolarmanThingTypeProvider thingTypeProvider) {
        super(thing);
        this.channelTypeProvider = channelTypeProvider;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
        this.thingTypeProvider = thingTypeProvider;
        this.definitionParser = new DefinitionParser();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        config = getConfigAs(SolarmanLoggerConfiguration.class);

        InverterDefinition inverterDefinition = definitionParser.parseDefinition(config.inverterType);

        if (inverterDefinition == null) {
            logger.error("Unable to find a definition for the provided inverter type");
            updateStatus(ThingStatus.UNKNOWN);
            return;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Found definition for {}", config.inverterType);
            }
        }
        SolarmanLoggerConnector solarmanV5Connector = new SolarmanLoggerConnector(config);
        SolarmanV5Protocol solarmanV5Protocol = new SolarmanV5Protocol(config, solarmanV5Connector);

        Map<ParameterItem, ChannelUID> paramToChannelMapping = setupChannelsForInverterDefinition(inverterDefinition);

        scheduler.scheduleAtFixedRate(() -> {
            AtomicBoolean thingReachable = new AtomicBoolean(false);

            inverterDefinition.getRequests().forEach(request -> {
                if (request.getStart() >= request.getEnd()) {
                    logger.warn("Error in inverter definition, start is larger or equal to end for request {}",
                            request);
                    return;
                }

                Map<Integer, byte[]> readRegistersMap = solarmanV5Protocol.readRegisters(
                        (byte) request.getMbFunctioncode().intValue(), request.getStart(), request.getEnd());

                if (readRegistersMap != null && !readRegistersMap.isEmpty())
                    thingReachable.set(true);
                else
                    return;

                updateChannelsForReadRegisters(paramToChannelMapping, readRegistersMap);
            });

            updateStatus(thingReachable.get() ? ThingStatus.ONLINE : ThingStatus.OFFLINE);
        }, 0, config.refreshInterval, TimeUnit.SECONDS);
    }

    private void updateChannelsForReadRegisters(Map<ParameterItem, ChannelUID> paramToChannelMapping,
            Map<Integer, byte[]> readRegistersMap) {
        paramToChannelMapping.forEach((parameterItem, channelUID) -> {
            List<Integer> registers = parameterItem.getRegisters();
            if (readRegistersMap.keySet().containsAll(registers)) {
                switch (parameterItem.getRule()) {
                    case 0, 1, 2, 3, 4 -> updateChannelWithNumericValue(parameterItem, channelUID, registers,
                            readRegistersMap);
                    case 5 -> updateChannelWithStringValue(parameterItem, channelUID, registers, readRegistersMap);
                    case 6 -> updateChannelsForRawValue(parameterItem, channelUID, registers, readRegistersMap);
                }
            }
        });
    }

    private void updateChannelWithStringValue(ParameterItem parameterItem, ChannelUID channelUID,
            List<Integer> registers, Map<Integer, byte[]> readRegistersMap) {
        String stringValue = registers.stream().map(readRegistersMap::get).reduce(new StringBuilder(), (acc, val) -> {
            short shortValue = ByteBuffer.wrap(val).order(ByteOrder.BIG_ENDIAN).getShort();
            return acc.append((char) (shortValue >> 8)).append((char) (shortValue & 0xFF));
        }, StringBuilder::append).toString();

        updateState(channelUID, new StringType(stringValue));
    }

    private void updateChannelWithNumericValue(ParameterItem parameterItem, ChannelUID channelUID,
            List<Integer> registers, Map<Integer, byte[]> readRegistersMap) {
        BigInteger value = extractNumericValue(registers, readRegistersMap);
        BigDecimal convertedValue = convertNumericValue(value, parameterItem.getOffset(), parameterItem.getScale());
        if (validateNumericValue(convertedValue, parameterItem.getValidation())) {
            State state;
            if (parameterItem.getUom() != null) {
                Unit<?> uom = SimpleUnitFormat.getInstance().parse(parameterItem.getUom());
                state = new QuantityType<>(convertedValue, uom);
            } else {
                state = new DecimalType(convertedValue);
            }
            updateState(channelUID, state);
        }
    }

    private void updateChannelsForRawValue(ParameterItem parameterItem, ChannelUID channelUID, List<Integer> registers,
            Map<Integer, byte[]> readRegistersMap) {
        String hexString = String.format("[%s]",
                reverse(registers).stream().map(readRegistersMap::get).map(
                        val -> String.format("0x%02X", ByteBuffer.wrap(val).order(ByteOrder.BIG_ENDIAN).getShort()))
                        .collect(Collectors.joining(",")));

        updateState(channelUID, new StringType(hexString));
    }

    private boolean validateNumericValue(BigDecimal convertedValue, Validation validation) {
        return true;
    }

    private BigDecimal convertNumericValue(BigInteger value, @Nullable BigDecimal offset, @Nullable BigDecimal scale) {
        return new BigDecimal(value).subtract(offset != null ? offset : BigDecimal.ZERO)
                .multiply(scale != null ? scale : BigDecimal.ONE);
    }

    private BigInteger extractNumericValue(List<Integer> registers, Map<Integer, byte[]> readRegistersMap) {
        return reverse(registers).stream().map(readRegistersMap::get).reduce(BigInteger.ZERO,
                (acc, val) -> acc.shiftLeft(Short.SIZE).add(BigInteger.valueOf(ByteBuffer.wrap(val).getShort())),
                BigInteger::add);
    }

    private Collection<Object> reverse(List<Integer> list) {
        return list.stream().reduce(new ArrayList<>(), (l, i) -> {
            l.add(0, i);
            return l;
        }, (l1, l2) -> {
            l2.addAll(l1);
            return l2;
        });
    }

    private Map<ParameterItem, ChannelUID> setupChannelsForInverterDefinition(InverterDefinition inverterDefinition) {
        removeExistingChannels();

        ThingBuilder thingBuilder = editThing();
        Map<ParameterItem, Channel> paramItemChannelMap = inverterDefinition.getParameters().stream()
                .flatMap(parameter -> {
                    String groupName = escapeName(parameter.getGroup());
                    ChannelGroupTypeUID channelGroupTypeUID = this.channelGroupTypeProvider
                            .registerChannelGroupType(StringUtils.lowerCase(groupName));
                    thingTypeProvider.registerChannelGroupDefinitions(List.of(new ChannelGroupDefinition(
                            StringUtils.lowerCase(groupName), channelGroupTypeUID, groupName, groupName)));

                    return parameter.getItems().stream().map(item -> {
                        String channelId = escapeName(item.getName());
                        String channelTypeId = escapeName(item.getName());

                        final ChannelTypeUID channelTypeUID = new ChannelTypeUID(
                                SolarmanBindingConstants.SOLARMAN_BINDING_ID, channelTypeId);

                        this.channelTypeProvider.registerChannelType(channelTypeUID, item);

                        return Pair.of(item, ChannelBuilder
                                .create(new ChannelUID(new ChannelGroupUID(thing.getUID(), channelGroupTypeUID.getId()),
                                        channelId))
                                .withType(channelTypeUID).withKind(ChannelKind.STATE)
                                .withConfiguration(new Configuration()).build());
                    });
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        thingBuilder.withChannels(new ArrayList<>(paramItemChannelMap.values()));

        updateThing(thingBuilder.build());

        return paramItemChannelMap.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().getUID()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static String escapeName(String name) {
        name = name.replace("+", "plus");
        name = name.replace("-", "minus");
        return StringUtils.replaceChars(StringUtils.lowerCase(name), " .()/\\", "_");
    }

    private void removeExistingChannels() {
        ThingBuilder thingBuilder = editThing();
        thing.getChannels().forEach(channel -> {
            thingBuilder.withoutChannel(channel.getUID());
        });
        updateThing(thingBuilder.build());
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
