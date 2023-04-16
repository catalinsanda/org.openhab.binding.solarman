package org.openhab.binding.solarman.internal.typeprovider;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarman.internal.SolarmanBindingConstants;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupDefinition;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;
import org.openhab.core.thing.type.ThingTypeRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ThingTypeProvider.class, SolarmanThingTypeProvider.class })
@NonNullByDefault
public class SolarmanThingTypeProvider implements ThingTypeProvider {

    private final Map<ThingTypeUID, ThingType> thingTypeMap = new ConcurrentHashMap<>();

    private final ThingTypeRegistry thingTypeRegistry;

    @Activate
    public SolarmanThingTypeProvider(@Reference ThingTypeRegistry thingTypeRegistry) {
        this.thingTypeRegistry = thingTypeRegistry;
    }

    @Override
    public Collection<ThingType> getThingTypes(@Nullable Locale locale) {
        return List.copyOf(thingTypeMap.values());
    }

    @Override
    public @Nullable ThingType getThingType(ThingTypeUID thingTypeUID, @Nullable Locale locale) {
        return thingTypeMap.get(thingTypeUID);
    }

    public void registerChannelGroupDefinitions(List<ChannelGroupDefinition> channelGroupDefinitions) {
        @Nullable
        ThingType thingType = thingTypeMap.get(SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER);

        if (thingType == null) {
            thingType = thingTypeRegistry.getThingType(SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER);
            if (thingType == null) {
                throw new RuntimeException(
                        "Unable to find thing with ID " + SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER);
            }
        }

        List<ChannelGroupDefinition> existingChannelGroupDefinitions = thingType.getChannelGroupDefinitions();
        @Nullable
        ThingTypeBuilder thingTypeBuilder = createThingTypeBuilder(SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER);
        if (thingTypeBuilder == null) {
            throw new RuntimeException("Unable to create thing builder");
        }

        thingTypeBuilder.withChannelGroupDefinitions(
                Stream.concat(existingChannelGroupDefinitions.stream(), channelGroupDefinitions.stream())
                        .collect(Collectors.toList()));

        thingTypeMap.put(SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER, thingTypeBuilder.build());
    }

    private @Nullable ThingTypeBuilder createThingTypeBuilder(ThingTypeUID thingTypeUID) {
        ThingType type = thingTypeRegistry.getThingType(thingTypeUID);

        if (type == null) {
            return null;
        }

        ThingTypeBuilder result = ThingTypeBuilder.instance(thingTypeUID, type.getLabel())
                .withChannelGroupDefinitions(type.getChannelGroupDefinitions())
                .withChannelDefinitions(type.getChannelDefinitions())
                .withExtensibleChannelTypeIds(type.getExtensibleChannelTypeIds())
                .withSupportedBridgeTypeUIDs(type.getSupportedBridgeTypeUIDs()).withProperties(type.getProperties())
                .isListed(false);

        String representationProperty = type.getRepresentationProperty();
        if (representationProperty != null) {
            result = result.withRepresentationProperty(representationProperty);
        }
        URI configDescriptionURI = type.getConfigDescriptionURI();
        if (configDescriptionURI != null) {
            result = result.withConfigDescriptionURI(configDescriptionURI);
        }
        String category = type.getCategory();
        if (category != null) {
            result = result.withCategory(category);
        }
        String description = type.getDescription();
        if (description != null) {
            result = result.withDescription(description);
        }

        return result;
    }
}
