package com.octo.contracts;

import com.octo.states.BankBalanceState;
import com.octo.states.BankTransferState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireThat;

public class BankTransferContract implements Contract {

    public static final String ID = "com.octo.contracts.BankTransferContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        verifyAll(tx);
    }

    private void verifyAll(LedgerTransaction tx) {

        BankTransferState transferOutputState = tx.outputsOfType(BankTransferState.class).get(0);
        List<BankBalanceState> listOutputBalanceStates = tx.outputsOfType(BankBalanceState.class);
        List<BankBalanceState> listInputBalanceStates = tx.inputsOfType(BankBalanceState.class);

        BankBalanceState senderOutputState = getBalanceStateByBank(listOutputBalanceStates, transferOutputState.getSender());
        BankBalanceState receiverOutputState = getBalanceStateByBank(listOutputBalanceStates, transferOutputState.getReceiver());
        BankBalanceState senderInputState = getBalanceStateByBank(listInputBalanceStates, transferOutputState.getSender());
        BankBalanceState receiverInputState = getBalanceStateByBank(listInputBalanceStates, transferOutputState.getReceiver());

        requireThat(require -> {
            require.using("The amount must not be negative", transferOutputState.getAmount().intValue() > 0);
            require.using("Sender cannot be the receiver", !transferOutputState.getReceiver().equals(transferOutputState.getSender()));
            require.using("Sender account must be debited by amount", senderOutputState.getAmount()
                    .compareTo(senderInputState.getAmount() - transferOutputState.getAmount()) == 0);
            require.using("Receiver account must be credited by amount", receiverOutputState.getAmount()
                    .compareTo(receiverInputState.getAmount() + transferOutputState.getAmount()) == 0);
            return null;
        });

        /*final BankTransferCommands command = tx.findCommand(BankTransferCommands.class, cmd -> true).getValue();

        if(command instanceof BankTransferCommands.CentralBankApproval)
            verifyCentralBankApproval(tx, command);
        if(command instanceof BankTransferCommands.TransferMoney)
            verifyTransfer(tx, command);*/
    }

    private void verifyCentralBankApproval(LedgerTransaction tx, BankTransferCommands command) {

    }

    private void verifyTransfer(LedgerTransaction tx, BankTransferCommands command) {
        /*requireThat(require -> {
            require.using("A transfer should have 2 output states",
                    tx.getOutputs().size() == 2);

            require.using("A transfer should consume 1 input state",
                    tx.getInputs().size() == 1);

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
                    outs.stream().allMatch(out -> out.getAmount().intValue() >= 0)
            );

            return null;
        });*/
    }

    private BankBalanceState getBalanceStateByBank(List<BankBalanceState> list, Party bank) {
        return list.stream().filter(st -> st.getBank().equals(bank)).findFirst().orElseThrow(() -> new IllegalStateException(""));
    }

    public interface BankTransferCommands extends CommandData {
        class TransferMoney implements BankTransferCommands {
        }

        class CentralBankApproval implements BankTransferCommands {
        }
    }
}
