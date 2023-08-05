package org.openhab.binding.solarman.internal.typeprovider;

import org.apache.commons.lang3.StringUtils;
import org.openhab.binding.solarman.internal.SolarmanBindingConstants;
import org.openhab.binding.solarman.internal.defmodel.ParameterItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.thing.type.ChannelTypeUID;

public class ChannelUtils {
    public static String getItemType(ParameterItem item) {
        return switch (item.getRule()) {
            case 5, 6, 7, 9 -> CoreItemFactory.STRING;
            case 8 -> CoreItemFactory.DATETIME;
            default -> CoreItemFactory.NUMBER;
        };
    }

    public static ChannelTypeUID channelType(ParameterItem item) {
        return switch (item.getRule()) {
            case 5, 6, 7, 9 -> new ChannelTypeUID(SolarmanBindingConstants.SOLARMAN_BINDING_ID, "string");
            case 8 -> new ChannelTypeUID(SolarmanBindingConstants.SOLARMAN_BINDING_ID, "datetime");
            default -> new ChannelTypeUID(SolarmanBindingConstants.SOLARMAN_BINDING_ID, "number");
        };
    }

    public static String escapeName(String name) {
        name = name.replace("+", "plus");
        name = name.replace("-", "minus");
        return StringUtils.replaceChars(StringUtils.lowerCase(name), " .()/\\&", "_");
    }
}
