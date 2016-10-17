package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.BrewServer;
import org.json.simple.JSONObject;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import static com.sb.elsinore.TriggerControl.PID;
import static com.sb.elsinore.UrlEndpoints.*;
import static com.sb.elsinore.triggers.TemperatureTrigger.METHOD;
import static com.sb.elsinore.triggers.TriggerInterface.TYPE;

/**
 * $Id: Mash.java,v 1.37 2008/01/16 17:55:04 jimcdiver Exp $
 *
 * @author aavis
 */
@SuppressWarnings("unused")
public class Mash {
    private Recipe myRecipe;


    //options:
    private double mashRatio;
    private String mashRatioU;
    private String tempUnits = "F";
    private String volUnits = Quantity.GAL;
    private double grainTempF;
    private double boilTempF;

    // private double thermalMass;
    private double tunLossF;
    private Quantity deadSpace = new Quantity();
    private double thinDecoctRatio;
    private double thickDecoctRatio;
    private double cerealMashTemp;
    private String name;

    // calculated:
    private double volQts;
    private int totalTime;
    private double absorbedQTS;
    private double totalWaterQTS;
    private double spargeQTS;

    // steps:
    private ArrayList<MashStep> steps = new ArrayList<>();

    // configurable temps, can be set by the user:
    // target temps are 1/2 between temp + next temp

    private float ACIDTMPF = 85;
    private float GLUCANTMPF = 95;
    private float PROTEINTMPF = 113;
    private float BETATMPF = 131;
    private float ALPHATMPF = 151;
    private float MASHOUTTMPF = 161;
    private float SPARGETMPF = 170;

    static final public String QT_PER_LB = "qt/lb";
    static final public String L_PER_KG = "l/kg";
    static final public String ACID = "acid";
    static final public String GLUCAN = "glucan";
    static final public String PROTEIN = "protein";
    static final public String BETA = "beta";
    static final public String ALPHA = "alpha";
    static final public String MASHOUT = "mashout";
    static final public String SPARGE = "sparge";
    static final public String INFUSION = "infusion";
    static final public String DECOCTION = "decoction";
    static final public String DECOCTION_THICK = "decoction thick";
    static final public String DECOCTION_THIN = "decoction thin";
    static final public String DIRECT = "direct";
    static final public String CEREAL_MASH = "cereal mash";
    static final public String FLY = "fly";
    static final public String BATCH = "batch";

    static final public String[] ratioUnits = {QT_PER_LB, L_PER_KG};
    static final public String[] types = {ACID, GLUCAN, PROTEIN, BETA, ALPHA, MASHOUT, SPARGE};
    static final public String[] methods = {INFUSION, DECOCTION, DECOCTION_THICK, DECOCTION_THIN, DIRECT, CEREAL_MASH};
    static final public String[] spargeMethods = {FLY, BATCH};
    private String notes;
    private double tunTemp;
    private double spargeTemp;
    private double ph;
    private Quantity tunWeight;
    private double tunSpecificHeat;
    private boolean tunAdjust;
    private double totalMashLbs;


