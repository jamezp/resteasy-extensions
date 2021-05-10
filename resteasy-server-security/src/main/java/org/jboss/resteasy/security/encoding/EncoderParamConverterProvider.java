/*
 * Copyright (c) 2021 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.jboss.resteasy.security.encoding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.security.encoding.annotations.EncodeParameter;
import org.jboss.resteasy.security.encoding.spi.Encoder;

/**
 * A provider which determines if a {@link EncoderParamConverter} should be used.
 * <p>
 * For a {@link EncoderParamConverter} the parameter must be a {@link String}.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see EncoderParamConverter
 */
public class EncoderParamConverterProvider implements ParamConverterProvider {
    private final ParamConverter<String> converter;

    @Context
    private Providers providers;

    /**
     * Creates a new provider.
     *
     * @param encoder the encoder used for the parameter converter
     */
    public EncoderParamConverterProvider(final Encoder encoder) {
        this.converter = new EncoderParamConverter(encoder);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(final Class<T> rawType, final Type genericType, final Annotation[] annotations) {
        if (rawType.isAssignableFrom(String.class) && isEncodeParameter(annotations)) {
            return (ParamConverter<T>) converter;
        }
        return null;
    }

    // TODO (jrp) rename this
    private boolean isEncodeParameter(final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof EncodeParameter) {
                return ((EncodeParameter) annotation).value();
            }
        }
        return true;
    }
}
