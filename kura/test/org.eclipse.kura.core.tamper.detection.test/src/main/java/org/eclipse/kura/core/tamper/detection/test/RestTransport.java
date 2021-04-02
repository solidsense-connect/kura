/*******************************************************************************
 * Copyright (c) 2021 Eurotech and/or its affiliates and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *  Eurotech
 ******************************************************************************/
package org.eclipse.kura.core.tamper.detection.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.kura.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestTransport implements Transport {

    private static final Encoder ENCODER = Base64.getEncoder();
    private static final Logger logger = LoggerFactory.getLogger(RestTransport.class);

    private static final String BASE_URL = "http://localhost:8080/services/tamper/";

    private boolean initialized = false;

    @Override
    public void init() {
        if (initialized) {
            return;
        }

        try {
            final ConfigurationService configurationService = ServiceUtil
                    .trackService(ConfigurationService.class, Optional.empty()).get(1, TimeUnit.MINUTES);

            configurationService.updateConfiguration("org.eclipse.kura.http.server.manager.HttpService",
                    Collections.singletonMap("http.ports", new Integer[] { 8080 }), false);
            waitPortOpen("localhost", 8080, 30, TimeUnit.SECONDS);
            Thread.sleep(5000);
            initialized = true;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String runRequestAndGetResponse(String resource, String method) {
        try {
            return runRequest(resource, method,
                    conn -> IOUtils.toString(conn.getInputStream(), StandardCharsets.UTF_8));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int runRequestAndGetStatus(String resource, String method) {
        try {
            return runRequest(resource, method, conn -> {
                conn.connect();
                return conn.getResponseCode();
            });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T runRequest(final String relativeUri, final String method, final ResponseCollector<T> collector)
            throws MalformedURLException, IOException {
        final HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + relativeUri).openConnection();
        final String encoded = ENCODER.encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encoded);
        connection.setRequestMethod(method);

        return collector.collect(connection);
    }

    private static void waitPortOpen(final String url, final int port, final long timeout, final TimeUnit timeoutUnit)
            throws InterruptedException {
        final long now = System.nanoTime();

        while (System.nanoTime() - now < timeoutUnit.toNanos(timeout)) {
            try {
                new Socket(url, port).close();
                logger.info("port open");
                return;
            } catch (final Exception e) {
                logger.warn("failed to connect");
            }
            Thread.sleep(1000);
        }
    }

    private interface ResponseCollector<T> {

        T collect(final HttpURLConnection conn) throws IOException;
    }

}
