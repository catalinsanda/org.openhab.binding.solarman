/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.solarman.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.openhab.binding.solarman.internal.defmodel.InverterDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * The {@link DefinitionParser} is parses inverter definitions
 *
 * @author Catalin Sanda - Initial contribution
 */
public class DefinitionParser {
    private final Logger logger = LoggerFactory.getLogger(DefinitionParser.class);

    private final ObjectMapper mapper;

    public DefinitionParser() {
        mapper = new ObjectMapper(new YAMLFactory());
    }

    public InverterDefinition parseDefinition(String definitionId) {
        ClassLoader cl = Objects.requireNonNull(getClass().getClassLoader());
        String definitionFileName = String.format("definitions/%s.yaml", definitionId);
        try (InputStream is = cl.getResourceAsStream(definitionFileName)) {
            if (is == null) {
                logger.error("Unable to read definition file {}", definitionFileName);
                return null;
            }
            return mapper.readValue(is, InverterDefinition.class);
        } catch (IOException e) {
            logger.error("Error parsing definition with ID: {}", definitionId, e);
            return null;
        }
    }
}
