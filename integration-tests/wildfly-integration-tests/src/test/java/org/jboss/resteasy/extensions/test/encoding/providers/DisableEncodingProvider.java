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

package org.jboss.resteasy.extensions.test.encoding.providers;

import javax.annotation.Priority;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.security.encoding.spi.Encoder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@Provider
@Produces({
        MediaType.APPLICATION_ATOM_XML,
        MediaType.APPLICATION_SVG_XML,
        MediaType.APPLICATION_XHTML_XML,
        MediaType.APPLICATION_XML,
        MediaType.TEXT_XML,
        MediaType.TEXT_HTML
})
@Priority(500)
public class DisableEncodingProvider implements ContextResolver<Encoder> {
    @Override
    public Encoder getContext(final Class<?> type) {
        return (value) -> (value == null ? null : value.toString());
    }
}
