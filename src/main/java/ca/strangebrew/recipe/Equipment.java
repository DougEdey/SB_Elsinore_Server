package ca.strangebrew.recipe;

/**
 * Hold an equipment profile as per BeerXML specs.
 * Created by doug on 01/02/15.
 */
public class Equipment {
    private String name = "New Profile";
    private Quantity boilSize = new Quantity();
    private Quantity batchSize = new Quantity();
    private Quantity tunVolume = new Quantity();
    private Quantity tunWeight = new Quantity();
    private double tunSpecificHeat;
    private Quantity topupWater = new Quantity();
    private Quantity trubChillerLoss = new Quantity();
    private double evapRate;
    private double boilTime;
    private boolean calcBoilVol;
    private Quantity lauterDeadspace = new Quantity();
    private double topupKettle;
    private double hopUtilization;
    private String notes;
    private double chillPercent;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Quantity getBoilSize() {
        return boilSize;
    }

    public void setBoilSize(double boilSize) {
        this.boilSize.setUnits(Quantity.L);
        this.boilSize.setAmount(boilSize);
    }

    public Quantity getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(double batchSize) {
        this.batchSize.setUnits(Quantity.L);
        this.batchSize.setAmount(batchSize);
    }

    public Quantity getTunVolume() {
        return tunVolume;
    }

    public void setTunVolume(double tunVolume) {
        this.tunVolume.setUnits(Quantity.L);
        this.tunVolume.setAmount(tunVolume);
    }

    public Quantity getTunWeight() {
        return tunWeight;
    }

    public void setTunWeight(double tunWeight) {
        this.tunWeight.setUnits(Quantity.KG);
        this.tunWeight.setAmount(tunWeight);
    }

    public double getTunSpecificHeat() {
        return tunSpecificHeat;
    }

    public void setTunSpecificHeat(double tunSpecificHeat) {
        this.tunSpecificHeat = tunSpecificHeat;
    }

    public Quantity getTopupWater() {
        return topupWater;
    }

    public void setTopupWater(double topupWater) {
        this.topupWater.setUnits(Quantity.L);
        this.topupWater.setAmount(topupWater);
    }

    public Quantity getTrubChillerLoss() {
        return trubChillerLoss;
    }

    public void setTrubChillerLoss(double trubChillerLoss) {
        this.trubChillerLoss.setUnits(Quantity.L);
        this.trubChillerLoss.setAmount(trubChillerLoss);
    }

    public double getEvapRate() {
        return evapRate;
    }

    public void setEvapRate(double evapRate) {
        this.evapRate = evapRate;
    }

    public double getBoilTime() {
        return boilTime;
    }

    public void setBoilTime(double boilTime) {
        this.boilTime = boilTime;
    }

    public boolean isCalcBoilVol() {
        return calcBoilVol;
    }

    public void setCalcBoilVol(boolean calcBoilVol) {
        this.calcBoilVol = calcBoilVol;
    }

    public Quantity getLauterDeadspace() {
        return lauterDeadspace;
    }

    public void setLauterDeadspace(double lauterDeadspace) {
        this.lauterDeadspace.setUnits(Quantity.L);
        this.lauterDeadspace.setAmount(lauterDeadspace);
    }

    public double getTopupKettle() {
        return topupKettle;
    }

    public void setTopupKettle(double topupKettle) {
        this.topupKettle = topupKettle;
    }

    public double getHopUtilization() {
        return hopUtilization;
    }

    public void setHopUtilization(double hopUtilization) {
        this.hopUtilization = hopUtilization;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setChillPercent(double chillPercent) {
        this.chillPercent = chillPercent;
    }

    public double getChillPercent() {
        return chillPercent;
    }
}
