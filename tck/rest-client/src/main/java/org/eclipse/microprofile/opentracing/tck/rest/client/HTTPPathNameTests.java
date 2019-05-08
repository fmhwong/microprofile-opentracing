/*
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.opentracing.tck.rest.client;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.opentracing.tck.OpenTracingBaseTests;
import org.eclipse.microprofile.opentracing.tck.application.TestWebServicesApplication;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import io.opentracing.tag.Tags;

/**
 * @author Felix Wong
 */
public class HTTPPathNameTests extends OpenTracingMpRestClientTests {

    public static class TestConfiguration implements ConfigSource {
        private Map<String, String> propMap = new HashMap<>();

        {
            propMap.put("mp.opentracing.server.operation-name-provider", "http-path");
        }

        @Override
        public Map<String, String> getProperties() {
            return propMap;
        }

        @Override
        public String getValue(String s) {
            return propMap.get(s);
        }

        @Override
        public String getName() {
            return this.getClass().getName();
        }
    }

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive deployment = OpenTracingBaseTests.createDeployment();
        deployment.addPackages(true, OpenTracingMpRestClientTests.class.getPackage());
        deployment.deleteClass(TestWebServicesApplication.class.getCanonicalName());
        deployment.addAsServiceProvider(ConfigSource.class, TestConfiguration.class);
        return deployment;
    }

    @Override
    protected String getOperationName(String spanKind, String httpMethod, Class<?> clazz, Method method) {
        if ((spanKind.equals(Tags.SPAN_KIND_SERVER)) || (spanKind.equals(Tags.SPAN_KIND_CLIENT))) {
            StringBuilder operationName = new StringBuilder(httpMethod.toUpperCase() + ":");
            Path classPath = clazz.getAnnotation(Path.class);
            if (classPath == null) {
                throw new IllegalArgumentException("Supplied clazz is not JAX-RS resource");
            }
            if (!classPath.value().startsWith("/")) {
                operationName.append("/");
            }
            operationName.append(classPath.value());
            if (!classPath.value().endsWith("/")) {
                operationName.append("/");
            }
            Path methodPath = method.getAnnotation(Path.class);
            String methodPathStr = methodPath.value();
            if (methodPathStr.startsWith("/")) {
                methodPathStr = methodPathStr.replaceFirst("/", "");
            }
            operationName.append(methodPathStr);
            return operationName.toString();
        }
        else {
            throw new RuntimeException("Span kind " + spanKind + " not implemented");
        }
    }
}