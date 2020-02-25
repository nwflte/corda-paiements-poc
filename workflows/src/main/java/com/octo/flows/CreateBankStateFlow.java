package com.octo.flows;

import com.octo.contracts.BankBalanceContract;
import com.octo.states.BankBalanceState;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Currency;

// ******************
// * Initiator flow *
// ******************
@InitiatingFlow
@StartableByRPC
public class CreateBankStateFlow extends FlowLogic<SignedTransaction> {
    private final Party bank;
    private final Amount<Currency> amount;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker
            .Step("Generating transaction based on new BankBalanceState.");
    private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker
            .Step("Signing transaction with our private key.");

    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker
            .Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };


    private final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            SIGNING_TRANSACTION,
            FINALISING_TRANSACTION);

    public CreateBankStateFlow(Party bank, Amount<Currency> amount) {
        this.bank = bank;
        this.amount = amount;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Override
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        BankBalanceState bankBalanceState = new BankBalanceState(bank, amount);

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        TransactionBuilder builder = new TransactionBuilder(notary)
                .addOutputState(bankBalanceState, BankBalanceContract.ID)
                .addCommand(new BankBalanceContract.BankBalanceCommands.CentralBankCreates(), getOurIdentity().getOwningKey());

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new VerifySignAndFinaliseFlow(builder));
    }
}
