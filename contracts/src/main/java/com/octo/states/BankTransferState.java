package com.octo.states;

import com.google.common.collect.ImmutableList;
import com.octo.contracts.BankTransferContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@BelongsToContract(BankTransferContract.class)
public class BankTransferState implements ContractState {

    private final Party sender;
    private final Party receiver;
    private final Integer amount;

    public BankTransferState(Party sender, Party receiver, Integer amount) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(sender, receiver);
    }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    public Integer getAmount() {
        return amount;
    }
}
