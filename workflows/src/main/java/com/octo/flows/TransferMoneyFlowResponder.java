package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

/* @InitiatedBy(TransferMoneyFlow.class)
public class TransferMoneyFlowResponder extends FlowLogic<SignedTransaction> {
    private final FlowSession counterPartySession;
    private final Integer amount;
    private final Party notary;

    public TransferMoneyFlowResponder(FlowSession counterPartySession, Integer amount, Party notary) {
        this.counterPartySession = counterPartySession;
        this.amount = amount;
        this.notary = notary;
    }

    private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }



    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        class SignTxFlow extends SignTransactionFlow{

            public SignTxFlow(@NotNull FlowSession otherSideSession, @NotNull ProgressTracker progressTracker) {
                super(otherSideSession, progressTracker);
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {

            }
        }

        return subFlow(new SignTxFlow(counterPartySession, SignTransactionFlow.tracker()));
    }
}*/
