package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.octo.contracts.BankBalanceContract;
import com.octo.states.BankBalanceState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Currency;
import java.util.List;

public class TransferMoneyFlow extends FlowLogic<SignedTransaction> {
    private final Party otherBank;
    // private final UniqueIdentifier linearId;
    private final Amount<Currency> amount;
    // private final Amount<Currency> amount;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public TransferMoneyFlow(Party otherBank, Amount<Currency> amount) {
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
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(otherBank, getOurIdentity()));
        List<StateAndRef<BankBalanceState>> inputStateAndRefs = getServiceHub().getVaultService().queryBy(BankBalanceState.class,
                queryCriteria).getStates();

        StateAndRef<BankBalanceState> thisBankInputStateAndRef = inputStateAndRefs.stream()
                .filter(inputStateAndRef -> inputStateAndRef.getState().getData().getBank().equals(getOurIdentity()))
                .findAny().orElseThrow(() -> new IllegalStateException("No balance state corresponding to current bank"));

        BankBalanceState thisBankInputState = thisBankInputStateAndRef.getState().getData();

        StateAndRef<BankBalanceState> otherBankInputStateAndRef = inputStateAndRefs.stream()
                .filter(inputStateAndRef -> inputStateAndRef.getState().getData().getBank().equals(otherBank))
                .findAny().orElseThrow(() -> new IllegalStateException("No balance state corresponding to other bank"));

        BankBalanceState otherBankInputState = otherBankInputStateAndRef.getState().getData();

        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        TransactionBuilder builder = new TransactionBuilder(notary)
                .addInputState(thisBankInputStateAndRef)
                .addInputState(otherBankInputStateAndRef)
                .addOutputState(thisBankInputState.decrease(amount), BankBalanceContract.ID)
                .addOutputState(otherBankInputState.increase(amount), BankBalanceContract.ID)
                .addCommand(new BankBalanceContract.BankBalanceCommands.TransferMoney(),
                        ImmutableList.of(getOurIdentity().getOwningKey()));

        return subFlow(new VerifySignAndFinaliseFlow(builder));
    }
}
