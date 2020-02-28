package com.octo.contracts;

import com.octo.states.BankBalanceState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ************
// * Contract *
// ************
public class BankBalanceContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.octo.contracts.BankBalanceContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        verifyAll(tx);
    }

    private void verifyAll(LedgerTransaction tx) {
        /*final BankBalanceCommands command =  tx.findCommand(BankBalanceCommands.class, cmd -> true).getValue();
        if(command instanceof BankBalanceCommands.CentralBankCreates)
            verifyCentralBankCreates(tx, command);
        if(command instanceof BankBalanceCommands.CentralBankApproval)
            verifyCentralBankApproval(tx, command);*/
    }

    private void verifyCentralBankApproval(LedgerTransaction tx, BankBalanceCommands command) {

    }
    private void verifyCentralBankCreates(LedgerTransaction tx, BankBalanceCommands command) {
        requireThat(require -> {
            require.using("A bank balance creation should not consume any input states", tx.getInputs().isEmpty());
            require.using("A bank balance creation should only create one output state", tx.getOutputs().size() == 1);
         return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface BankBalanceCommands extends CommandData {
        class CentralBankCreates implements BankBalanceCommands {}
        class CentralBankApproval implements BankBalanceCommands {}
    }
}