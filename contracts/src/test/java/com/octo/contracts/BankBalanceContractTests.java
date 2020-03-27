package com.octo.contracts;

import com.google.common.collect.ImmutableList;
import com.octo.states.BankBalanceState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static net.corda.testing.node.NodeTestUtils.ledger;

public class BankBalanceContractTests {
    private final MockServices ledgerServices = new MockServices();

    static private TestIdentity bankA = new TestIdentity(new CordaX500Name("BankA", "London", "GB"));
    static private TestIdentity centralBank = new TestIdentity(new CordaX500Name("BankB", "London", "GB"));

    @Test
    public void createBankBalanceShouldHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(BankBalanceContract.ID, new BankBalanceState(bankA.getParty(), centralBank.getParty(), 2000));
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()), new BankBalanceContract.BankBalanceCommands.CentralBankCreates());
                tx.failsWith("A bank balance creation should not consume any input states");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void createBankBalanceShouldHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(BankBalanceContract.ID, new BankBalanceState(bankA.getParty(), centralBank.getParty(), 2000));
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()), new BankBalanceContract.BankBalanceCommands.CentralBankCreates());
                tx.failsWith("A bank balance creation should only create one output state");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void createBankBalanceShouldHaveDifferentIssuerAndBank() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(BankBalanceContract.ID, new BankBalanceState(bankA.getParty(), bankA.getParty(), 2000));
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()), new BankBalanceContract.BankBalanceCommands.CentralBankCreates());
                tx.failsWith("The issuer is same as bank");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void bankBalanceSoldeShouldBePositive() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(BankBalanceContract.ID, new BankBalanceState(bankA.getParty(), bankA.getParty(), -10));
                tx.command(ImmutableList.of(bankA.getPublicKey(), centralBank.getPublicKey()), new BankBalanceContract.BankBalanceCommands.CentralBankCreates());
                tx.failsWith("The balance must not be negative");
                return null;
            });
            return null;
        }));
    }


}