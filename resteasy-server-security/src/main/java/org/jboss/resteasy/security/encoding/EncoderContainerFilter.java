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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;

import org.jboss.resteasy.security.encoding.spi.Encoder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@PreMatching
@Provider
public class EncoderContainerFilter implements ContainerRequestFilter {

    @Context
    private Providers providers;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final Encoder encoder = getEncoder(resolveMediaType(requestContext));

        if (encoder != null) {
            final UriInfo uriInfo = requestContext.getUriInfo();

            final UriBuilder uriBuilder = uriInfo.getBaseUriBuilder();

            // Filter path parameters
            final List<PathSegment> pathSegments = uriInfo.getPathSegments(true);
            for (PathSegment segment : pathSegments) {
                // TODO (jrp) we need to do something better with /
                uriBuilder.path(encoder.encode(segment.getPath()));

                // Filter matrix parameters
                segment.getMatrixParameters().forEach((name, values) -> {
                    final Object[] replacementValues = new Object[values.size()];
                    int i = 0;
                    for (String value : values) {
                        replacementValues[i++] = encoder.encode(value);
                    }
                    uriBuilder.replaceMatrixParam(name, replacementValues);
                });
            }

            // Filter query parameters
            final MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters(true);
            queryParameters.forEach((name, values) -> {
                final Object[] replacementValues = new Object[values.size()];
                int i = 0;
                for (String value : values) {
                    replacementValues[i++] = encoder.encode(value);
                }
                uriBuilder.replaceQueryParam(name, replacementValues);
            });

            final URI uri = uriBuilder.build();
            Logger.getLogger(EncoderContainerFilter.class.getName()).warning(String.format("New URI: %s", uri));
            requestContext.setRequestUri(uri);
        }
    }

    private Encoder getEncoder(final MediaType mediaType) {
        final ContextResolver<Encoder> context = providers.getContextResolver(Encoder.class, mediaType);
        final Encoder encoder = (context == null ? null : context.getContext(Encoder.class));
        return encoder == null ? null : value -> {
            try {
                return URLEncoder.encode(encoder.encode(value), "utf-8");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private MediaType resolveMediaType(final ContainerRequestContext requestContext) {
        // TODO (jrp) this is RESTEasy specific, but also isn't set for @PreMatching RESTEASY_CHOSEN_ACCEPT
        final Object resteasyValue = requestContext.getProperty("RESTEASY_CHOSEN_ACCEPT");
        if (resteasyValue instanceof MediaType) {
            return (MediaType) resteasyValue;
        }

        final List<MediaType> acceptableMediaTypes = requestContext.getAcceptableMediaTypes();
        if (acceptableMediaTypes.size() == 1) {
            return acceptableMediaTypes.get(0);
        }
        // TODO (jrp) we refine more or process each type?
        return MediaType.WILDCARD_TYPE;
    }
}
