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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.Collections;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.Produces;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.security.encoding.annotations.EncodeEntity;
import org.jboss.resteasy.security.encoding.annotations.EncodeParameter;
import org.jboss.resteasy.security.encoding.spi.Encoder;
import org.jboss.resteasy.security.encoding.util.MediaTypes;

/**
 * A provider used to dynamically register encoders for parameters and entities.
 * <p>
 * Encoding can be disabled for a type, method or parameter with the {@link EncodeParameter} annotation by setting the
 * value to {@code false}.
 * </p>
 * <p>
 * If a type or method is found annotated with the {@link EncodeEntity} parameter and the
 * {@linkplain EncodeEntity#encode() encode} value is set to {@code true} the entity will be encoded. See the
 * {@link EncodeEntityReaderInterceptor} for details on how the entity is encoded.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see EncodeEntityReaderInterceptor
 */
@Provider
@ConstrainedTo(RuntimeType.SERVER)
public class EncoderFeature implements DynamicFeature {

    @Context
    private Providers providers;

    @Override
    public void configure(final ResourceInfo resourceInfo, final FeatureContext context) {
        final Method method = resourceInfo.getResourceMethod();
        final Class<?> type = resourceInfo.getResourceClass();

        // Register the entity encoder if annotated
        final EncodeEntity encodeEntity;
        if (method.isAnnotationPresent(EncodeEntity.class)) {
            encodeEntity = method.getAnnotation(EncodeEntity.class);
        } else {
            encodeEntity = type.getAnnotation(EncodeEntity.class);
        }
        // TODO (jrp) we should likely do this as different producers might have different encoders
        final Collection<MediaType> mediaTypes = resolveProducedMediaType(resourceInfo);
        final ContextResolver<Encoder> encoderContext = providers.getContextResolver(Encoder.class, MediaType.APPLICATION_XHTML_XML_TYPE);
        if (encoderContext == null) {
            return;
        }

        final Encoder encoder = encoderContext.getContext(Encoder.class);
        if (encoder == null) {
            return;
        }

        if (encodeEntity != null && encodeEntity.encode()) {
            context.register(new EncodeEntityReaderInterceptor(encoder, encodeEntity));
        }

        final EncodeParameter encodeParameter;
        if (method.isAnnotationPresent(EncodeParameter.class)) {
            encodeParameter = method.getAnnotation(EncodeParameter.class);
        } else {
            encodeParameter = type.getAnnotation(EncodeParameter.class);
        }

        boolean register = true;
        if (encodeParameter != null) {
            if (!encodeParameter.value()) {
                // The type or method were annotated with @EncodeParameter(false)
                register = false;
                // Check the parameters for the annotation
                for (Parameter parameter : method.getParameters()) {
                    if (parameter.isAnnotationPresent(EncodeParameter.class)) {
                        // If the annotation is set to true we need to register the provider in strict mode
                        if (parameter.getAnnotation(EncodeParameter.class).value()) {
                            register = true;
                            break;
                        }
                    }
                }
            }
        }
        if (register) {
            context.register(new EncoderContainerFilter());
            //context.register(new EncoderParamConverterProvider(encoder));
        }
    }

    private static Collection<MediaType> resolveProducedMediaType(final ResourceInfo resourceInfo) {
        // First attempt to get the media types from the method
        final Method method = resourceInfo.getResourceMethod();
        Produces produces = method.getAnnotation(Produces.class);
        if (produces == null) {
            produces = resourceInfo.getResourceClass().getAnnotation(Produces.class);
        }
        return produces == null ? Collections.singleton(MediaType.WILDCARD_TYPE) : MediaTypes.getMediaTypes(produces.value());
    }
}

