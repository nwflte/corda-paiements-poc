package com.octo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.octo.flows.CreateBankStateFlow;
import com.octo.states.BankBalanceState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.testing.driver.Driver.driver;
import static org.junit.Assert.assertEquals;

public class DriverBasedTest {
    private final TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "London", "GB"));
    private final TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "New York", "US"));
    private final TestIdentity centralBank = new TestIdentity(new CordaX500Name("CentralBank", "New York", "US"));

    @Test
    public void nodeTest() {
        driver(new DriverParameters().withIsDebug(true).withStartNodesInProcess(true), dsl -> {
            // Start a pair of nodes and wait for them both to be ready.
            List<User> users = Arrays.asList(new User("user1", "test", ImmutableSet.of("ALL")));
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(bankA.getName()).withRpcUsers(users)),
                    dsl.startNode(new NodeParameters().withProvidedName(bankB.getName()).withRpcUsers(users)),
                    dsl.startNode(new NodeParameters().withProvidedName(centralBank.getName()).withRpcUsers(users))
            );

            try {
                NodeHandle partyAHandle = handleFutures.get(0).get();
                NodeHandle partyBHandle = handleFutures.get(1).get();
                NodeHandle partyCBHandle = handleFutures.get(2).get();
                // From each node, make an RPC call to retrieve another node's name from the network map, to verify that the
                // nodes have started and can communicate.

                // This is a very basic test: in practice tests would be starting flows, and verifying the states in the vault
                // and other important metrics to ensure that your CorDapp is working as intended.
                assertEquals(partyAHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()).getName(), bankB.getName());
                assertEquals(partyBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()).getName(), bankA.getName());

                // Test CreateBalanceFlow
                partyCBHandle.getRpc().startFlowDynamic(CreateBankStateFlow.class, partyCBHandle.getRpc().wellKnownPartyFromX500Name(bankB.getName()), 10000).getReturnValue().get();
                partyCBHandle.getRpc().startFlowDynamic(CreateBankStateFlow.class, partyCBHandle.getRpc().wellKnownPartyFromX500Name(bankA.getName()), 10000).getReturnValue().get();


                List<BankBalanceState> statesA = partyAHandle.getRpc().vaultQuery(BankBalanceState.class)
                        .getStates().stream().map(st -> st.getState().getData()).collect(Collectors.toList());
                List<BankBalanceState> statesB = partyBHandle.getRpc().vaultQuery(BankBalanceState.class)
                        .getStates().stream().map(st -> st.getState().getData()).collect(Collectors.toList());

                for (BankBalanceState bbs : statesA)
                    assertEquals((Integer) 10000, bbs.getAmount());
                for (BankBalanceState bbs : statesB)
                    assertEquals((Integer) 10000, bbs.getAmount());

            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test: ", e);
            }

            return null;
        });
    }
}