/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jclouds;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.Image;
import org.jclouds.compute.domain.NodeMetadata;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class JcloudsSpringComputeTest extends CamelSpringTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint result;

    @After
    public void tearDown() throws Exception {
        template.sendBodyAndHeaders("direct:start", null, destroyHeaders(null, null));
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("classpath:compute-test.xml");
    }

    @Test
    public void testListImages() throws InterruptedException {
        template.sendBodyAndHeader("direct:start", null, JcloudsConstants.OPERATION, JcloudsConstants.LIST_IMAGES);
        result.expectedMessageCount(1);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> images = exchange.getIn().getBody(Set.class);
                assertTrue(images.size() > 0);
                for (Object obj : images) {
                    assertTrue(obj instanceof Image);
                }
            }
        }
    }

    @Test
    public void testListHardware() throws InterruptedException {
        template.sendBodyAndHeader("direct:start", null, JcloudsConstants.OPERATION, JcloudsConstants.LIST_HARDWARE);
        result.expectedMessageCount(1);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> hardwares = exchange.getIn().getBody(Set.class);
                assertTrue(hardwares.size() > 0);
                for (Object obj : hardwares) {
                    assertTrue(obj instanceof Hardware);
                }
            }
        }
    }

    @Test
    public void testListNodes() throws InterruptedException {
        template.sendBodyAndHeader("direct:start", null, JcloudsConstants.OPERATION, JcloudsConstants.LIST_NODES);
        result.expectedMessageCount(1);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> nodeMetadatas = exchange.getIn().getBody(Set.class);
                assertEquals("Nodes should be 0", 0, nodeMetadatas.size());
            }
        }
    }

    @Test
    public void testCreateAndListNodes() throws InterruptedException {
        template.sendBodyAndHeaders("direct:start", null, createHeaders("1", "default"));

        template.sendBodyAndHeader("direct:start", null, JcloudsConstants.OPERATION, JcloudsConstants.LIST_NODES);
        result.expectedMessageCount(2);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> nodeMetadatas = exchange.getIn().getBody(Set.class);
                assertEquals("Nodes should be 1", 1, nodeMetadatas.size());
            }
        }
    }


    @Test
    public void testCreateAndListWithPredicates() throws InterruptedException {
        //Create a node for the default group
        template.sendBodyAndHeaders("direct:start", null, createHeaders("1", "default"));

        //Create a node for the group 'other'
        template.sendBodyAndHeaders("direct:start", null, createHeaders("1", "other"));
        template.sendBodyAndHeaders("direct:start", null, createHeaders("2", "other"));

        template.sendBodyAndHeaders("direct:start", null, listNodeHeaders(null, "other", null));
        template.sendBodyAndHeaders("direct:start", null, listNodeHeaders("3", "other", null));
        template.sendBodyAndHeaders("direct:start", null, listNodeHeaders("3", "other", "RUNNING"));

        result.expectedMessageCount(6);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();

        Exchange exchange = exchanges.get(3);
        Set<?> nodeMetadatas = exchange.getIn().getBody(Set.class);
        assertEquals("Nodes should be 2", 2, nodeMetadatas.size());
        NodeMetadata nodeMetadata = nodeMetadatas.toArray(new NodeMetadata[0])[0];
        assertEquals("other", nodeMetadata.getGroup());

        exchange = exchanges.get(4);
        nodeMetadatas = exchange.getIn().getBody(Set.class);
        assertEquals("Nodes should be 1", 1, nodeMetadatas.size());
        nodeMetadata = nodeMetadatas.toArray(new NodeMetadata[0])[0];
        assertEquals("other", nodeMetadata.getGroup());
        assertEquals("3", nodeMetadata.getId());

        exchange = exchanges.get(5);
        nodeMetadatas = exchange.getIn().getBody(Set.class);
        assertEquals("Nodes should be 1", 1, nodeMetadatas.size());
        nodeMetadata = nodeMetadatas.toArray(new NodeMetadata[0])[0];
        assertEquals("other", nodeMetadata.getGroup());
        assertEquals("3", nodeMetadata.getId());
    }

    @Test
    public void testCreateAndDestroyNode() throws InterruptedException {
        template.sendBodyAndHeaders("direct:start", null, createHeaders("1", "default"));
        result.expectedMessageCount(1);
        result.assertIsSatisfied();
        List<Exchange> exchanges = result.getExchanges();
        if (exchanges != null && !exchanges.isEmpty()) {
            for (Exchange exchange : exchanges) {
                Set<?> nodeMetadatas = exchange.getIn().getBody(Set.class);
                assertEquals("There should be no node running", 1, nodeMetadatas.size());

                for (Object obj : nodeMetadatas) {
                    NodeMetadata nodeMetadata = (NodeMetadata) obj;
                    template.sendBodyAndHeaders("direct:start", null, destroyHeaders(nodeMetadata.getId(), null));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Ignore("For now not possible to combine stub provider with ssh module, requird for runScript")
    @Test
    public void testRunScript() throws InterruptedException {
        Map<String, Object> runScriptHeaders = new HashMap<String, Object>();
        runScriptHeaders.put(JcloudsConstants.OPERATION, JcloudsConstants.RUN_SCRIPT);

        Set<? extends NodeMetadata> nodeMetadatas = (Set<? extends NodeMetadata>) template.requestBodyAndHeaders("direct:in-out", null, createHeaders("1", "default"));
        assertEquals("There should be a node running", 1, nodeMetadatas.size());
        for (NodeMetadata nodeMetadata : nodeMetadatas) {
            runScriptHeaders.put(JcloudsConstants.NODE_ID, nodeMetadata.getId());
            template.requestBodyAndHeaders("direct:in-out", null, runScriptHeaders);
            template.sendBodyAndHeaders("direct:in-out", null, destroyHeaders(nodeMetadata.getId(), null));
        }
    }


    /**
     * Returns a {@Map} with the create headers.
     *
     * @param imageId The imageId to use for creating the node.
     * @param group   The group to be assigned to the node.
     * @return
     */
    protected Map<String, Object> createHeaders(String imageId, String group) {
        Map<String, Object> createHeaders = new HashMap<String, Object>();
        createHeaders.put(JcloudsConstants.OPERATION, JcloudsConstants.CREATE_NODE);
        createHeaders.put(JcloudsConstants.IMAGE_ID, imageId);
        createHeaders.put(JcloudsConstants.GROUP, group);
        return createHeaders;
    }


    /**
     * Returns a {@Map} with the destroy headers.
     *
     * @param nodeId The id of the node to destroy.
     * @param group  The group of the node to destroy.
     * @return
     */
    protected Map<String, Object> destroyHeaders(String nodeId, String group) {
        Map<String, Object> destroyHeaders = new HashMap<String, Object>();
        destroyHeaders.put(JcloudsConstants.OPERATION, JcloudsConstants.DESTROY_NODE);
        if (nodeId != null) {
            destroyHeaders.put(JcloudsConstants.NODE_ID, nodeId);
        }
        if (group != null) {
            destroyHeaders.put(JcloudsConstants.GROUP, group);
        }
        return destroyHeaders;
    }

    /**
     * Returns a {@Map} with the destroy headers.
     *
     * @param nodeId The id of the node to destroy.
     * @param group  The group of the node to destroy.
     * @return
     */
    protected Map<String, Object> listNodeHeaders(String nodeId, String group, Object state) {
        Map<String, Object> listHeaders = new HashMap<String, Object>();
        listHeaders.put(JcloudsConstants.OPERATION, JcloudsConstants.LIST_NODES);
        if (nodeId != null) {
            listHeaders.put(JcloudsConstants.NODE_ID, nodeId);
        }

        if (group != null) {
            listHeaders.put(JcloudsConstants.GROUP, group);
        }

        if (state != null) {
            listHeaders.put(JcloudsConstants.NODE_STATE, state);
        }

        return listHeaders;
    }
}
