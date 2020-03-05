package com.octo.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.octo.contracts.BankBalanceContract;
import com.octo.contracts.BankTransferContract;
import com.octo.states.BankBalanceState;
import com.octo.states.BankTransferState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.security.SignatureException;
import java.util.List;

public class TransferMoneyFlow{

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {
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

        public Initiator(Party otherBank, Integer amount) {
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
            // Get Notary
            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Query to Get Balance State
            QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(getOurIdentity()),
                    null, null, Vault.StateStatus.UNCONSUMED);
            List<StateAndRef<BankBalanceState>> inputStateAndRefs = getServiceHub().getVaultService()
                    .queryBy(BankBalanceState.class, queryCriteria).getStates();

            if (inputStateAndRefs.isEmpty())
                throw new IllegalStateException("No balance state corresponding to sending bank");
            StateAndRef<BankBalanceState> thisBankInputStateAndRef = inputStateAndRefs.get(0);

            BankBalanceState inputBankBalanceState = thisBankInputStateAndRef.getState().getData();

            // Initiate flow with other parties
            final FlowSession otherBankSession = initiateFlow(otherBank);
            final FlowSession centralBankSession = initiateFlow(inputBankBalanceState.getIssuer());

            // Send signing flag to parties
            otherBankSession.send(true);
            centralBankSession.send(false);

            // Get the other bank to create the output BankBlanaceState (Increase it's balance by amount)
            final BankBalanceState otherBankBalanceState = otherBankSession.
                    sendAndReceive(BankBalanceState.class, amount).unwrap(wrappedBankBalance -> wrappedBankBalance);

            final StateAndRef<BankBalanceState> otherBankInputStateAndRef = otherBankSession.
                    receive(StateAndRef.class).unwrap(wrapped -> wrapped);

            // Create Signers List
            final List<PublicKey> requiredSigners = ImmutableList.of(getOurIdentity().getOwningKey(), otherBank.getOwningKey());

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            TransactionBuilder builder = new TransactionBuilder(notary)
                    .addInputState(thisBankInputStateAndRef)
                    .addInputState(otherBankInputStateAndRef)
                    .addOutputState(inputBankBalanceState.decrease(amount), BankBalanceContract.ID)
                    .addOutputState(otherBankBalanceState, BankBalanceContract.ID)
                    .addOutputState(new BankTransferState(getOurIdentity(), otherBank, amount), BankTransferContract.ID)
                    .addCommand(new BankTransferContract.BankTransferCommands.TransferMoney(), requiredSigners);


            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            // builder.verify(getServiceHub());
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(builder);
            otherBankSession.send(partSignedTx);

            SignedTransaction returnedTx = subFlow(new ReceiveTransactionFlow(otherBankSession, false));

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(returnedTx,
                    ImmutableList.of(otherBankSession), CollectSignaturesFlow.Companion.tracker()));

            SignedTransaction finalityTx = subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(otherBankSession, centralBankSession)));
            return finalityTx;
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession counterPartySession;

        public Responder(FlowSession counterPartySession) {
            this.counterPartySession = counterPartySession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            final boolean needsToSignTransaction = counterPartySession.receive(Boolean.class).unwrap(bool -> bool);

            if (needsToSignTransaction) {
                // Receive amount from sender bank
                final Integer amount = counterPartySession.receive(Integer.class).unwrap(wrappedInt -> wrappedInt);

                Party otherParty = counterPartySession.getCounterparty();

                QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(ImmutableList.of(getOurIdentity()),
                        null, null, Vault.StateStatus.UNCONSUMED);
                List<StateAndRef<BankBalanceState>> inputStateAndRefs = getServiceHub().getVaultService()
                        .queryBy(BankBalanceState.class, queryCriteria).getStates();

                if (inputStateAndRefs.isEmpty())
                    throw new IllegalStateException("No balance state corresponding to receiving bank");
                StateAndRef<BankBalanceState> thisBankInputStateAndRef = inputStateAndRefs.get(0);

                BankBalanceState outputBankBalanceState = thisBankInputStateAndRef.getState().getData().increase(amount);

                counterPartySession.send(outputBankBalanceState);

                counterPartySession.send(thisBankInputStateAndRef);

                SignedTransaction signedTx = counterPartySession.receive(SignedTransaction.class).unwrap(wrappedTx -> wrappedTx);
                subFlow(new SendTransactionFlow(counterPartySession, signedTx));

                final SecureHash txId = subFlow(new CheckTransactionAndSignFlow(outputBankBalanceState, thisBankInputStateAndRef, counterPartySession,
                        SignTransactionFlow.Companion.tracker())).getId();
                getLogger().debug("This is bank B");
                return subFlow(new ReceiveFinalityFlow(counterPartySession, txId));
            }
            getLogger().debug("This is central bank");
            return subFlow(new ReceiveFinalityFlow(counterPartySession));
        }


        public static class CheckTransactionAndSignFlow extends SignTransactionFlow {
            private final BankBalanceState expectedBankBalanceState;
            private final StateAndRef<BankBalanceState> thisBankInputStateAndRef;

            CheckTransactionAndSignFlow(BankBalanceState expectedBankBalanceState, StateAndRef<BankBalanceState> thisBankInputStateAndRef, FlowSession otherPartyFlow,
                                        ProgressTracker progressTracker) {
                super(otherPartyFlow, progressTracker);
                this.expectedBankBalanceState = expectedBankBalanceState;
                this.thisBankInputStateAndRef = thisBankInputStateAndRef;
            }

            @Override
            protected void checkTransaction(@NotNull SignedTransaction stx) throws FlowException {
                try {
                    LedgerTransaction ltx = stx.toLedgerTransaction(getServiceHub(), false);
                } catch (SignatureException e) {
                    throw new FlowException("Transaction had invalid signature.");
                }

            }

        }
    }

}
