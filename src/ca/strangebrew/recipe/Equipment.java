package ca.strangebrew.recipe;

/**
 * Hold an equipment profile as per BeerXML specs.
 * Created by doug on 01/02/15.
 */
public class Equipment {
    private String name = "New Profile";
    private double boilSize;
    private double batchSize;
    private double tunVolume;
    private double tunWeight;
    private double tunSpecificHeat;
    private double topupWater;
    private double trubChillerLoss;
    private double evapRate;
    private double boilTime;
    private boolean calcBoilVol;
    private double lauterDeadspace;
    private double topupKettle;
    private double hopUtilization;
    private String notes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBoilSize() {
        return boilSize;
    }

    public void setBoilSize(double boilSize) {
        this.boilSize = boilSize;
    }

    public double getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(double batchSize) {
        this.batchSize = batchSize;
    }

    public double getTunVolume() {
        return tunVolume;
    }

    public void setTunVolume(double tunVolume) {
        this.tunVolume = tunVolume;
    }

    public double getTunWeight() {
        return tunWeight;
    }

    public void setTunWeight(double tunWeight) {
        this.tunWeight = tunWeight;
    }

    public double getTunSpecificHeat() {
        return tunSpecificHeat;
    }

    public void setTunSpecificHeat(double tunSpecificHeat) {
        this.tunSpecificHeat = tunSpecificHeat;
    }

    public double getTopupWater() {
        return topupWater;
    }

    public void setTopupWater(double topupWater) {
        this.topupWater = topupWater;
    }

    public double getTrubChillerLoss() {
        return trubChillerLoss;
    }

    public void setTrubChillerLoss(double trubChillerLoss) {
        this.trubChillerLoss = trubChillerLoss;
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

    public double getLauterDeadspace() {
        return lauterDeadspace;
    }

    public void setLauterDeadspace(double lauterDeadspace) {
        this.lauterDeadspace = lauterDeadspace;
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
}
