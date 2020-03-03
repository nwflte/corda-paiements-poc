package com.octo;


import com.octo.contracts.BankTransferContract;
import com.octo.flows.CreateBankStateFlow;
import com.octo.flows.TransferMoneyFlow;
import com.octo.states.BankBalanceState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueAndPaymentFlow;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.node.User;
import org.apache.shiro.crypto.hash.Hash;
import org.junit.Test;
import rx.Observable;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.corda.node.services.Permissions.invokeRpc;
import static net.corda.testing.core.ExpectKt.expect;
import static net.corda.testing.core.ExpectKt.expectEvents;
import static net.corda.testing.driver.Driver.driver;
import static net.corda.testing.node.internal.InternalTestUtilsKt.FINANCE_CORDAPPS;
import static net.corda.node.services.Permissions.startFlow;
import static org.junit.Assert.assertEquals;


public class IntegrationTests {

    @Test
    public void twoBanksTransfer(){
        // START 1
        driver(new DriverParameters()
                .withIsDebug(true)
                .withExtraCordappPackagesToScan(asList("com.corda.contracts"))
        .withStartNodesInProcess(true)
                ,dsl -> {

            User centralBankUser = new User("user1", "password", new HashSet<>(asList(
                    startFlow(CreateBankStateFlow.class), invokeRpc("vaultTrack")
            )));


            User bankAUser = new User("user1", "password", new HashSet<>(asList(
                    startFlow(TransferMoneyFlow.Initiator.class), invokeRpc("vaultTrack")
            )));

            User bankBUser = new User("user1", "password", new HashSet<>(asList(
                    startFlow(TransferMoneyFlow.Responder.class), invokeRpc("vaultTrack")
            )));

            try {
                List<CordaFuture<NodeHandle>> nodeHandleFutures = asList(
                    dsl.startNode(new NodeParameters().
                            withProvidedName(new CordaX500Name("BankA", "London", "GB")).withRpcUsers(singletonList(bankAUser))),
                        dsl.startNode(new NodeParameters().
                                withProvidedName(new CordaX500Name("BankB", "New York", "US")).withRpcUsers(singletonList(bankBUser))),
                        dsl.startNode(new NodeParameters().
                                withProvidedName(new CordaX500Name("CentralBank", "New York", "US")).withRpcUsers(singletonList(centralBankUser)))
                );

                NodeHandle bankA = nodeHandleFutures.get(0).get();
                NodeHandle bankB = nodeHandleFutures.get(1).get();
                NodeHandle centralBank = nodeHandleFutures.get(2).get();

                // END 1

                // START 2
                CordaRPCClient bankAClient = new CordaRPCClient(bankA.getRpcAddress());
                CordaRPCOps bankAProxy = bankAClient.start("user1", "password").getProxy();

                CordaRPCClient bankBClient = new CordaRPCClient(bankB.getRpcAddress());
                CordaRPCOps bankBProxy = bankBClient.start("user1", "password").getProxy();

                CordaRPCClient centralBankClient = new CordaRPCClient(centralBank.getRpcAddress());
                CordaRPCOps centralBankProxy = centralBankClient.start("user1", "password").getProxy();

                // END 2

                // START 3
                Observable<Vault.Update<BankBalanceState>> bankAVaultUpdates = bankAProxy.vaultTrack(BankBalanceState.class)
                        .getUpdates();
                Observable<Vault.Update<BankBalanceState>> bankBVaultUpdates = bankBProxy.vaultTrack(BankBalanceState.class)
                        .getUpdates();

                // END 3
                // OpaqueBytes issueRef = OpaqueBytes.of((byte)0);

                // START 4
                centralBankProxy.startFlowDynamic(
                        CreateBankStateFlow.class,
                        bankA.getNodeInfo().getLegalIdentities().get(0),
                        10000
                ).getReturnValue().get();
                centralBankProxy.startFlowDynamic(
                        CreateBankStateFlow.class,
                        bankB.getNodeInfo().getLegalIdentities().get(0),
                        10000
                ).getReturnValue().get();

                Class<Vault.Update<BankBalanceState>> bankBalanceUpdateClass = (Class<Vault.Update<BankBalanceState>>)(Class<?>)Vault.Update.class;

                expectEvents(bankAVaultUpdates, true, () ->
                        expect(bankBalanceUpdateClass, update ->  true, update -> {
                            System.out.println("BankA got vault update of " + update);
                            Integer amount = update.getProduced().iterator().next().getState().getData().getAmount();
                            assertEquals((Integer)10000, amount);
                            return null;
                        })
                        );
                // END 4

            } catch (Exception e) {
                throw new RuntimeException("Exception thrown in driver DSL", e);
            }
return null;
        });
    }
}
