package com.octo.contracts;

import com.octo.states.BankBalanceState;
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
    public static final String ID = "com.octo.contracts.TemplateContract";

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {
        verifyAll(tx);
    }

    private void verifyAll(LedgerTransaction tx) {
        CommandWithParties<BankBalanceCommands> command =  requireSingleCommand(tx.getCommands(), BankBalanceCommands.class);
        BankBalanceCommands commandType = command.getValue();

        if(commandType instanceof BankBalanceCommands.TransferMoney)
            verifyTransfer(tx, command);
        if(commandType instanceof BankBalanceCommands.CentralBankApproval) verifyCentralBankApproval(tx, command);
    }

    private void verifyCentralBankApproval(LedgerTransaction tx, CommandWithParties<BankBalanceCommands> command) {

    }
    private void verifyCentralBankCreates(LedgerTransaction tx, CommandWithParties<BankBalanceCommands> command) {
        requireThat(require -> {
            require.using("A bank balance creation should not consume any input states", tx.getInputs().isEmpty());
            require.using("A bank balance creation should only create one output state", tx.getOutputs().size() == 1);

         return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, CommandWithParties<BankBalanceCommands> command) {
        requireThat(require -> {
                require.using("A transfer should have 2 output states",
                        tx.getOutputs().size() == 2);

                require.using("A transfer should consume 2 input state",
                        tx.getInputs().size() == 2);

                final List<BankBalanceState> ins = tx.inputsOfType(BankBalanceState.class);
                final List<BankBalanceState> outs = tx.outputsOfType(BankBalanceState.class);

                require.using("The balance should change after a transfer",
                        outs.stream().allMatch(out -> {
                                    BankBalanceState inputState = ins.stream().filter(in -> in.getBank().equals(out.getBank())).findAny()
                                            .orElseThrow(() -> new IllegalStateException("No corresponding state"));
                                    return out.getAmount().compareTo(inputState.getAmount()) != 0;
                                }
                        )
                );

                require.using("The balance should stay positive after decrease",
                        outs.stream().allMatch(out -> out.getAmount().getQuantity() >= 0)
                        );

                return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface BankBalanceCommands extends CommandData {
        class TransferMoney implements BankBalanceCommands {}
        class CentralBankCreates implements BankBalanceCommands {}
        class CentralBankApproval implements BankBalanceCommands {}
    }
}