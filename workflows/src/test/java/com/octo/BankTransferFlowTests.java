package com.octo;

import com.google.common.collect.ImmutableList;
import com.octo.flows.CreateBankStateFlow;
import com.octo.flows.TransferMoneyFlow;
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

public class BankTransferFlowTests {

    @Rule
    public final ExpectedException exception = ExpectedException.none();
    private final MockNetwork network = new MockNetwork(new MockNetworkParameters(ImmutableList.of(
            TestCordapp.findCordapp("com.octo.contracts"),
            TestCordapp.findCordapp("com.octo.flows")
    )));
    private final StartedMockNode a = network.createNode();
    private final StartedMockNode b = network.createNode();
    private final StartedMockNode bc = network.createNode();

    public BankTransferFlowTests() {
        for (StartedMockNode node : ImmutableList.of(a, b)) {
            node.registerInitiatedFlow(TransferMoneyFlow.Responder.class);
        }
    }

    @Before
    public void setup() throws Exception {
        CreateBankStateFlow flowA = new CreateBankStateFlow(a.getInfo().getLegalIdentities().get(0), 1000);
        CreateBankStateFlow flowB = new CreateBankStateFlow(b.getInfo().getLegalIdentities().get(0), 0);
        CordaFuture<SignedTransaction> futureA = bc.startFlow(flowA);
        CordaFuture<SignedTransaction> futureB = bc.startFlow(flowB);
        network.runNetwork();
        futureA.get();
        futureB.get();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void flowRejectsNegativeAmountTransfer() throws Exception {
        TransferMoneyFlow.Initiator flow = new TransferMoneyFlow.Initiator(b.getInfo().getLegalIdentities().get(0), -500);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        exception.expectCause(instanceOf(TransactionVerificationException.class));
        future.get();
    }

    @Test
    public void flowRejectsCreatingBankBalanceIfExists() throws Exception {

    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        TransferMoneyFlow.Initiator flow = new TransferMoneyFlow.Initiator(b.getInfo().getLegalIdentities().get(0), 500);
        CordaFuture<SignedTransaction> future = a.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();
        for (StartedMockNode node : ImmutableList.of(a, b, bc)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

}
