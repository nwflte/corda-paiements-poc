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
    private final Amount<Currency> amount;
    private final UniqueIdentifier linearId;

    public BankBalanceState(Party bank, Amount<Currency> amount) {
        this.bank = bank;
        this.amount = amount;
        this.linearId = new UniqueIdentifier();
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(bank);
    }

    public BankBalanceState increase(Amount<Currency> value){
        return new BankBalanceState(bank, amount.plus(value));
    }

    public BankBalanceState decrease(Amount<Currency> value){
        return new BankBalanceState(bank, amount.minus(value));
    }

    public Party getBank() {
        return bank;
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }
}