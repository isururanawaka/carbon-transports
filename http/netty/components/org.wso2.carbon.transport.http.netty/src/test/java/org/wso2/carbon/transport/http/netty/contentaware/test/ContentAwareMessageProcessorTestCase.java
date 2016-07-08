/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.transport.http.netty.contentaware.test;

import io.netty.handler.codec.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.messaging.CarbonMessageProcessor;
import org.wso2.carbon.transport.http.netty.config.ListenerConfiguration;
import org.wso2.carbon.transport.http.netty.config.SenderConfiguration;
import org.wso2.carbon.transport.http.netty.listener.NettyListener;
import org.wso2.carbon.transport.http.netty.util.TestUtil;
import org.wso2.carbon.transport.http.netty.util.server.HTTPServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * A test case for echo message from MessageProcessor level
 */
public class ContentAwareMessageProcessorTestCase {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ContentAwareMessageProcessorTestCase.class);

    private NettyListener nettyListener;

    private ListenerConfiguration listenerConfiguration;

    private SenderConfiguration senderConfiguration;

    private HTTPServer httpServer;
    private URI baseURI = URI.create(String.format("http://%s:%d", "localhost", 8080));

    @BeforeClass(dependsOnGroups = "passthroughPost")
    public void setUp() {
        listenerConfiguration = new ListenerConfiguration();
        listenerConfiguration.setHost(TestUtil.TEST_HOST);
        listenerConfiguration.setId("test-listener");
        listenerConfiguration.setWorkerPoolSize(Runtime.getRuntime().availableProcessors());
        listenerConfiguration.setEnableDisruptor(true);
        listenerConfiguration.setPort(TestUtil.TEST_ESB_PORT);
        senderConfiguration = new SenderConfiguration("passthrough-sender");
        nettyListener = TestUtil
                .startCarbonTransport(listenerConfiguration, senderConfiguration, new MessageEchoingMessageProcessor());
        httpServer = TestUtil.startHTTPServer(TestUtil.TEST_SERVER_PORT);
    }

    @Test(dependsOnGroups = "passthroughPost")
    public void disruptorEnabledMessageEchoingFromProcessorTestCase() {
        String testValue = "Test Message";
        try {
            LOGGER.info("############Runining######" + "disruptorEnabledMessageEchoingFromProcessorTestCase");
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, testValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(testValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "disruptorEnabledMessageEchoingFromProcessorTestCase")
    public void workerPoolEnabledMessageEchoingFromProcessorTestCase() {
        TestUtil.shutDownCarbonTransport(nettyListener);
        listenerConfiguration.setEnableDisruptor(false);
        nettyListener = TestUtil
                .startCarbonTransport(listenerConfiguration, senderConfiguration, new MessageEchoingMessageProcessor());
        String testValue = "Test Message";
        try {
            LOGGER.info("############Runining######" + "workerPoolEnabledMessageEchoingFromProcessorTestCase");
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, testValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(testValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "workerPoolEnabledMessageEchoingFromProcessorTestCase")
    public void requestTransformFromProcessorTestCase() {

        String testValue = "<A><B><C>request</C></B></A>";
        String transformValue = "<A><B><C>transformed</C></B></A>";
        try {
            LOGGER.info("############Runining######" + "requestTransformFromProcessorTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new RequestMessageTransformProcessor(transformValue);
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, testValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(transformValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "requestTransformFromProcessorTestCase")
    public void requestResponseTransformFromProcessorTestCase() {

        String requestValue = "XXXXXXXX";
        String responseValue = "YYYYYYY";
        String expectedValue = responseValue + ":" + requestValue;
        try {
            LOGGER.info("############Runining######" + "requestResponseTransformFromProcessorTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new RequestResponseTransformProcessor(responseValue);
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(expectedValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }
    }

    @Test(dependsOnMethods = "requestResponseTransformFromProcessorTestCase")
    public void requestResponseCreationFromProcessorTestCase() {

        String requestValue = "XXXXXXXX";
        String responseValue = "YYYYYYY";
        String expectedValue = responseValue + ":" + requestValue;
        try {
            LOGGER.info("############Runining######" + "requestResponseCreationFromProcessorTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new RequestResponseCreationProcessor(responseValue);
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(expectedValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "requestResponseCreationFromProcessorTestCase")
    public void requestResponseStreamingFromProcessorTestCase() {

        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            LOGGER.info("############Runining######" + "requestResponseStreamingFromProcessorTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new RequestResponseCreationStreamingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(requestValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "requestResponseStreamingFromProcessorTestCase")
    public void requestResponseTransformStreamingFromProcessorTestCase() {

        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            LOGGER.info("############Runining######" + "requestResponseTransformStreamingFromProcessorTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new RequestResponseTransformStreamingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            TestUtil.writeContent(urlConn, requestValue);
            assertEquals(200, urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            assertEquals(requestValue, content);
            urlConn.disconnect();
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @Test(dependsOnMethods = "requestResponseTransformStreamingFromProcessorTestCase")
    public void responseStreamingWithoutBufferingTestCase() {

        String requestValue = "<A><B><C>Test Message</C></B></A>";
        try {
            LOGGER.info("############Runining######" + "responseStreamingWithoutBufferingTestCase");
            CarbonMessageProcessor carbonMessageProcessor = new ResponseStreamingWithoutBufferingProcessor();
            TestUtil.updateMessageProcessor(carbonMessageProcessor, senderConfiguration);
            HttpURLConnection urlConn = TestUtil.request(baseURI, "/", HttpMethod.POST.name(), true);
            LOGGER.info("############Request sent######");
            TestUtil.writeContent(urlConn, requestValue);
            LOGGER.info("############Response Received######");
            assertEquals(200, urlConn.getResponseCode());
            LOGGER.info("############Response Code######", urlConn.getResponseCode());
            String content = TestUtil.getContent(urlConn);
            LOGGER.info("############Test Content######" + content);
            assertEquals(requestValue, content);
            urlConn.disconnect();
            LOGGER.info("############Test disconnected######");
        } catch (IOException e) {
            LOGGER.error("IO Exception occurred", e);
            assertTrue(false);
        }

    }

    @AfterClass
    public void cleanUp() {
        LOGGER.info("############Cleanup called######");
        TestUtil.cleanUp(nettyListener, httpServer);
    }

}
