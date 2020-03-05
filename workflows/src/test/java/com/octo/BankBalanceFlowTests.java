package com.octo;

import com.google.common.collect.ImmutableList;
import com.octo.flows.CreateBankStateFlow;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.MockNetworkParameters;
import net.corda.testing.node.StartedMockNode;
import net.corda.testing.node.TestCordapp;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;

public class BankBalanceFlowTests {
    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode bc = network.createNode();

    public BankBalanceFlowTests() {

    }

    @Before
    public void setup() {
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRejectsNegativeBankBalance() throws Exception {
        CreateBankStateFlow flow = new CreateBankStateFlow(a.getInfo().getLegalIdentities().get(0), -1000);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void flowRejectsCreatingBankBalanceIfExists() throws Exception {

    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        CreateBankStateFlow flow = new CreateBankStateFlow(a.getInfo().getLegalIdentities().get(0), 1000);
        CordaFuture<SignedTransaction> future = bc.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(a, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

}