    public Mash(String name, Recipe recipe) {
        this.name = name;
        this.myRecipe = recipe;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getNotes() {
        return this.notes;
    }

    public void setTunTemp(double tunTemp) {
        this.tunTemp = tunTemp;
    }

    public double getTunTemp() {
        return this.tunTemp;
    }

    public void setSpargeTemp(double spargeTemp) {
        this.spargeTemp = spargeTemp;
    }

    public double getSpargeTemp() {
        return this.spargeTemp;
    }

    public void setPh(double ph) {
        this.ph = ph;
    }

    public double getPh() {
        return this.ph;
    }

    public void setTunWeight(double tunWeight) {
        this.tunWeight = new Quantity();
        this.tunWeight.setUnits(Quantity.KG);
        this.tunWeight.setAmount(tunWeight);
    }

    public void setTunWeight(String weightString) {
        this.tunWeight = new Quantity(weightString);
    }

    public Quantity getTunWeight() {
        return this.tunWeight;
    }

    public void setTunSpecificHeat(double tunSpecificHeat) {
        this.tunSpecificHeat = tunSpecificHeat;
    }

    public double getTunSpecificHeat() {
        return this.tunSpecificHeat;
    }

    public void setTunAdjust(boolean tunAdjust) {
        this.tunAdjust = tunAdjust;
    }

    public boolean isTunAdjust() {
        return this.tunAdjust;
    }

    public double getMashRatio(int i) {
        return this.steps.get(i).getMashRatio();
    }

    public String getMashRatioU(int i) {
        return this.steps.get(i).getMashRatioU();
    }

    public String getStepInfuseTemp(int i) {
        return this.steps.get(i).getInfuseTemp();
    }

    public String getStepTempU(int i) {
        return this.steps.get(i).getStrikeTempU();
    }

    public String getStepName(int i) {
        return this.steps.get(i).getName();
    }

    public double getStrikeTemp(int i) {
        return this.steps.get(i).getStrikeTemp();
    }

    public String getDisplayStrikeTemp(int i) {
        MashStep s = this.steps.get(i);
        double temp = s.getStrikeTemp();
        if (s.getStrikeTempU().equals("F")) {
            temp = BrewCalcs.cToF(s.getStrikeTemp());
        }

        return temp + " " + s.getStrikeTempU();
    }

    public String getDisplayStepStartTemp(int i) {
        MashStep s = this.steps.get(i);
        double temp = s.getStartTemp();
        if (s.getStrikeTempU().equals("F")) {
            temp = BrewCalcs.cToF(s.getStartTemp());
        }

        return temp + " " + s.getStrikeTempU();
    }

    public double getTotalMashLbs() {
        this.myRecipe.setAllowRecalcs(true);
        this.myRecipe.calcMaltTotals();
        this.myRecipe.setAllowRecalcs(false);
        return this.myRecipe.getTotalMashLbs();
    }

    public class MashStep implements Comparable<MashStep> {
        private String type;
        private double startTemp;
        private double endTemp;
        private String method;
        private int minutes;
        private int rampMin;
        private String directions;
        private double temp;
        private double weightLbs;

        public Quantity inVol = new Quantity();
        public Quantity outVol = new Quantity();
        private double infuseTemp;
        private String infuseTempUnit;
        private double mashRatio;
        private String mashRatioU;
        private String strikeTempU = "C";
        private double strikeTemp;
        private String name;

        // public Quantity decoctVol = new Quantity();

        public MashStep(String type, double startTemp, double endTemp, String method, int min,
                        int rmin) {
            this.type = type;
            this.startTemp = startTemp;
            this.endTemp = endTemp;
            this.method = method;
            this.minutes = min;
            this.rampMin = rmin;

        }

        // default constructor:
        public MashStep() {
            this.rampMin = 0;
            this.endTemp = Mash.this.ALPHATMPF + 1;
            this.startTemp = Mash.this.ALPHATMPF + 1;
            this.minutes = 60;
            this.method = INFUSION;
            this.type = ALPHA;
            this.weightLbs = 0;

        }

        // getter/setter methods

        public String getDirections() {
            return this.directions;
        }

        public void setDirections(String directions) {
            this.directions = directions;
        }

        public double getEndTemp() {
            return this.endTemp;
        }

        public void setEndTemp(double endTemp) {
            this.endTemp = endTemp;
        }

        public Quantity getInVol() {
            return this.inVol;
        }

        public void setInVol(Quantity vol) {
            this.inVol = vol;
        }

        public Quantity getOutVol() {
            return this.outVol;
        }

        public void setOutVol(Quantity vol) {
            this.outVol = vol;
        }

        public String getMethod() {
            return this.method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public int getMinutes() {
            return this.minutes;
        }

        public void setMinutes(int minutes) {
            this.minutes = minutes;
        }

        public int getRampMin() {
            return this.rampMin;
        }

        public void setRampMin(int rampMin) {
            this.rampMin = rampMin;
        }

        public double getStartTemp() {
            return this.startTemp;
        }

        public void setStartTemp(double startTemp) {
            this.startTemp = startTemp;
        }

        public double getTemp() {
            return this.temp;
        }

        public String getType() {
            return this.type;
        }

        public void setType(String s) {
            this.type = s;
        }

        public int compareTo(@Nonnull MashStep m) {
            int result = ((Double) this.getStartTemp()).compareTo(m.getStartTemp());
            return (result == 0 ? -1 : result);
        }

        public void setInVol(double infuseAmount) {
            this.inVol.setUnits(Quantity.LITRES);
            this.inVol.setAmount(infuseAmount);
        }

        public void setInfuseTemp(String infuseTemp) {
            String[] split = infuseTemp.trim().split(" ");
            try {
                this.infuseTemp = Double.parseDouble(split[0].trim().replace(",", ""));
                this.infuseTempUnit = split[1].trim();
            } catch (NumberFormatException nfe) {
                System.out.println("Couldn't parse: " + split[0] + " as a number.");
                nfe.printStackTrace();
            }
        }

        public String getInfuseTemp() {
            return this.infuseTemp + " " + this.getStrikeTempU();
        }

        public void setMashRatio(String mashRatio) {
            try {
                mashRatio = mashRatio.trim();

                this.mashRatio = readDouble(mashRatio);
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        public double readDouble(String s) {
            s = s.trim();
            StringBuilder sb = new StringBuilder(s);
            int i = sb.indexOf(",");
            while (i > 0) {
                // if we're at the decimal point, change it to a .
                if (i == (sb.length() - 2)) {
                    sb.replace(i, i + 1, ",");
                }
                // otherwise remove it
                else {
                    sb.replace(i, i + 1, "");
                }
                i = sb.indexOf(",");
            }

            return Double.parseDouble(sb.toString());
        }

        public double getMashRatio() {
            return this.mashRatio;
        }

        public void setMashRatioU(String mashRatioU) {
            this.mashRatioU = mashRatioU;
        }

        public String getMashRatioU() {
            return this.mashRatioU;
        }

        public void setStrikeTempU(String strikeTempU) {
            this.strikeTempU = strikeTempU;
        }

        public String getStrikeTempU() {
            return this.strikeTempU;
        }

        public void setStrikeTemp(double strikeTemp) {
            this.strikeTemp = strikeTemp;
        }

        public double getStrikeTemp() {
            return this.strikeTemp;
        }

        public double getDisplayStrikeTemp() {
            if (getStrikeTempU().equals("F"))
                return BrewCalcs.cToF(this.strikeTemp);
            return this.strikeTemp;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void convertTo(String newUnit) {
            Mash.this.tempUnits = newUnit.toUpperCase();
            this.strikeTempU = newUnit.toUpperCase();
        }
    }

    public MashStep addStep(String type, double startTemp, double endTemp, String method, int min,
                            int rampmin, double weight) {
        MashStep step = new MashStep(type, startTemp, endTemp, method, min, rampmin);
        step.weightLbs = weight;
        this.steps.add(step);
        calcMashSchedule();
        return step;
    }

    public int addStep() {
        MashStep step = new MashStep();
        // calcStepType(temp);
        if (!this.steps.isEmpty()) {
            MashStep lastStep = this.steps.get(this.steps.size() - 1);
            step.setStartTemp(lastStep.getEndTemp() + 1);
            step.setEndTemp(step.getStartTemp());
            step.setType(calcStepType(step.getStartTemp()));
        }

        this.steps.add(step);
        int i = this.steps.size();
        calcMashSchedule();
        // return the index of the last added step:
        return i - 1;

    }

    public void delStep(int i) {
        if (this.steps.size() > i && !this.steps.isEmpty() && i > -1) {
            this.steps.remove(i);
            calcMashSchedule();
        }

    }

    // set methods:
    // public void setMaltWeight(double mw) {	maltWeightLbs = mw;	}
    public void setMashRatio(double mr) {
        this.mashRatio = mr;
        calcMashSchedule();
    }

    public double getMashRatio() {
        return this.mashRatio;
    }

    public void setMashRatioU(String u) {
        this.mashRatioU = u;
        calcMashSchedule();
    }

    public String getMashRatioU() {
        return this.mashRatioU;
    }

    public void setMashVolUnits(String u) {
        this.volUnits = u;
        calcMashSchedule();
    }

    public void setMashTempUnits(String newUnits) {
        if (newUnits.trim().endsWith("F")) {
            this.tempUnits = "F";
        } else {
            this.tempUnits = "C";
        }
        calcMashSchedule();
    }

    // TODO hardcoded temp strings should be a static somewhere
    public void setGrainTemp(double t) {
        if (this.tempUnits.equals("F"))
            this.grainTempF = t;
        else
            this.grainTempF = BrewCalcs.cToF(t);
        calcMashSchedule();
    }

    public void setBoilTemp(double t) {
        if (this.tempUnits.equals("F"))
            this.boilTempF = t;
        else
            this.boilTempF = BrewCalcs.cToF(t);
        calcMashSchedule();
    }

    public void setTunLoss(double t) {
        if (this.tempUnits.equals("F"))
            this.tunLossF = t;
        else
            this.tunLossF = t * 1.8;
        calcMashSchedule();
    }

    public void setDeadSpace(double d) {
        this.deadSpace.setAmount(d);
    }

    public double getDeadSpace() {
        return this.deadSpace.getValue();
    }

    public void setDecoctRatio(String type, double r) {
        if (type.equals("thick"))
            this.thickDecoctRatio = r;
        else
            this.thinDecoctRatio = r;
        calcMashSchedule();
    }

    public void setTempRange(String type, double t) {
        if (this.tempUnits.equals("C"))
            t = BrewCalcs.cToF(t);
        if (type.equalsIgnoreCase(MASHOUT))
            this.MASHOUTTMPF = (float) t;
        if (type.equalsIgnoreCase(SPARGE))
            this.SPARGETMPF = (float) t;

    }

    public void setName(String s) {
        this.name = s;
    }

    /**
     * @param val Value to convert to a string
     * @return Val converted to the mash vol, formated to 1 decimal
     */
    private String getVolConverted(double val) {
        double d = Quantity.convertUnit(Quantity.QT, this.volUnits, val);
        return SBStringUtils.format(d, 1);
    }

    // get methods:
    public String getMashVolUnits() {
        return this.volUnits;
    }

    public String getMashTempUnits() {
        return this.tempUnits;
    }

    public int getMashTotalTime() {
        return this.totalTime;
    }

    public double getGrainTemp() {
        if (this.tempUnits.equals("F"))
            return this.grainTempF;
        else
            return BrewCalcs.fToC(this.grainTempF);
    }

    public double getBoilTemp() {
        if (this.tempUnits.equals("F"))
            return this.boilTempF;
        else
            return BrewCalcs.fToC(this.boilTempF);
    }

    public double getTempRange(String type) {
        double t = 0;
        if (type.equals(MASHOUT))
            t = this.MASHOUTTMPF;
        else if (type.equals(SPARGE))
            t = this.SPARGETMPF;
        if (this.tempUnits.equals("C"))
            t = BrewCalcs.fToC(t);

        return t;
    }

    public double getSpargeVol() {
        return Quantity.convertUnit(Quantity.QT, this.volUnits, this.spargeQTS);
    }

    public double getSpargeQts() {
        return this.spargeQTS;
    }

    public double getTunLoss() {
        if (this.tempUnits.equals("F"))
            return this.tunLossF;
        else
            return (this.tunLossF / 1.8);
    }

    /**
     * @return A string, which is the total converted to the mash units
     * + the units.
     */
    public String getMashTotalVol() {
        double d = Quantity.convertUnit(Quantity.QT, this.volUnits, this.volQts);
        return SBStringUtils.format(d, 1) + " " + this.volUnits;
    }

    public String getAbsorbedStr() {
        return getVolConverted(this.absorbedQTS);
    }

    public double getAbsorbedQts() {
        return this.absorbedQTS;
    }

    public String getTotalWaterStr() {
        return getVolConverted(this.totalWaterQTS);
    }

    public double getTotalWaterQts() {
        return this.totalWaterQTS;
    }

    public double getThickDecoctRatio() {
        return this.thickDecoctRatio;
    }

    public double getThinDecoctRatio() {
        return this.thinDecoctRatio;
    }

    public String getName() {
        return this.name;
    }


    // mash step methods:
    public int setStepType(int i, String t) {
        if (this.steps.size() < i || this.steps.isEmpty())
            return -1;
        MashStep ms = this.steps.get(i);
        ms.setType(t);
        ms.setStartTemp(calcStepTemp(t));
        ms.setEndTemp(calcStepTemp(t));
        return 0;
    }

    public String getStepType(int i) {
        if (this.steps.size() < i || this.steps.isEmpty())
            return "";
        MashStep ms = this.steps.get(i);
        return ms.getType();
    }


    public String getStepDirections(int i) {
        return this.steps.get(i).getDirections();
    }

    public void setStepMethod(int i, String m) {
        this.steps.get(i).setMethod(m);
        if (m.equals(CEREAL_MASH))
            this.steps.get(i).weightLbs = 0;
        calcMashSchedule();
    }

    public String getStepMethod(int i) {
        return this.steps.get(i).getMethod();
    }

    public void setStepStartTemp(int i, double t) {
        if (this.tempUnits.equals("C")) {
            t = BrewCalcs.cToF(t);
        }
        this.steps.get(i).setStartTemp(t);
        this.steps.get(i).setEndTemp(t);
        this.steps.get(i).setType(calcStepType(t));

        calcMashSchedule();

    }

    public double getStepStartTemp(int i) {
        if (this.tempUnits.equals("F"))
            return BrewCalcs.cToF(this.steps.get(i).getStartTemp());
        else
            return this.steps.get(i).getStartTemp();
    }

    public void setStepEndTemp(int i, double t) {
        if (this.tempUnits.equals("F")) {
            this.steps.get(i).setEndTemp(BrewCalcs.fToC(t));
        } else {
            this.steps.get(i).setEndTemp(t);
        }
        calcMashSchedule();
    }

    public double getStepEndTemp(int i) {
        if (this.tempUnits.equals("F"))
            return BrewCalcs.cToF(this.steps.get(i).getEndTemp());
        else
            return this.steps.get(i).getEndTemp();

    }

    public void setStepRampMin(int i, int m) {
        this.steps.get(i).setRampMin(m);
    }

    public int getStepRampMin(int i) {
        return this.steps.get(i).getRampMin();
    }

    public double getStepTemp(int i) {
        if (this.steps.get(i).getTemp() == 0)
            return 0;
        if (this.tempUnits.equals("C"))
            return this.steps.get(i).getTemp();
        else
            return BrewCalcs.cToF(this.steps.get(i).getTemp());
    }

    public double getStepWeight(int i) {
        double w = this.steps.get(i).weightLbs;
        return Quantity.convertUnit(Quantity.LB, this.myRecipe.getMaltUnits(), w);
    }

    public void setStepWeight(int i, double w) {
        // you can only set the weight on a cereal mash step
        MashStep s = this.steps.get(i);
        if (s.method.equals(CEREAL_MASH)) {
            s.weightLbs = Quantity.convertUnit(this.myRecipe.getMaltUnits(), Quantity.LB, w);
            calcMashSchedule();
        }
    }

    public void setStepMin(int i, int m) {
        this.steps.get(i).setMinutes(m);
    }

    public int getStepMin(int i) {
        return this.steps.get(i).getMinutes();
    }

    public double getStepInVol(int i) {
        double vol = this.steps.get(i).getInVol().getValue();
        return Quantity.convertUnit(Quantity.QT, this.volUnits, vol);
    }

    public Quantity getStepInQuantity(int i) {
        return this.steps.get(i).getInVol();
    }

    public double getStepOutVol(int i) {
        double vol = this.steps.get(i).getOutVol().getValue();
        return Quantity.convertUnit(Quantity.QT, this.volUnits, vol);

    }

    public Quantity getStepOutQuantity(int i) {
        return this.steps.get(i).getOutVol();
    }

    public int getStepSize() {
        return this.steps.size();
    }

    // Introducing: the big huge mash calc method!

    public void calcMashSchedule() {
        // Method to run through the mash table and calculate values

        if (!this.myRecipe.allowRecalcs)
            return;

        double targetTemp;
        double waterAddedQTS = 0;
        double waterEquiv = 0;
        double currentTemp = getGrainTemp();

        double displTemp;
        double tunLoss; // figure out a better way to do this, eg: themal mass
        double decoct;
        int totalMashTime = 0;
        int totalSpargeTime = 0;
        double mashWaterQTS = 0;
        double mashVolQTS = 0;
        int numSparge = 0;
        double totalWeightLbs;
        double totalCerealLbs = 0;

        double maltWeightLbs = this.myRecipe.getTotalMashLbs();

        // convert CurrentTemp to F
        if (this.tempUnits.equals("F")) {
            currentTemp = BrewCalcs.cToF(currentTemp);
            tunLoss = this.tunLossF * 1.8;
        } else {
            tunLoss = this.tunLossF;
        }

        // perform calcs on first record
        if (this.steps.isEmpty())
            return;

        // sort the list
        Collections.sort(this.steps);

        // add up the cereal mash lbs
        for (MashStep step : this.steps) {
            // convert mash ratio to qts/lb if in l/kg
            double mr = step.getMashRatio();
            if (step.getMashRatioU().equalsIgnoreCase(L_PER_KG)) {
                mr *= 0.479325;
            }
            if (step.method.equals("cereal mash")) {
                totalCerealLbs += step.weightLbs;
            }
            step.setStrikeTemp(calcStrikeTemp(step.getStartTemp(), currentTemp, mr, tunLoss));
            step.setStrikeTempU(this.tempUnits);
            waterAddedQTS += step.weightLbs * mr;
            waterEquiv += step.weightLbs * (0.192 + mr);
            mashVolQTS += calcMashVol(step.weightLbs, mr);
        }

        totalWeightLbs = maltWeightLbs - totalCerealLbs;

        // the first step is always an infusion
        MashStep stp = this.steps.get(0);
        totalMashTime += stp.minutes;
        mashWaterQTS += waterAddedQTS;
        stp.inVol.setUnits(Quantity.QT);
        stp.inVol.setAmount(waterAddedQTS);
        stp.method = INFUSION;
        stp.weightLbs = totalWeightLbs;

        // subtract the water added from the Water Equiv so that they are correct when added in the next part of the loop
        waterEquiv -= waterAddedQTS;

        stp.directions = "Mash in with " + SBStringUtils.format(stp.inVol.getValueAs(this.volUnits), 1) + " " + this.volUnits
                + " of water at " + SBStringUtils.format(stp.getDisplayStrikeTemp(), 1) + " " + stp.getStrikeTempU();

        // set TargetTemp to the end temp
        targetTemp = stp.endTemp;

        for (int i = 1; i < this.steps.size(); i++) {
            stp = this.steps.get(i);
            currentTemp = targetTemp; // switch
            targetTemp = stp.startTemp;

            // if this is a former sparge step that's been changed, change
            // the method to infusion
            BrewServer.LOG.info("Mash step Type: " + stp.type + " Method: " + stp.method);
            if (!stp.type.equals(SPARGE) && (stp.method.equals(FLY) || stp.method.equals(BATCH)))
                stp.method = INFUSION;

            String tMethod = stp.method.toLowerCase();
            // do calcs
            if (tMethod.equals(INFUSION)) { // calculate an infusion step
                waterEquiv += waterAddedQTS; // add previous addition to get WE
                double strikeTemp = this.boilTempF; // boiling water

                // Updated the water added
                waterAddedQTS = calcWaterAddition(targetTemp, currentTemp,
                        waterEquiv, this.boilTempF);

                stp.outVol.setAmount(0);
                stp.inVol.setUnits(Quantity.QT);
                stp.inVol.setAmount(waterAddedQTS);
                stp.temp = strikeTemp;
                stp.weightLbs = totalWeightLbs;
                if (this.tempUnits.equals("C"))
                    strikeTemp = 100;
                stp.directions = "Add " + SBStringUtils.format(stp.inVol.getValueAs(this.volUnits), 1) + " " + this.volUnits
                        + " of water at " + SBStringUtils.format(strikeTemp, 1) + " " + this.tempUnits;

                mashWaterQTS += waterAddedQTS;
                mashVolQTS += waterAddedQTS;

            } else if (tMethod.contains(DECOCTION)) { // calculate a decoction step

                waterEquiv += waterAddedQTS; // add previous addition to get WE
                waterAddedQTS = 0;
                double ratio = 0.75;

                if (stp.method.contains(DECOCTION_THICK))
                    ratio = this.thickDecoctRatio;
                else if (stp.method.contains(DECOCTION_THIN))
                    ratio = this.thinDecoctRatio;
                // Calculate volume (qts) of mash to remove
                decoct = calcDecoction2(targetTemp, currentTemp, mashWaterQTS, ratio, totalWeightLbs);
                stp.outVol.setUnits(Quantity.QT);
                stp.outVol.setAmount(decoct);
                stp.inVol.setAmount(0);
                stp.temp = this.boilTempF;
                stp.weightLbs = totalWeightLbs;

                // Updated the decoction, convert to right units & make directions
                stp.directions = "Remove " + SBStringUtils.format(stp.outVol.getValueAs(this.volUnits), 1) + " " + this.volUnits
                        + " of mash, boil, and return to mash.";

            } else if (tMethod.equals(DIRECT)) { // calculate a direct heat step
                waterEquiv += waterAddedQTS; // add previous addition to get WE
                waterAddedQTS = 0;
                displTemp = stp.startTemp;
                if (this.tempUnits.equals("F"))
                    displTemp = BrewCalcs.cToF(displTemp);
                stp.directions = "Add direct heat until mash reaches " + displTemp
                        + " " + this.tempUnits + ".";
                stp.inVol.setAmount(0);
                stp.outVol.setAmount(0);
                stp.temp = 0;
                stp.weightLbs = totalWeightLbs;

            } else if (tMethod.contains(CEREAL_MASH)) { // calculate a cereal mash step
                double mr = stp.getMashRatio();
                if (stp.getMashRatioU().equalsIgnoreCase(L_PER_KG)) {
                    mr *= 0.479325;
                }
                waterEquiv += waterAddedQTS; // add previous addition to get WE
                targetTemp = stp.startTemp;
                double extraWaterQTS = 0;
                double cerealTemp = this.boilTempF;
                double cerealTargTemp = this.cerealMashTemp;
                String addStr = "";

				/*
                 * 1. check the temp of the mash when you add the boiling cereal mash @ default ratio back
				 * 2. if it's > than the step temp, adjust the step temp
				 * 3. if it's < than the step temp, add extra water to increase the "heat equivalencey" of the cereal mash 
				 */

                double cerealWaterEquiv = stp.weightLbs * (0.192 + mr);
                waterAddedQTS = mr * stp.weightLbs;
                double strikeTemp = calcStrikeTemp(this.cerealMashTemp, this.grainTempF, mr, 0);

                double newTemp = ((waterEquiv * currentTemp) + (cerealWaterEquiv * cerealTemp)) / (waterEquiv + cerealWaterEquiv);

                if (newTemp > targetTemp) {
                    stp.startTemp = newTemp;
                }
                if (newTemp < targetTemp) {
                    double addQts = ((waterEquiv * (targetTemp - currentTemp)) / (cerealTemp - targetTemp)) - 0.192;
                    extraWaterQTS = addQts - waterAddedQTS;
                    addStr = " Add " + SBStringUtils.format(Quantity.convertUnit("qt", this.volUnits, extraWaterQTS), 1)
                            + " " + this.volUnits + " water to the cereal mash.";
                }

                // Calculate final temp of cereal mash
                // cerealTemp = (targetTemp * (waterEquiv + cerealWaterEquiv) - (waterEquiv * currentTemp)) / cerealWaterEquiv;


                totalMashTime += stp.minutes;
                mashWaterQTS += waterAddedQTS + extraWaterQTS;
                stp.inVol.setUnits(Quantity.QT);
                stp.inVol.setAmount(waterAddedQTS);
                stp.outVol.setAmount(0);
                stp.temp = strikeTemp;

                // make directions

                String weightStr = SBStringUtils.format(Quantity.convertUnit(Quantity.LB, this.myRecipe.getMaltUnits(), stp.weightLbs), 1)
                        + " " + this.myRecipe.getMaltUnits();
                String volStr = SBStringUtils.format(Quantity.convertUnit(Quantity.QT, this.volUnits, waterAddedQTS), 1)
                        + " " + this.volUnits;
                if (this.tempUnits.equals("F")) {
                    strikeTemp = BrewCalcs.cToF(strikeTemp);
                    cerealTemp = BrewCalcs.cToF(cerealTemp);
                    targetTemp = BrewCalcs.cToF(targetTemp);
                    cerealTargTemp = BrewCalcs.cToF(cerealTargTemp);
                }
                String tempStr = SBStringUtils.format(strikeTemp, 1) + this.tempUnits;
                String tempStr2 = SBStringUtils.format(cerealTemp, 1) + this.tempUnits;
                String tempStr3 = SBStringUtils.format(targetTemp, 1) + this.tempUnits;
                String tempStr4 = SBStringUtils.format(cerealTargTemp, 1) + this.tempUnits;
                stp.directions = "Cereal mash: mash " + weightStr + " grain with " + volStr + " water at " +
                        tempStr + " to hit " + tempStr4 + " and rest.";
                stp.directions += addStr;
                stp.directions += " Raise to " + tempStr2 + " and add to the main mash to reach " + tempStr3;

                // add cereal mash to total weight
                totalWeightLbs += stp.weightLbs;

            } else {

                BrewServer.LOG.warning("Unrecognised mash step: " + stp.method);
            }

            if (stp.type.equals(SPARGE))
                numSparge++;

            else {
                totalMashTime += stp.minutes;
            }
            // set target temp to end temp for next step
            targetTemp = stp.endTemp;

        } // for steps.size()

        this.totalTime = totalMashTime;
        this.volQts = mashVolQTS;

        // water use stats:
        BrewServer.LOG.warning("Total weight: " + totalWeightLbs);
        this.absorbedQTS = totalWeightLbs * 0.52; // figure from HBD

        // spargeTotalQTS = (myRecipe.getPreBoilVol("qt")) - (mashWaterQTS - absorbedQTS);
        this.totalWaterQTS = mashWaterQTS;
        this.spargeQTS = this.myRecipe.getPreBoilVol(Quantity.QT) -
                (mashWaterQTS - this.absorbedQTS - this.deadSpace.getValueAs(Quantity.QT));

        BrewServer.LOG.warning("Sparge Quarts: " + this.spargeQTS);

        // Now let's figure out the sparging:
        if (numSparge == 0)
            return;

        // Amount to collect per sparge
        double col = this.myRecipe.getPreBoilVol(Quantity.QT) / numSparge;
        double charge[] = new double[numSparge];
        double collect[] = new double[numSparge];
        double totalCollectQts = this.myRecipe.getPreBoilVol(Quantity.QT);


        // do we need to add more water to charge up
        // is the amount we need to collect less than the initial mash volume - absorbption
        System.out.println("Collecting: " + col + " MashWater " + mashWaterQTS +
                " Absorbed " + this.absorbedQTS + " Loss: " + this.deadSpace.getValueAs(Quantity.QT));

        if (col <= (mashWaterQTS - this.absorbedQTS)) {
            charge[0] = 0;
            collect[0] = mashWaterQTS - this.absorbedQTS; // how much is left over from the mash
        } else {
            charge[0] = col - (mashWaterQTS - this.absorbedQTS); // add the additional water to get out the desired first collection amount PER sparge
            collect[0] = col;
        }

        // do we need any more steps?
        if (numSparge > 1) {
            /*
            batch_1_sparge_liters = (boil_size_l/<total number of steps> ) - mash_water_l + grain_wt_kg * 0.625)
		    		batch_2_liters = boil_size_l / <total number of steps>
		    	*/
            BrewServer.LOG.info("NumSparge: " + numSparge);

            BrewServer.LOG.info("Collecting: " + col);
            for (int i = 1; i < numSparge; i++) {
                charge[i] = col;
                collect[i] = col;
            }
        }

        int j = 0;
        for (int i = 1; i < this.steps.size(); i++) {
            stp = this.steps.get(i);
            if (stp.getType().equals(SPARGE)) {

                stp.inVol.setUnits(Quantity.QT);
                stp.inVol.setAmount(charge[j]);

                stp.outVol.setUnits(Quantity.QT);
                stp.outVol.setAmount(collect[j]);

                stp.temp = this.SPARGETMPF;
                totalSpargeTime += stp.getMinutes();
                String collectStr = SBStringUtils.format(Quantity.convertUnit(Quantity.QT, this.volUnits, collect[j]), 2) +
                        " " + this.volUnits;
                String tempStr;
                if (this.tempUnits.equals("F")) {
                    tempStr = "" + SBStringUtils.format(this.SPARGETMPF, 1) + "F";
                } else {
                    tempStr = SBStringUtils.format(BrewCalcs.fToC(this.SPARGETMPF), 1) + "C";
                }

                if (numSparge > 1) {
                    stp.setMethod(BATCH);
                    String add = SBStringUtils.format(Quantity.convertUnit(Quantity.QT, this.volUnits, charge[j]), 2) +
                            " " + this.volUnits;
                    stp.setDirections("Add " + add + " at " + tempStr + " to collect " + collectStr);

                } else {
                    stp.inVol.setUnits(Quantity.QT);
                    stp.inVol.setAmount(this.spargeQTS);

                    stp.outVol.setUnits(Quantity.QT);
                    stp.outVol.setAmount(collect[j]);

                    stp.setMethod(FLY);
                    stp.setDirections("Sparge with " +
                            SBStringUtils.format(Quantity.convertUnit("qt", this.volUnits, this.spargeQTS), 1) +
                            " " + this.volUnits + " at " + tempStr + " to collect " + collectStr);
                }
                j++;
            }
        }
    }


    // private methods:

	/* from John Palmer:
     * 		Vd (quarts) = [(T2 - T1)(.4G + 2W)] / [(Td - T1)(.4g + w)]
            Where:
            Vd = decoction volume
            T1 = initial mash temperature
            T2 = target mash temperature
            Td = decoction temperature (212F)
            G = Grainbill weight
            W = volume of water in mash (i.e. initial infusion volume)
            g = pounds of grain per quart of decoction = 1/(Rd + .32)
            w = quarts of water per quart of decoction = g*Rd*water density = 2gRd
            Rd = ratio of grain to water in the decoction volume (range of .6 to 1
            quart/lb)
            thick decoctions will have a ratio of .6-.7, thinner decoctions will
           have a ratio of .8-.9
	 */

    private double calcDecoction2(double targetTemp, double currentTemp, double waterVolQTS, double ratio, double weightLbs) {
        double decoctQTS;

        double g = 1 / (ratio + .32);
        double w = 2 * g * ratio;

        decoctQTS = ((targetTemp - currentTemp) * ((0.4 * weightLbs) + (2 * waterVolQTS)))
                / ((this.boilTempF - currentTemp) * (0.4 * g + w));


        return decoctQTS;
    }


    private String calcStepType(double temp) {
        String stepType = "none";
        // less than 90, none
        // 86 - 95 - acid
        if (temp >= this.ACIDTMPF && temp < this.GLUCANTMPF)
            stepType = ACID;
            // 95 - 113 - glucan
        else if (temp < this.PROTEINTMPF)
            stepType = GLUCAN;
            // 113 - 131 protein
        else if (temp < this.BETATMPF)
            stepType = PROTEIN;
            // 131 - 150 beta
        else if (temp < this.ALPHATMPF)
            stepType = BETA;
            // 150-162 alpha
        else if (temp < this.MASHOUTTMPF)
            stepType = ALPHA;
            // 163-169, mashout
        else if (temp < this.SPARGETMPF)
            stepType = MASHOUT;
            // over 170, sparge
        else if (temp >= this.SPARGETMPF)
            stepType = SPARGE;

        return stepType;
    }

    private double calcStepTemp(String stepType) {
        float stepTempF = 0;
        if (Objects.equals(stepType, ACID))
            stepTempF = (this.ACIDTMPF + this.GLUCANTMPF) / 2;
        else if (Objects.equals(stepType, GLUCAN))
            stepTempF = (this.GLUCANTMPF + this.PROTEINTMPF) / 2;
        else if (Objects.equals(stepType, PROTEIN))
            stepTempF = (this.PROTEINTMPF + this.BETATMPF) / 2;
        else if (Objects.equals(stepType, BETA))
            stepTempF = (this.BETATMPF + this.ALPHATMPF) / 2;
        else if (Objects.equals(stepType, ALPHA))
            stepTempF = (this.ALPHATMPF + this.MASHOUTTMPF) / 2;
        else if (Objects.equals(stepType, MASHOUT))
            stepTempF = (this.MASHOUTTMPF + this.SPARGETMPF) / 2;
        else if (Objects.equals(stepType, SPARGE))
            stepTempF = this.SPARGETMPF;

        return stepTempF;
    }

    double calcMashVol(double grainWeightLBS, double ratio) {
        // given lbs and ratio, what is the volume of the grain in quarts?
        // note: this calc is for the first record only, and returns the heat equivalent of
        // grain + water added for first infusion
        // HBD posts indicate 0.32, but reality is closer to 0.42

        return (grainWeightLBS * (0.42 + ratio));
    }

    double calcStrikeTemp(double targetTemp, double currentTemp, double ratio,
                          double tunLossF) {
        // calculate strike temp
        // Ratio is in quarts / lb, TunLoss is in F

        // this uses thermal mass:
        // double strikeTemp = (maltWeightLbs + thermalMass)*( targetTemp - currentTemp )/( boilTempF - targetTemp );

        return (targetTemp + 0.192 * (targetTemp - currentTemp) / ratio)
                + tunLossF;
    }

    double calcWaterAddition(double targetTemp, double currentTemp,
                             double mashVol, double boilTempF) {
        // calculate amount of boiling water to add to raise mash to new temp
        return (mashVol * (targetTemp - currentTemp) / (boilTempF - targetTemp));
    }


    public String toXml() {

        calcMashSchedule();
        StringBuilder sb = new StringBuilder();
        sb.append("  <MASH>\n");
        sb.append(SBStringUtils.xmlElement("NAME", this.name, 4));
        sb.append(SBStringUtils.xmlElement("MASH_VOLUME", SBStringUtils.format(Quantity.convertUnit("qt", this.volUnits, this.volQts), 2), 4));
        sb.append(SBStringUtils.xmlElement("MASH_VOL_U", "" + this.volUnits, 4));
        sb.append(SBStringUtils.xmlElement("MASH_RATIO", "" + this.mashRatio, 4));
        sb.append(SBStringUtils.xmlElement("MASH_RATIO_U", "" + this.mashRatioU, 4));
        sb.append(SBStringUtils.xmlElement("MASH_TIME", "" + this.totalTime, 4));
        sb.append(SBStringUtils.xmlElement("MASH_TMP_U", "" + this.tempUnits, 4));
        sb.append(SBStringUtils.xmlElement("THICK_DECOCT_RATIO", "" + this.thickDecoctRatio, 4));
        sb.append(SBStringUtils.xmlElement("THIN_DECOCT_RATIO", "" + this.thinDecoctRatio, 4));
        if (this.tempUnits.equals("C")) {
            sb.append(SBStringUtils.xmlElement("MASH_TUNLOSS_TEMP", "" + (this.tunLossF / 1.8), 4));
            sb.append(SBStringUtils.xmlElement("GRAIN_TEMP", "" + BrewCalcs.fToC(this.grainTempF), 4));
            sb.append(SBStringUtils.xmlElement("BOIL_TEMP", "" + BrewCalcs.fToC(this.boilTempF), 4));
        } else {
            sb.append(SBStringUtils.xmlElement("MASH_TUNLOSS_TEMP", "" + this.tunLossF, 4));
            sb.append(SBStringUtils.xmlElement("GRAIN_TEMP", "" + this.grainTempF, 4));
            sb.append(SBStringUtils.xmlElement("BOIL_TEMP", "" + this.boilTempF, 4));
        }
        for (MashStep step : this.steps) {
            sb.append("    <ITEM>\n");
            sb.append("      <TYPE>").append(step.type).append("</TYPE>\n");
            sb.append("      <TEMP>").append(step.startTemp).append("</TEMP>\n");
            if (this.tempUnits.equals("C"))
                sb.append("      <DISPL_TEMP>").append(SBStringUtils.format(BrewCalcs.fToC(step.startTemp), 1)).append("</DISPL_TEMP>\n");
            else
                sb.append("      <DISPL_TEMP>").append(step.startTemp).append("</DISPL_TEMP>\n");
            sb.append("      <END_TEMP>").append(step.endTemp).append("</END_TEMP>\n");
            if (this.tempUnits.equals("C"))
                sb.append("      <DISPL_END_TEMP>").append(SBStringUtils.format(BrewCalcs.fToC(step.endTemp), 1)).append("</DISPL_END_TEMP>\n");
            else
                sb.append("      <DISPL_END_TEMP>").append(step.endTemp).append("</DISPL_END_TEMP>\n");
            sb.append("      <MIN>").append(step.minutes).append("</MIN>\n");
            sb.append("      <RAMP_MIN>").append(step.rampMin).append("</RAMP_MIN>\n");
            sb.append("      <METHOD>").append(step.method).append("</METHOD>\n");
            sb.append("      <WEIGHT_LBS>").append(step.weightLbs).append("</WEIGHT_LBS>\n");
            sb.append("      <DIRECTIONS>").append(step.directions).append("</DIRECTIONS>\n");
            sb.append("    </ITEM>\n");
        }

        sb.append("  </MASH>\n");
        return sb.toString();
    }

    /**
     * Return the current mash steps as a JSON Representation.
     *
     * @return The new JSONObject status
     */
    @SuppressWarnings("unchecked")
    public JSONObject toJSONObject(String device) {
        JSONObject mashObject = new JSONObject();

        for (int i = 0; i < this.steps.size(); i++) {
            MashStep st = this.steps.get(i);
            JSONObject currentStep = new JSONObject();
            currentStep.put(TYPE, st.type);
            currentStep.put(METHOD, st.method);
            currentStep.put(TEMP, st.getStartTemp());
            currentStep.put(DURATION, st.minutes);
            currentStep.put(TEMP_UNIT, this.tempUnits);
            currentStep.put(POSITION, i);
            mashObject.put(i, currentStep);
        }

        if (device != null) {
            mashObject.put(PID, device);
        }
        return mashObject;
    }

}