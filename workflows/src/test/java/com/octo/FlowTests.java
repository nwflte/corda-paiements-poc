package com.octo;

import com.google.common.collect.ImmutableList;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FlowTests {
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
        TestCordapp.findCordapp("com.octo.contracts"),
        TestCordapp.findCordapp("com.octo.flows")
    )));

    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();

    /*public FlowTests() {
        a.registerInitiatedFlow(Responder.class);
        b.registerInitiatedFlow(Responder.class);
    }*/

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void dummyTest() {

    }
}
