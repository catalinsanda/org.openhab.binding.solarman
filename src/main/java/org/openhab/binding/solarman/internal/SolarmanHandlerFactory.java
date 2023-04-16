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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanChannelGroupTypeProvider;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanChannelTypeProvider;
import org.openhab.binding.solarman.internal.typeprovider.SolarmanThingTypeProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link SolarmanHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Catalin Sanda - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.solarman", service = ThingHandlerFactory.class)
public class SolarmanHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set
            .of(SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER);

    private final SolarmanChannelTypeProvider channelTypeProvider;
    private final SolarmanChannelGroupTypeProvider channelGroupTypeProvider;
    private final SolarmanThingTypeProvider thingTypeProvider;

    @Activate
    public SolarmanHandlerFactory(final @Reference SolarmanChannelTypeProvider channelTypeProvider,
            final @Reference SolarmanChannelGroupTypeProvider channelGroupTypeProvider,
            final @Reference SolarmanThingTypeProvider thingTypeProvider) {
        this.channelTypeProvider = channelTypeProvider;
        this.channelGroupTypeProvider = channelGroupTypeProvider;
        this.thingTypeProvider = thingTypeProvider;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SolarmanBindingConstants.THING_TYPE_SOLARMAN_LOGGER.equals(thingTypeUID)) {
            return new SolarmanLoggerHandler(thing, this.channelTypeProvider, this.channelGroupTypeProvider,
                    this.thingTypeProvider);
        }

        return null;
    }
}
