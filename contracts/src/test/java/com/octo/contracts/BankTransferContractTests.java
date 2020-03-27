package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.states.BankBalanceState;
import com.octo.states.BankTransferState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class BankTransferContractTests {
    static private TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "London", "GB"));
    static private TestIdentity bankB = new TestIdentity(new CordaX500Name("BankB", "London", "GB"));
    static private TestIdentity centralBank = new TestIdentity(new CordaX500Name("BankB", "London", "GB"));
    private final MockServices ledgerServices = new MockServices();

    @Test
    public void transferAmountMustNotBeNegative() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                BankBalanceState bbsA = new BankBalanceState(bankA.getParty(), centralBank.getParty(), 2000);
                BankBalanceState bbsB = new BankBalanceState(bankB.getParty(), centralBank.getParty(), 1000);

                tx.input(BankBalanceContract.ID, bbsA);
                tx.input(BankBalanceContract.ID, bbsB);
                tx.command(ImmutableList.of(bankA.getPublicKey(), bankB.getPublicKey(), centralBank.getPublicKey()),
                        new BankTransferContract.BankTransferCommands.TransferMoney());
                Integer amountToTransfer = -10;
                tx.output(BankTransferContract.ID, new BankTransferState(bankA.getParty(), bankB.getParty(), amountToTransfer));
                tx.output(BankBalanceContract.ID, bbsA.decrease(amountToTransfer));
                tx.output(BankBalanceContract.ID, bbsB.increase(amountToTransfer));
                tx.failsWith("The amount must not be negative");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void transferSenderAndReceiverShouldBeDifferent() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                BankBalanceState bbsA = new BankBalanceState(bankA.getParty(), centralBank.getParty(), 2000);
                BankBalanceState bbsB = new BankBalanceState(bankB.getParty(), centralBank.getParty(), 1000);

                tx.input(BankBalanceContract.ID, bbsA);
                tx.input(BankBalanceContract.ID, bbsB);
                tx.command(ImmutableList.of(bankA.getPublicKey(), bankB.getPublicKey(), centralBank.getPublicKey()),
                        new BankTransferContract.BankTransferCommands.TransferMoney());
                Integer amountToTransfer = 10;
                tx.output(BankTransferContract.ID, new BankTransferState(bankA.getParty(), bankA.getParty(), amountToTransfer));
                tx.output(BankBalanceContract.ID, bbsA.decrease(amountToTransfer));
                tx.output(BankBalanceContract.ID, bbsB.increase(amountToTransfer));
                tx.failsWith("Sender and receiver of transfer should be different");
                return null;
            });
            return null;
        }));
    }
}
