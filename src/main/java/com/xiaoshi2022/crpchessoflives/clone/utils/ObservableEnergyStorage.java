package com.xiaoshi2022.crpchessoflives.clone.utils;

import net.neoforged.neoforge.energy.EnergyStorage;

public class ObservableEnergyStorage extends EnergyStorage {
    public ObservableEnergyStorage(final int capacity) {
        super(capacity);
    }

    public ObservableEnergyStorage(final int capacity, final int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public ObservableEnergyStorage(final int capacity, final int maxReceive, final int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public ObservableEnergyStorage(final int capacity, final int maxReceive, final int maxExtract, final int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        if (!simulate) {
            this.onEnergyChanged();
        }
        return super.receiveEnergy(toReceive, simulate);
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        if (!simulate) {
            this.onEnergyChanged();
        }
        return super.extractEnergy(toExtract, simulate);
    }

    public void onEnergyChanged() {
    }
}
