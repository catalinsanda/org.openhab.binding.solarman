package org.openhab.binding.solarman.internal.channel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

import java.math.BigDecimal;

@NonNullByDefault
public class BaseChannelConfig {
    public @Nullable String uom;
    public BigDecimal scale = BigDecimal.ONE;
    public Integer rule = 1;
    public BigDecimal offset = BigDecimal.ZERO;
    public String registers = "";
}