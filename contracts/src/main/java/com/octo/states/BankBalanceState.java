package com.octo.states;

import com.octo.contracts.BankBalanceContract;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

// *********
// * State *
// *********
@BelongsToContract(BankBalanceContract.class)
public class BankBalanceState implements LinearState {

    private final Party bank;
    private final Integer amount;
    private final UniqueIdentifier linearId;

    public BankBalanceState(Party bank, Integer amount) {
        this.bank = bank;
        this.amount = amount;
        this.linearId = new UniqueIdentifier();
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(bank);
    }

    public BankBalanceState increase(Integer value){
        return new BankBalanceState(bank, amount + value);
    }

    public BankBalanceState decrease(Integer value){
        return new BankBalanceState(bank, amount - value);
    }

    public Party getBank() {
        return bank;
    }

    public Integer getAmount() {
        return amount;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}