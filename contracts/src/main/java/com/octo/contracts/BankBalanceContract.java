package com.octo.contracts;

import com.octo.states.BankBalanceState;
import com.octo.states.BankTransferState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.util.List;

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
        List<BankBalanceState> listOutputStates = tx.outputsOfType(BankBalanceState.class);
        List<BankBalanceState> listInputStates = tx.inputsOfType(BankBalanceState.class);
        requireThat(require -> {
            require.using("The balance must not be negative", listOutputStates.stream().allMatch(st -> st.getAmount().intValue() >= 0));
            require.using("The balance must not be negative", listInputStates.stream().allMatch(st -> st.getAmount().intValue() >= 0));
            return null;
        });

        if (tx.getCommands().size() != 1)
            throw new IllegalStateException("There must be one command in transactions involving BankBalanceState");

        if (tx.getCommand(0).getValue() instanceof BankBalanceCommands.CentralBankCreates)
            verifyCentralBankCreates(tx);
        else if (tx.getCommand(0).getValue() instanceof BankTransferContract.BankTransferCommands.TransferMoney)
            verifyTransferMoney(tx);

    }

    private void verifyCentralBankCreates(LedgerTransaction tx) {
        requireThat(require -> {
            require.using("A bank balance creation should not consume any input states", tx.getInputs().isEmpty());
            require.using("A bank balance creation should only create one output state", tx.getOutputs().size() == 1);
            BankBalanceState bankBalanceState = (BankBalanceState) tx.getOutput(0);
            require.using("The issuer is same as bank", !bankBalanceState.getBank().equals(bankBalanceState.getIssuer()));
            return null;
        });
    }

    private void verifyTransferMoney(LedgerTransaction tx) {

        BankTransferState transferOutputState = tx.outputsOfType(BankTransferState.class).get(0);
        Integer amount = transferOutputState.getAmount();
        Party sender = transferOutputState.getSender();
        Party receiver = transferOutputState.getReceiver();

        requireThat(require -> {
            require.using("A bank transfer transaction should have two input states of type BankBalanceState",
                    tx.getInputs().size() == 2 && tx.inputsOfType(BankBalanceState.class).size() == 2);
            require.using("A bank transfer transaction should have two output states of type BankBalanceState",
                    tx.getInputs().size() == 2 && tx.inputsOfType(BankBalanceState.class).size() == 2);
            require.using("Sender cannot be the receiver", !sender.equals(receiver));
            require.using("Sender account must be debited by amount", findOutputState(tx, sender).getAmount()
                    .compareTo(findInputState(tx, sender).getAmount() - amount) == 0);
            require.using("Receiver account must be credited by amount", findOutputState(tx, receiver).getAmount()
                    .compareTo(findInputState(tx, receiver).getAmount() + amount) == 0);
            return null;
        });
    }

    private BankBalanceState findOutputState(LedgerTransaction tx, Party bank) {
        return tx.findOutput(BankBalanceState.class, bbs -> bbs.getBank().equals(bank));
    }

    private BankBalanceState findInputState(LedgerTransaction tx, Party bank) {
        return tx.findInput(BankBalanceState.class, bbs -> bbs.getBank().equals(bank));
    }

    // Used to indicate the transaction's intent.
    public interface BankBalanceCommands extends CommandData {
        class CentralBankCreates implements BankBalanceCommands {
        }
    }
}