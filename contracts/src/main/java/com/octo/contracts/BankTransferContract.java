package com.octo.contracts;

import com.octo.states.BankTransferState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BankTransferContract implements Contract {

    public static final String ID = "com.octo.contracts.BankTransferContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        List<BankTransferState> listOutputStates = tx.outputsOfType(BankTransferState.class);
        List<BankTransferState> listInputStates = tx.inputsOfType(BankTransferState.class);
        requireThat(require -> {
            require.using("The amount must not be negative", Stream.concat(listInputStates.stream(), listOutputStates.stream())
                    .allMatch(st -> st.getAmount() >= 0));
            require.using("Sender and receiver of transfer should be different", Stream.concat(listInputStates.stream(), listOutputStates.stream())
                    .allMatch(st -> !st.getSender().equals(st.getReceiver())));
            return null;
        });

        if (tx.getCommands().size() != 1)
            throw new IllegalStateException("There must be one command in transactions involving BankTransferState");

        if (tx.getCommand(0).getValue() instanceof BankTransferCommands.TransferMoney)
            verifyTransfer(tx);

    }

    private void verifyCentralBankApproval(LedgerTransaction tx) {

    }

    private void verifyTransfer(LedgerTransaction tx) {
        BankTransferState transferOutputState = tx.outputsOfType(BankTransferState.class).get(0);
        Integer amount = transferOutputState.getAmount();

        requireThat(require -> {
            require.using("A bank transfer transaction should have one output state of type BankTransferState",
                    tx.outputsOfType(BankTransferState.class).size() == 1);
            require.using("The amount must not be negative", amount > 0);
            return null;
        });
    }

    public interface BankTransferCommands extends CommandData {
        class TransferMoney implements BankTransferCommands {
        }
        class CentralBankApproval implements BankTransferCommands {
        }
    }
}
