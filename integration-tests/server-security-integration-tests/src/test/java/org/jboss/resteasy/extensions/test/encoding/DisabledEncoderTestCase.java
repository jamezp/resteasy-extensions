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

package org.jboss.resteasy.extensions.test.encoding;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.resteasy.extensions.test.TestDeployment;
import org.jboss.resteasy.extensions.test.encoding.providers.DisableEncodingProvider;
import org.jboss.resteasy.extensions.test.encoding.resources.ParameterConversionResource;
import org.jboss.resteasy.extensions.test.encoding.resources.PlainTextResource;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ExtendWith(ArquillianExtension.class)
public class DisabledEncoderTestCase extends AbstractEncoderTestCase {

    private static final String DISABLED_DEPLOYMENT = "disabled-encoder.war";
    private static final String TEXT_ONLY_DEPLOYMENT = "text-only.war";

    @Deployment(name = DISABLED_DEPLOYMENT)
    public static Archive<?> createDisabledDeployment() {
        return TestDeployment.createWar(DISABLED_DEPLOYMENT, "org.jboss.resteasy:resteasy-server-security")
                .addClasses(AbstractEncoderTestCase.class, ParameterConversionResource.class, DisableEncodingProvider.class);
    }

    @Deployment(name = TEXT_ONLY_DEPLOYMENT)
    public static Archive<?> createTextDeployment() {
        return TestDeployment.createWar(TEXT_ONLY_DEPLOYMENT, "org.jboss.resteasy:resteasy-server-security")
                .addClasses(AbstractEncoderTestCase.class, PlainTextResource.class);
    }

    // TODO (jrp) should this be here? We really want to make sure this is disabled by default
    @Test
    @OperateOnDeployment(TEXT_ONLY_DEPLOYMENT)
    public void pathParamGetText() throws Exception {
        final String url = createUrl("text", "param", SCRIPT_PARAM);
        final Response response = client.target(url)
                .request(MediaType.TEXT_PLAIN_TYPE)
                .get();
        validateResponse(response, "Hello " + SCRIPT_PARAM);
    }

    @Test
    @OperateOnDeployment(DISABLED_DEPLOYMENT)
    public void pathParamGetHtml() throws Exception {
        final String url = createUrl("greet", "param", SCRIPT_PARAM);
        final Response response = client.target(url)
                .request(MediaType.TEXT_HTML_TYPE)
                .get();
        // TODO (jrp) do this better
        validateResponse(response, String.format("<h1>Hello %s</h1>", SCRIPT_PARAM));
    }
}
