package com.octo.flows;


import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.StatesToRecord;
import net.corda.core.transactions.SignedTransaction;


public class SendToCentralBankFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Send extends FlowLogic<Void> {
        private final Party centralBank;
        private final SignedTransaction finalityTx;

        public Send(Party centralBank, SignedTransaction finalityTx) {
            this.centralBank = centralBank;
            this.finalityTx = finalityTx;
        }

        @Override
        @Suspendable
        public Void call() throws FlowException {
            FlowSession session = initiateFlow(centralBank);
            return subFlow(new SendTransactionFlow(session, finalityTx));
        }
    }

    @InitiatedBy(Send.class)
    public static class Receive extends FlowLogic<SignedTransaction> {

        private final FlowSession otherSession;

        public Receive(FlowSession otherSession) {
            this.otherSession = otherSession;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            return subFlow(new ReceiveTransactionFlow(otherSession, false, StatesToRecord.ALL_VISIBLE));
        }
    }

}
