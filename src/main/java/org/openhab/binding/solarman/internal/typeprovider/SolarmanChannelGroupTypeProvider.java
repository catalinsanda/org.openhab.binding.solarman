package org.openhab.binding.solarman.internal.typeprovider;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarman.internal.SolarmanBindingConstants;
import org.openhab.core.thing.type.ChannelGroupType;
import org.openhab.core.thing.type.ChannelGroupTypeBuilder;
import org.openhab.core.thing.type.ChannelGroupTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeUID;
import org.osgi.service.component.annotations.Component;

/**
 * @author Catalin Sanda - Initial contribution
 */
@Component(service = { ChannelGroupTypeProvider.class, SolarmanChannelGroupTypeProvider.class })
@NonNullByDefault
public class SolarmanChannelGroupTypeProvider implements ChannelGroupTypeProvider {
    private final Map<ChannelGroupTypeUID, ChannelGroupType> channelGroupTypeMap = new ConcurrentHashMap<>();

    @Override
    public @Nullable ChannelGroupType getChannelGroupType(ChannelGroupTypeUID channelGroupTypeUID,
            @Nullable Locale locale) {
        return channelGroupTypeMap.get(channelGroupTypeUID);
    }

    @Override
    public Collection<ChannelGroupType> getChannelGroupTypes(@Nullable Locale locale) {
        return List.copyOf(channelGroupTypeMap.values());
    }

    public ChannelGroupTypeUID registerChannelGroupType(String chanelGroup) {
        ChannelGroupTypeUID channelGroupTypeUID = new ChannelGroupTypeUID(SolarmanBindingConstants.SOLARMAN_BINDING_ID,
                chanelGroup);
        ChannelGroupType channelGroupType = ChannelGroupTypeBuilder.instance(channelGroupTypeUID, chanelGroup).build();
        channelGroupTypeMap.put(channelGroupTypeUID, channelGroupType);
        return channelGroupTypeUID;
    }
}
