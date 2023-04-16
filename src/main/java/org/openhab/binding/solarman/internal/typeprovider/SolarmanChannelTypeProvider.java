package org.openhab.binding.solarman.internal.typeprovider;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarman.internal.defmodel.ParameterItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.type.*;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.osgi.service.component.annotations.Component;

@Component(service = { ChannelTypeProvider.class, SolarmanChannelTypeProvider.class })
@NonNullByDefault
public class SolarmanChannelTypeProvider implements ChannelTypeProvider {
    private final Map<ChannelTypeUID, ChannelType> channelTypeMap = new ConcurrentHashMap<>();

    public Collection<ChannelType> getChannelTypes(@Nullable Locale locale) {
        return List.copyOf(this.channelTypeMap.values());
    }

    @Override
    public @Nullable ChannelType getChannelType(ChannelTypeUID channelTypeUID, @Nullable Locale locale) {
        return this.channelTypeMap.get(channelTypeUID);
    }

    public void registerChannelType(ChannelTypeUID channelTypeUID, ParameterItem item) {
        StateDescriptionFragmentBuilder stateDescriptionFragmentBuilder = StateDescriptionFragmentBuilder.create()
                .withReadOnly(true);

        String itemType = switch (item.getRule()) {
            case 0, 1, 2, 3, 4 -> CoreItemFactory.NUMBER;
            case 5 -> CoreItemFactory.STRING;
            case 6 -> CoreItemFactory.STRING; // Display as HEX
            default -> CoreItemFactory.NUMBER;
        };

        StateChannelTypeBuilder stateChannelTypeBuilder = ChannelTypeBuilder
                .state(channelTypeUID, item.getName(), itemType)
                .withDescription(String.format("%s %s", item.getName(), buildRegisterDescription(item)))
                .withStateDescriptionFragment(stateDescriptionFragmentBuilder.build());

        channelTypeMap.put(channelTypeUID, stateChannelTypeBuilder.build());
    }

    private String buildRegisterDescription(ParameterItem item) {
        return String.format("[%s]", item.getRegisters().stream().map(register -> String.format("0x%04X", register))
                .collect(Collectors.joining(",")));
    }
}
