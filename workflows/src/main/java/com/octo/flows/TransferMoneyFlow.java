package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.octo.contracts.BankBalanceContract;
import com.octo.contracts.BankTransferContract;
import com.octo.states.BankBalanceState;
import com.octo.states.BankTransferState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TransferMoneyFlow extends FlowLogic<SignedTransaction> {
    private final Party otherBank;
    private final Integer amount;
    // private final Amount<Currency> amount;

    private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker
            .Step("Generating transaction.");
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

    public TransferMoneyFlow(Party otherBank, Integer amount) {
        this.otherBank = otherBank;
        this.amount = amount;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }


    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        progressTracker.setCurrentStep(GENERATING_TRANSACTION);
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(getOurIdentity()),
                null, null, Vault.StateStatus.UNCONSUMED);
        List<StateAndRef<BankBalanceState>> inputStateAndRefs = getServiceHub().getVaultService()
                .queryBy(BankBalanceState.class, queryCriteria).getStates();

        if(inputStateAndRefs.isEmpty())
            throw new IllegalStateException("No balance state corresponding to current bank");
        StateAndRef<BankBalanceState> thisBankInputStateAndRef = inputStateAndRefs.get(0);

        BankBalanceState inputBankInputState = thisBankInputStateAndRef.getState().getData();

        BankTransferState outputBankTransferState = new BankTransferState(getOurIdentity(), otherBank, amount);

        progressTracker.setCurrentStep(SIGNING_TRANSACTION);
        TransactionBuilder builder = new TransactionBuilder(notary)
                .addInputState(thisBankInputStateAndRef)
                .addOutputState(inputBankInputState.decrease(amount), BankBalanceContract.ID)
                .addOutputState(outputBankTransferState, BankTransferContract.ID)
                .addCommand(new BankTransferContract.BankTransferCommands.TransferMoney(),
                        ImmutableList.of(getOurIdentity().getOwningKey()));

        progressTracker.setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new VerifySignAndFinaliseFlow(builder));
    }
}
