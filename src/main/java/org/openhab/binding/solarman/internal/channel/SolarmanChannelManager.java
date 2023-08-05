package org.openhab.binding.solarman.internal.channel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.openhab.binding.solarman.internal.defmodel.InverterDefinition;
import org.openhab.binding.solarman.internal.defmodel.ParameterItem;
import org.openhab.binding.solarman.internal.typeprovider.ChannelUtils;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelKind;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.openhab.binding.solarman.internal.SolarmanBindingConstants.DYNAMIC_CHANNEL;
import static org.openhab.binding.solarman.internal.typeprovider.ChannelUtils.escapeName;

public class SolarmanChannelManager {
    private final ObjectMapper objectMapper;

    public SolarmanChannelManager() {
        objectMapper = new ObjectMapper();
    }

    public Map<ParameterItem, Channel> generateItemChannelMap(Thing thing, InverterDefinition inverterDefinition) {
        return inverterDefinition.getParameters().stream()
                .flatMap(parameter -> {
                    String groupName = escapeName(parameter.getGroup());

                    return parameter.getItems().stream().map(item -> {
                        String channelId = groupName + "_" + escapeName(item.getName());

                        return Pair.of(item,
                                ChannelBuilder
                                        .create(new ChannelUID(thing.getUID(), channelId))
                                        .withType(ChannelUtils.channelType(item))
                                        .withLabel(item.getName())
                                        .withKind(ChannelKind.STATE)
                                        .withAcceptedItemType(ChannelUtils.getItemType(item))
                                        .withProperties(Map.of(DYNAMIC_CHANNEL, Boolean.TRUE.toString()))
                                        .withConfiguration(buildConfigurationFromItem(item)).build()
                        );
                    });
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    private Configuration buildConfigurationFromItem(ParameterItem item) {
        Configuration configuration = new Configuration();

        BaseChannelConfig baseChannelConfig = new BaseChannelConfig();

        baseChannelConfig.offset = item.getOffset();
        baseChannelConfig.rule = item.getRule();
        baseChannelConfig.registers = convertRegisters(item.getRegisters());
        baseChannelConfig.scale = item.getScale();
        baseChannelConfig.uom = item.getUom();

        Map<String, Object> configurationMap = objectMapper
                .convertValue(baseChannelConfig, new TypeReference<>() {
                });

        configurationMap.forEach(configuration::put);

        return configuration;
    }

    private String convertRegisters(List<Integer> registers) {
        return "[" +
                registers.stream()
                        .map(register -> String.format("0x%04X", register))
                        .collect(Collectors.joining(",")) +
                "]";
    }
}
