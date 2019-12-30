
package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.Messages;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.devices.TempProbe;
import com.sb.elsinore.notificiations.Notifications;
import com.sb.elsinore.triggers.TemperatureTrigger;
import com.sb.elsinore.triggers.WaitTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;


/**
 * StrangeBrew Java - a homebrew recipe calculator
 * Copyright (C) 2005  Drew Avis
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
@SuppressWarnings("unused")
public class Recipe {

    public boolean allowRecalcs = true;
    public Mash mash = null;
    /**
     * Implement a local comparator.
     */
    Comparator<Fermentable> fermCompare = (h1, h2) -> {
        if (h1.getAmountAs(Quantity.LB) > h2.getAmountAs(Quantity.LB)) {
            return 1;
        }
        if (h1.getAmountAs(Quantity.LB) < h2.getAmountAs(Quantity.LB)) {
            return -1;
        }
        if (h1.getAmountAs(Quantity.LB) == h2.getAmountAs(Quantity.LB)) {
            return 0;
        }
        return 0;
    };
    // basics:
    private String version = "";
    private boolean isDirty = false;
    private double attenuation;
    private int boilMinutes = 60;
    private String brewer;
    private String comments;
    private GregorianCalendar created;
    private double efficiency = 75;
    private double estOg = 1.050;
    private double estFg;
    private double ibu;
    private boolean mashed;
    private String name;
    private Style style = new Style();
    private List<Yeast> yeasts = new ArrayList<>();
    private WaterProfile sourceWater = new WaterProfile();
    private WaterProfile targetWater = new WaterProfile();
    private List<Salt> brewingSalts = new ArrayList<>();
    private Acid acid = Acid.getAcidByName(Acid.CITRIC);
    private Logger logger = LoggerFactory.getLogger(Notifications.class);
    // water use:
    //
    // All water use is now based on calculation from mash and always done
    // on-the-fly with getters <-------------
    //
    // Only hard data deserves a variable!
    // initial vol = mash + sparge
    // pre boil = initial
    // post boil = pre - evap - chill
    // final - post - kettle loss - trub loss - misc loss (+ dillution) (- tun
    // dead space)
    // total = initial (+ dillution)
    private Quantity postBoilVol = new Quantity();
    private Quantity kettleLossVol = new Quantity();
    private Quantity trubLossVol = new Quantity();
    private Quantity miscLossVol = new Quantity();
    // Carbonation
    private double bottleTemp = 0.0;
    private double servTemp = 0.0;
    private double targetVol = 0.0;
    private PrimeSugar primeSugar = new PrimeSugar();
    private String carbTempU = "";
    private boolean kegged = false;
    private double kegPSI = 0.0;
    // options:
    private String colourMethod = "";
    private String hopUnits = "";
    private String maltUnits = "";
    private String ibuCalcMethod = "";
    private double ibuHopUtil = 1.0;
    private String evapMethod = "percent";
    private String alcMethod = BrewCalcs.ALC_BY_VOLUME;
    private double pelletHopPct = 0.0;
    private String bottleU = "";
    private double bottleSize = 0.0;
    private double otherCost = 0.0;
    private int fwhTime;
    private int dryHopTime;
    private int mashHopTime;
    // totals:
    private double totalMaltCost;
    private double totalHopsCost;
    private double totalMiscCost;
    private double totalMaltLbs;
    private double totalHopsOz;
    private double totalMashLbs;
    private int totalFermentTime;
    // ingredients
    private List<Hop> hops = new ArrayList<>();
    private List<Fermentable> fermentables = new ArrayList<>();
    private List<Misc> misc = new ArrayList<>();
    private List<FermentStep> fermentationSteps = new ArrayList<>();
    // notes
    private List<Note> notes = new ArrayList<>();
    private Equipment equipmentProfile;
    private Quantity preBoilVol = new Quantity();
    private String tasteNotes;
    private double tasteRating;
    private double measuredOg;
    private double measuredFg;
    private String dateBrewed;
    private double primeSugarEquiv;
    private double kegPrimingFactor;
    private String gravUnits = "SG";
    private String carbMethod;

    // default constuctor
    public Recipe() {
    }

    // Recipe copy constructor
    public Recipe(Recipe r) {
        this.version = r.version;

        this.isDirty = r.getDirty();
        this.allowRecalcs = r.allowRecalcs;
        this.attenuation = r.getAttenuation();
        this.boilMinutes = r.getBoilMinutes();
        this.brewer = r.getBrewer();
        this.comments = r.getComments();
        this.created = r.getCreated();
        this.efficiency = r.getEfficiency();
        this.estOg = r.estOg;
        this.estFg = r.getEstFg();
        this.ibu = r.ibu;
        this.mashed = r.mashed;
        this.name = r.getName();
        this.style = r.getStyleObj();
        this.yeasts = r.getYeasts();
        this.sourceWater = r.getSourceWater();
        this.targetWater = r.getTargetWater();
        this.brewingSalts = new ArrayList<>(r.getSalts());
        this.acid = r.getAcid();
        this.mash = r.mash;

        // water use:
        this.kettleLossVol = new Quantity(r.getVolUnits(), r.getKettleLoss(r.getVolUnits()));
        this.trubLossVol = new Quantity(r.getVolUnits(), r.getTrubLoss(r.getVolUnits()));
        this.miscLossVol = new Quantity(r.getVolUnits(), r.getMiscLoss(r.getVolUnits()));
        this.postBoilVol = new Quantity(r.getVolUnits(), r.getPostBoilVol(r.getVolUnits()));

        // Carbonation
        this.bottleTemp = r.getBottleTemp();
        this.servTemp = r.getServTemp();
        this.targetVol = r.getTargetVol();
        this.primeSugar = r.getPrimeSugar();
        this.carbTempU = r.getCarbTempU();
        this.kegged = r.isKegged();
        this.kegPSI = r.getKegPSI();

        this.colourMethod = r.getColourMethod();
        this.hopUnits = r.getHopUnits();
        this.maltUnits = r.getMaltUnits();
        this.ibuCalcMethod = r.getIBUMethod();
        this.ibuHopUtil = r.ibuHopUtil;
        this.evapMethod = r.getEvapMethod();
        this.alcMethod = r.getAlcMethod();
        this.pelletHopPct = r.getPelletHopPct();
        this.bottleU = r.getBottleU();
        this.bottleSize = r.getBottleSize();
        this.otherCost = r.getOtherCost();
        this.fwhTime = r.fwhTime;
        this.dryHopTime = r.dryHopTime;
        this.mashHopTime = r.mashHopTime;

        // totals: (all should be pure calculation, not stored!!!
        this.totalMaltCost = r.getTotalMaltCost();
        this.totalHopsCost = r.getTotalHopsCost();
        this.totalMiscCost = r.getTotalMiscCost();
        this.totalMaltLbs = r.getTotalMaltLbs();
        this.totalHopsOz = r.getTotalHopsOz();
        this.totalMashLbs = r.getTotalMaltLbs();
        this.totalFermentTime = r.getTotalFermentTime();

        // ingredients
        this.hops = new ArrayList<>(r.hops);
        this.fermentables = new ArrayList<>(r.fermentables);
        this.misc = new ArrayList<>(r.misc);
        // Fermentation
        this.fermentationSteps = new ArrayList<>(r.fermentationSteps);
        // notes
        this.notes = new ArrayList<>(r.notes);
    }

    public static String buildString(final char ch, final int length) {
        final char newStr[] = new char[length];
        for (int i = 0; i < length; ++i) {
            newStr[i] = ch;
        }
        return new String(newStr);
    }

    public static String padLeft(final String str, final int fullLength, final char ch) {
        return (fullLength > str.length()) ? str.concat(Recipe.buildString(ch, fullLength - str.length())) : str;
    }

    public static String padRight(final String str, final int fullLength, final char ch) {
        return (fullLength > str.length()) ? Recipe.buildString(ch, fullLength - str.length()).concat(str) : str;
    }

    // Getters:
    public double getAlcohol() {
        return BrewCalcs.calcAlcohol(getAlcMethod(), this.estOg, getEstFg());
    }

    public String getAlcMethod() {
        return this.alcMethod;
    }

    public void setAlcMethod(final String s) {
        this.isDirty = true;
        this.alcMethod = s;
    }

    public double getAttenuation() {
        if (this.yeasts.size() > 0 && this.yeasts.get(0).getAttenuation() > 0.0) {
            return this.yeasts.get(0).getAttenuation();
        }
        return this.attenuation;
    }

    public void setAttenuation(final double a) {
        this.isDirty = true;
        if ((a != this.attenuation) && (a > 0)) {
            this.attenuation = a;
            calcMaltTotals();
        }

    }

    public int getBoilMinutes() {
        return this.boilMinutes;
    }

    public void setBoilMinutes(final int b) {
        this.isDirty = true;
        this.boilMinutes = b;
    }

    public String getBottleU() {
        return this.bottleU;
    }

    public void setBottleU(final String u) {
        this.isDirty = true;
        this.bottleU = u;
    }

    public double getBottleSize() {
        return this.bottleSize;
    }

    public void setBottleSize(final double b) {
        this.isDirty = true;
        this.bottleSize = b;
    }

    public double getBUGU() {
        double bugu = 0.0;
        if (this.estOg != 1.0) {
            bugu = this.ibu / ((this.estOg - 1) * 1000);
        }
        return bugu;
    }

    public String getBrewer() {
        return this.brewer;
    }

    public void setBrewer(final String b) {
        this.isDirty = true;
        this.brewer = b;
    }

    public String getComments() {
        return this.comments;
    }

    public void setComments(final String c) {
        this.isDirty = true;
        this.comments = c;
    }

    public double getColour() {
        return getColour(getColourMethod());
    }

    public double getMcu() {
        double mcu = 0;
        for (final Fermentable m : this.fermentables) {
            mcu += m.getLov() * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }

        return mcu;
    }

    public double getColour(final String method) {
        return BrewCalcs.calcColour(getMcu(), method);
    }

    public String getColourMethod() {
        return this.colourMethod;
    }

    public void setColourMethod(final String c) {
        this.isDirty = true;
        this.colourMethod = c;
        calcMaltTotals();
    }

    public GregorianCalendar getCreated() {
        return this.created;
    }

    public void setCreated(final Date d) {
        this.isDirty = true;
        this.created.setTime(d);
    }

    public double getEfficiency() {
        return this.efficiency;
    }

    public void setEfficiency(final double e) {
        this.isDirty = true;
        if ((e != this.efficiency) && (e > 0)) {
            this.efficiency = e;
            calcMaltTotals();
        }
    }

    public double getEstOg() {
        return this.estOg;
    }

    public void setEstOg(final double o) {
        if (o == 0) {
            return;
        }
        this.isDirty = true;
        if ((o != this.estOg) && (o > 0)) {
            this.estOg = o;
            this.attenuation = 100 - ((this.estFg - 1) / (this.estOg - 1) * 100);
            calcEfficiency();
        }
    }

    public double getEstFg() {
        return this.estFg;
    }

    public void setEstFg(final double f) {
        this.isDirty = true;
        if ((f != this.estFg) && (f > 0)) {
            this.estFg = f;
            this.attenuation = 100 - ((this.estFg - 1) / (this.estOg - 1) * 100);
        }
    }

    public double getEvap() {
        return this.equipmentProfile.getEvapRate();
    }

    public void setEvap(final double e) {
        this.isDirty = true;
        this.equipmentProfile.setEvapRate(e);
        calcMaltTotals();
        calcHopsTotals();
    }

    public String getEvapMethod() {
        return this.evapMethod;
    }

    public void setEvapMethod(final String e) {
        this.isDirty = true;
        this.evapMethod = e;
    }

    public Mash getMash() {
        return this.mash;
    }

    public void setMash(Mash mash) {
        this.mash = mash;
    }

    public double getIbu() {
        return this.ibu;
    }

    public String getIBUMethod() {
        return this.ibuCalcMethod;
    }

    public void setIBUMethod(final String s) {
        if (s == null || s.equals("")) {
            return;
        }
        this.isDirty = true;
        this.ibuCalcMethod = s;
        calcHopsTotals();
    }

    public String getMaltUnits() {
        return this.maltUnits;
    }

    public void setMaltUnits(final String m) {
        this.isDirty = true;
        this.maltUnits = m;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String n) {
        this.isDirty = true;
        this.name = n;
    }

    public double getOtherCost() {
        return this.otherCost;
    }

    public void setOtherCost(final double c) {
        this.isDirty = true;
        this.otherCost = c;
    }

    public double getPelletHopPct() {
        return this.pelletHopPct;
    }

    public void setPelletHopPct(final double p) {
        this.isDirty = true;
        this.pelletHopPct = p;
        calcHopsTotals();
    }

    // Water getters - the calculated version
    public double getKettleLoss(final String s) {
        return this.kettleLossVol.getValueAs(s);
    }

    public double getMiscLoss(final String s) {
        return this.miscLossVol.getValueAs(s);
    }

    // Setters:

    public double getTotalWaterVol(final String s) {
        Quantity q = new Quantity(Quantity.QT, this.mash.getTotalWaterQts() + this.mash.getSpargeVol());
        return q.getValueAs(s);
    }

    public double getChillShrinkVol(final String s) {
        if (this.equipmentProfile != null) {
            return getPostBoilVol(s) * this.equipmentProfile.getChillPercent();
        }
        double CHILLPERCENT = 0.03;
        return getPostBoilVol(s) * CHILLPERCENT;
    }

    public double getPreBoilVol(final String s) {
        if (this.equipmentProfile != null && this.equipmentProfile.isCalcBoilVol()) {
            return getPostBoilVol(s)
                    + getEvapVol(s)
                    + getChillShrinkVol(s)
                    + this.equipmentProfile.getTopupKettle()
                    + this.equipmentProfile.getTrubChillerLoss().getValueAs(s)
                    + this.equipmentProfile.getLauterDeadspace().getValueAs(s);
        }

        return this.preBoilVol.getValueAs(s);
    }

    public double getFinalWortVol(final String s) {
        return getPostBoilVol(s) - (getKettleLoss(s) + getTrubLoss(s) + getMiscLoss(s));
    }

    public Quantity getPostBoilVol() {
        return this.postBoilVol;
    }

    public double getPostBoilVol(final String s) {
        return this.postBoilVol.getValueAs(s);
    }

    public double getTrubLoss(final String s) {
        return this.trubLossVol.getValueAs(s);
    }

    public double getEvapVol(final String s) {
        // JvH changing the boiltime, changes the post boil volume (NOT the pre
        // boil)
        double e;
        if (this.evapMethod.equals("Constant")) {
            e = getEvap() * getBoilMinutes() / 60;

            Quantity tVol = new Quantity(getVolUnits(), e);
            return tVol.getValueAs(s);
        } else { // %
            e = (getPostBoilVol(s)//  - equipmentProfile.getTopupKettle()
                    - this.equipmentProfile.getTrubChillerLoss().getValueAs(s)
                    - this.equipmentProfile.getLauterDeadspace().getValueAs(s))
                    * (getEvap() / 100) * getBoilMinutes() / 60;
        }
        return e;
    }

    public String getVolUnits() {
        if (this.postBoilVol != null) {
            return this.postBoilVol.getUnits();
        }
        return "";
    }

    public void setVolUnits(final String v) {
        this.isDirty = true;
        this.kettleLossVol.convertTo(v);
        this.postBoilVol.convertTo(v);
        this.trubLossVol.convertTo(v);
        this.miscLossVol.convertTo(v);
        calcMaltTotals();
        calcHopsTotals();
    }

    public double getSparge() {
        // return (getVolConverted(spargeQTS));
        return this.mash.getSpargeVol();
    }

    public String getStyle() {
        return this.style.getName();
    }

    public void setStyle(final String s) {
        this.isDirty = true;
        this.style.setName(s);
    }

    public void setStyle(final Style s) {
        this.isDirty = true;
        this.style = s;
        this.style.setComplete();
    }

    public Style getStyleObj() {
        return this.style;
    }

    public double getTotalHopsOz() {
        return this.totalHopsOz;
    }

    public double getTotalHops() {
        return Quantity.convertUnit(Quantity.OZ, this.hopUnits, this.totalHopsOz);
    }

    public double getTotalHopsCost() {
        return this.totalHopsCost;
    }

    public double getTotalMaltCost() {
        return this.totalMaltCost;
    }

    public double getTotalMashLbs() {
        return this.totalMashLbs;
    }

    public double getTotalMash() {
        return Quantity.convertUnit(Quantity.LB, getMaltUnits(), this.totalMashLbs);
    }

    public double getTotalMaltLbs() {
        return this.totalMaltLbs;
    }

    public double getTotalMalt() {
        return Quantity.convertUnit(Quantity.LB, this.maltUnits, this.totalMaltLbs);
    }

    public double getTotalMiscCost() {
        return this.totalMiscCost;
    }

    public String getYeast(int i) {
        return this.yeasts.get(i).getName();
    }

    public Yeast getYeastObj(int i) {
        return this.yeasts.get(i);
    }

    public boolean getDirty() {
        return this.isDirty;
    }

    // Set saved flag
    public void setDirty(final boolean d) {
        this.isDirty = d;
    }

    /*
     * Turn off allowRecalcs when you are importing a recipe, so that strange
     * things don't happen. BE SURE TO TURN BACK ON!
     */
    public void setAllowRecalcs(final boolean b) {
        this.allowRecalcs = b;
    }

    public void setHopsUnits(final String h) {
        this.isDirty = true;
        this.hopUnits = h;
    }

    public void setKettleLoss(final Quantity q) {
        this.isDirty = true;
        this.kettleLossVol = q;
        calcMaltTotals();
    }

    public void setMashed(final boolean m) {
        this.isDirty = true;
        this.mashed = m;
    }

    public void setMashRatio(final double m) {
        this.isDirty = true;
        this.mash.setMashRatio(m);
    }

    public void setMashRatioU(final String u) {
        this.isDirty = true;
        this.mash.setMashRatioU(u);
    }

    public void setMiscLoss(final Quantity m) {
        this.isDirty = true;
        this.miscLossVol = m;
        calcMaltTotals();
    }

    public void setTrubLoss(final Quantity t) {
        this.isDirty = true;
        this.trubLossVol = t;
        calcMaltTotals();
    }

    public void setYeastName(final int i, final String s) {
        this.isDirty = true;
        this.yeasts.get(i).setName(s);
    }

    public void setYeast(final int i, final Yeast y) {
        this.isDirty = true;
        this.yeasts.add(i, y);
    }

    public void setVersion(final String v) {
        this.isDirty = true;
        this.version = v;
    }

    // Fermentation Steps
    // Getters
    public int getFermentStepSize() {
        return this.fermentationSteps.size();
    }

    public String getFermentStepType(final int i) {
        return this.fermentationSteps.get(i).getType();
    }

    public int getFermentStepTime(final int i) {
        return this.fermentationSteps.get(i).getTime();
    }

    public int getFermentStepTimeMins(final int i) {
        return this.fermentationSteps.get(i).getTime() * 24 * 60;
    }

    public double getFermentStepTemp(final int i) {
        return this.fermentationSteps.get(i).getTemp();
    }

    public String getFermentStepTempU(final int i) {
        return this.fermentationSteps.get(i).getTempU();
    }

    public FermentStep getFermentStep(final int i) {
        return this.fermentationSteps.get(i);
    }

    public FermentStep getFermentStep(final String stepType) {
        for (FermentStep fs : this.fermentationSteps) {
            if (fs.getType().equals(stepType)) {
                return fs;
            }
        }

        return new FermentStep();
    }

    public int getTotalFermentTime() {
        return this.totalFermentTime;
    }

    // Setters
    public void setFermentStepType(final int i, final String s) {
        this.isDirty = true;
        this.fermentationSteps.get(i).setType(s);
    }

    public void setFermentStepTime(final int i, final int t) {
        this.isDirty = true;
        this.fermentationSteps.get(i).setTime(t);
    }

    public void setFermentStepTemp(final int i, final double d) {
        this.isDirty = true;
        this.fermentationSteps.get(i).setTemp(d);
    }

    public void setFermentStepTempU(final int i, final String s) {
        this.isDirty = true;
        this.fermentationSteps.get(i).setTempU(s);
    }

    public void addFermentStep(final FermentStep fs) {
        this.isDirty = true;
        this.fermentationSteps.add(fs);
        calcFermentTotals();
    }

    public FermentStep delFermentStep(final int i) {
        this.isDirty = true;
        FermentStep temp = null;
        if (!this.fermentationSteps.isEmpty() && (i > -1) && (i < this.fermentationSteps.size())) {
            temp = this.fermentationSteps.remove(i);
            calcFermentTotals();
        }

        return temp;
    }

    public void calcFermentTotals() {
        this.totalFermentTime = 0;
        for (FermentStep fermentationStep : this.fermentationSteps) {
            this.totalFermentTime += fermentationStep.getTime();
        }

        Collections.sort(this.fermentationSteps);
    }

    // hop list get functions:
    public String getHopUnits() {
        return this.hopUnits;
    }

    public Hop getHop(final int i) {
        if (i < this.hops.size()) {
            return this.hops.get(i);
        } else {
            return null;
        }
    }

    public int getHopsListSize() {
        return this.hops.size();
    }

    public String getHopName(final int i) {
        return this.hops.get(i).getName();
    }

    public String getHopType(final int i) {
        return this.hops.get(i).getType();
    }

    public double getHopAlpha(final int i) {
        return this.hops.get(i).getAlpha();
    }

    public String getHopUnits(final int i) {
        return this.hops.get(i).getUnits();
    }

    public String getHopAdd(final int i) {
        return this.hops.get(i).getAdd();
    }

    public int getHopMinutes(final int i) {
        return this.hops.get(i).getMinutes();
    }

    public double getHopIBU(final int i) {
        return this.hops.get(i).getIBU();
    }

    public double getHopCostPerU(final int i) {
        return this.hops.get(i).getCostPerU();
    }

    public double getHopAmountAs(final int i, final String s) {
        return this.hops.get(i).getAmountAs(s);
    }

    public String getHopDescription(final int i) {
        return this.hops.get(i).getDescription();
    }

    // hop list set functions
    public void setHopUnits(final int i, final String u) {
        this.isDirty = true;
        this.hops.get(i).setUnits(u);
    }

    public void setHopName(final int i, final String n) {
        this.isDirty = true;
        this.hops.get(i).setName(n);
    }

    public void setHopType(final int i, final String t) {
        this.isDirty = true;
        this.hops.get(i).setType(t);
    }

    public void setHopAdd(final int i, final String a) {
        this.isDirty = true;
        this.hops.get(i).setAdd(a);
    }

    public void setHopAlpha(final int i, final double a) {
        this.isDirty = true;
        this.hops.get(i).setAlpha(a);
    }

    public void setHopMinutes(final int i, final int m) {
        this.isDirty = true;
        // have to re-sort hops
        this.hops.get(i).setMinutes(m);
    }

    public void setHopCost(final int i, final String c) {
        this.isDirty = true;
        this.hops.get(i).setCost(c);
    }

    public void setHopAmount(final int i, final double a) {
        this.isDirty = true;
        this.hops.get(i).setAmount(a);
    }

    // fermentable get methods
    // public ArrayList getFermentablesList() { return fermentables; }
    public Fermentable getFermentable(final int i) {
        if (i < this.fermentables.size()) {
            return this.fermentables.get(i);
        } else {
            return null;
        }
    }

    public int getMaltListSize() {
        return this.fermentables.size();
    }

    public String getMaltName(final int i) {
        return this.fermentables.get(i).getName();
    }

    public String getMaltUnits(final int i) {
        return this.fermentables.get(i).getUnits();
    }

    public double getMaltPppg(final int i) {
        return this.fermentables.get(i).getPppg();
    }

    public double getMaltLov(final int i) {
        return this.fermentables.get(i).getLov();
    }

    public double getMaltCostPerU(final int i) {
        return this.fermentables.get(i).getCostPerU();
    }

    public double getMaltCostPerUAs(final int i, String s) {
        return this.fermentables.get(i).getCostPerUAs(s);
    }

    public double getMaltPercent(final int i) {
        return this.fermentables.get(i).getPercent();
    }

    public double getMaltAmountAs(final int i, final String s) {
        return this.fermentables.get(i).getAmountAs(s);
    }

    public String getMaltDescription(final int i) {
        return this.fermentables.get(i).getDescription();
    }

    public boolean getMaltMashed(final int i) {
        return this.fermentables.get(i).getMashed();
    }

    public boolean getMaltSteep(final int i) {
        return this.fermentables.get(i).getSteep();
    }

    public boolean getMaltFerments(final int i) {
        return this.fermentables.get(i).ferments();
    }

    // fermentable set methods
    public void setMaltName(final int i, final String n) {
        // have to re-sort
        this.isDirty = true;
        this.fermentables.get(i).setName(n);

    }

    public void setMaltUnits(final int i, final String u) {
        this.isDirty = true;
        this.fermentables.get(i).setUnits(u);
        this.fermentables.get(i).setCost(this.fermentables.get(i).getCostPerUAs(u));
    }

    public void setMaltAmount(final int i, final double a) {
        setMaltAmount(i, a, false);
    }

    public void setMaltAmount(final int i, final double a, final boolean sort) {
        this.isDirty = true;
        this.fermentables.get(i).setAmount(a);

        if (sort) {
            calcMaltTotals();
        }
    }

    public void setMaltAmountAs(final int i, final double a, final String u) {
        this.isDirty = true;
        this.fermentables.get(i).setAmountAs(a, u);
        this.fermentables.get(i).setCost(this.fermentables.get(i).getCostPerUAs(u));
    }

    public void setMaltPppg(final int i, final double p) {
        this.isDirty = true;
        this.fermentables.get(i).setPppg(p);
    }

    public void setMaltLov(final int i, final double l) {
        this.isDirty = true;

        this.fermentables.get(i).setLov(l);
    }

    public void setMaltCost(final int i, final String c) {
        this.isDirty = true;
        this.fermentables.get(i).setCost(c);
    }

    public void setMaltCost(final int i, final Double c) {
        this.isDirty = true;
        this.fermentables.get(i).setCost(c);
    }

    public void setMaltPercent(final int i, final double p) {
        this.isDirty = true;
        this.fermentables.get(i).setPercent(p);
    }

    public void setMaltSteep(final int i, final boolean c) {
        this.isDirty = true;
        this.fermentables.get(i).setSteep(c);
    }

    public void setMaltMashed(final int i, final boolean c) {
        this.isDirty = true;
        this.fermentables.get(i).setMashed(c);
    }

    public void setMaltFerments(final int i, final boolean c) {
        this.isDirty = true;
        this.fermentables.get(i).ferments(c);
    }

    // misc get/set functions
    public int getMiscListSize() {
        return this.misc.size();
    }

    public Misc getMisc(final int i) {
        return this.misc.get(i);
    }

    public String getMiscName(final int i) {
        return this.misc.get(i).getName();
    }

    public void setMiscName(final int i, final String n) {
        this.isDirty = true;
        this.misc.get(i).setName(n);
    }

    public double getMiscAmount(final int i) {
        final Misc m = this.misc.get(i);
        return m.getAmountAs(m.getUnits());
    }

    public void setMiscAmount(final int i, final double a) {
        this.isDirty = true;
        this.misc.get(i).setAmount(a);
        calcMiscCost();
    }

    public String getMiscUnits(final int i) {
        return this.misc.get(i).getUnits();
    }

    public void setMiscUnits(final int i, final String u) {
        this.isDirty = true;
        this.misc.get(i).setUnits(u);
        calcMiscCost();
    }

    public double getMiscCost(final int i) {
        return this.misc.get(i).getCostPerU();
    }

    public void setMiscCost(final int i, final double c) {
        this.isDirty = true;
        this.misc.get(i).setCost(c);
        calcMiscCost();
    }

    public String getMiscStage(final int i) {
        return this.misc.get(i).getStage();
    }

    public void setMiscStage(final int i, final String s) {
        this.isDirty = true;
        this.misc.get(i).setStage(s);
    }

    public int getMiscTime(final int i) {
        return this.misc.get(i).getTime();
    }

    public void setMiscTime(final int i, final int t) {
        this.isDirty = true;
        this.misc.get(i).setTime(t);
    }

    public String getMiscDescription(final int i) {
        return this.misc.get(i).getDescription();
    }

    // Setters that need to do extra work:

    public void setMiscComments(final int i, final String c) {
        this.isDirty = true;
        this.misc.get(i).setComments(c);
    }

    public String getMiscComments(final int i) {
        return this.misc.get(i).getComments();
    }

    // notes get/set methods
    public Note getNote(int i) {
        return this.notes.get(i);
    }

    public int getNotesListSize() {
        return this.notes.size();
    }

    public Date getNoteDate(final int i) {
        return this.notes.get(i).getDate();
    }

    public void setNoteDate(final int i, final Date d) {
        this.isDirty = true;
        this.notes.get(i).setDate(d);
    }

    public String getNoteType(final int i) {
        return this.notes.get(i).getType();
    }

    public void setNoteType(final int i, final String t) {
        this.isDirty = true;
        if (i > -1) {
            this.notes.get(i).setType(t);
        }
    }

    public String getNoteNote(final int i) {
        return this.notes.get(i).getNote();
    }

    public void setNoteNote(final int i, final String n) {
        this.isDirty = true;
        this.notes.get(i).setNote(n);
    }

    public void setReadVolUnits(final String v) {
        this.isDirty = true;
        this.kettleLossVol.setUnits(v);
        this.postBoilVol.setUnits(v);
        this.trubLossVol.setUnits(v);
        this.miscLossVol.setUnits(v);
        calcMaltTotals();
        calcHopsTotals();
    }

    public void setPreBoil(final Quantity p) {
        this.isDirty = true;

        // The one-true-volume is postBoil.. so calc it, and set it
        if (this.equipmentProfile != null && this.equipmentProfile.isCalcBoilVol()) {
            Quantity post = new Quantity(getVolUnits(), p.getValueAs(getVolUnits()) - getEvapVol(getVolUnits())
                    - getChillShrinkVol(getVolUnits()));
            setPostBoil(post);
        } else {
            this.preBoilVol = p;
        }
    }

    public void setPostBoil(final Quantity p) {
        this.isDirty = true;

        // One-true vol, set it and all the getters will go from there
        // Hack alert; chop off "double ugglyness" rounding errors at umtenth
        // decimal place
        // this is causing recalc problems:
        // long hackNumber = (long)(p.getValue() * 100);
        // p.setAmount((double)hackNumber / 100);
        this.postBoilVol = p;

        // Recalc all the bits
        calcMaltTotals();
        calcHopsTotals();
        calcPrimeSugar();
        calcKegPSI();
    }

    public void setFinalWortVol(final Quantity p) {
        this.isDirty = true;

        // The one-true-volume is postBoil.. so calc it, and set it
        Quantity post = new Quantity(getVolUnits(), p.getValueAs(getVolUnits()) + getKettleLoss(getVolUnits())
                + getTrubLoss(getVolUnits()) + getMiscLoss(getVolUnits()));
        setPostBoil(post);
    }

    /*
     * Functions that add/remove from ingredient lists
     */
    public void addMalt(final Fermentable m) {
        this.isDirty = true;


        this.fermentables.add(m);
        calcMaltTotals();
    }

    public void delMalt(final int i) {
        this.isDirty = true;
        if (!this.fermentables.isEmpty() && (i > -1) && (i < this.fermentables.size())) {
            this.fermentables.remove(i);
            calcMaltTotals();
        }
    }

    public void addHop(final Hop h) {
        this.isDirty = true;
        this.hops.add(h);
        calcHopsTotals();
    }

    public void delHop(final int i) {
        this.isDirty = true;
        if (!this.hops.isEmpty() && (i > -1) && (i < this.hops.size())) {
            this.hops.remove(i);
            calcHopsTotals();
        }
    }

    public void addMisc(final Misc m) {
        this.isDirty = true;
        this.misc.add(m);
        calcMiscCost();
    }

    public void delMisc(final int i) {
        this.isDirty = true;
        if (!this.misc.isEmpty() && (i > -1) && (i < this.misc.size())) {
            this.misc.remove(i);
            calcMiscCost();
        }
    }

    private void calcMiscCost() {
        this.totalMiscCost = 0;
        for (final Misc m : this.misc) {
            this.totalMiscCost += m.getAmountAs(m.getUnits()) * m.getCostPerU();
        }
    }

    public void addNote(final Note n) {
        this.isDirty = true;
        this.notes.add(n);
    }

    public void delNote(final int i) {
        this.isDirty = true;
        if (!this.notes.isEmpty() && (i > -1) && (i < this.notes.size())) {
            this.notes.remove(i);
        }
    }

    /**
     * Handles a string of the form "d u", where d is a double amount, and u is
     * a string of units. For importing the quantity attribute from QBrew xml.
     *
     * @param a The Amount and unit string to parse.
     */
    public void setAmountAndUnits(final String a) {
        this.isDirty = true;
        final int i = a.indexOf(" ");
        final String d = a.substring(0, i);
        final String u = a.substring(i);

        Quantity post = new Quantity(u.trim(), Double.parseDouble(d.trim()));
        setPostBoil(post);
    }

    /**
     * Calculate all the malt totals from the array of malt objects TODO: Other
     * things to implement: - cost tracking - hopped malt extracts (IBUs) - the %
     * that this malt represents - error checking
     */

    // Calc functions.
    private void calcEfficiency() {
        double possiblePoints = 0;
        for (final Fermentable m : this.fermentables) {
            possiblePoints += (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }
        this.efficiency = (this.estOg - 1) / possiblePoints * 100;
    }

    public void calcMaltTotals() {

        if (!this.allowRecalcs) {
            return;
        }
        double maltPoints = 0;
        double fermentingMaltPoints = 0;
        this.totalMaltLbs = 0;
        this.totalMaltCost = 0;
        this.totalMashLbs = 0;

        double curPoints = 0.0;

        // first figure out the total we're dealing with
        for (final Fermentable m : this.fermentables) {
            if (m.getName().equals("") || m.getAmountAs(Quantity.LB) <= 0.00) {
                continue;
            }
            this.totalMaltLbs += (m.getAmountAs(Quantity.LB));
            if (m.getMashed()) { // apply efficiency and add to mash weight
                curPoints = (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) * getEfficiency()
                        / getPostBoilVol(Quantity.GAL);
                this.totalMashLbs += (m.getAmountAs(Quantity.LB));
            } else {
                curPoints += (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) * 100 / getPostBoilVol(Quantity.GAL);
            }

            maltPoints += curPoints;

            // Check to see if we can ferment this sugar
            if (m.ferments()) {
                fermentingMaltPoints += curPoints;
            }
        }

        // Now calculate the percentages
        for (Fermentable m : this.fermentables) {
            // Malt % By Weight
            if (m.getAmountAs(Quantity.LB) == 0) {
                m.setPercent(0);
            } else {
                m.setPercent((m.getAmountAs(Quantity.LB) / this.totalMaltLbs * 100));
            }

            this.totalMaltCost += m.getCostPerU() * m.getAmountAs(m.getUnits());
        }

        // set the fields in the object
        this.estOg = (maltPoints / 100) + 1;
        double estFermOg = (fermentingMaltPoints / 100) + 1;

        double attGrav = (estFermOg - 1) * (getAttenuation() / 100);

        // FG
        this.estFg = this.estOg - attGrav;
        // mash.setMaltWeight(totalMashLbs);

        Collections.sort(this.fermentables, this.fermCompare);
        Collections.reverse(this.fermentables);
    }

    public void calcHopsTotals() {

        if (!this.allowRecalcs) {
            return;
        }
        Collections.sort(this.hops);
        double ibuTotal = 0;
        this.totalHopsCost = 0;
        this.totalHopsOz = 0;

        for (Hop hop : this.hops) {
            // calculate the average OG of the boil
            // first, the OG at the time of addition:
            double adjPreSize, aveOg;

            int time = hop.getMinutes();
            if (hop.getAdd().equalsIgnoreCase(Hop.FWH)) {
                time = time - this.fwhTime;
            } else if (hop.getAdd().equalsIgnoreCase(Hop.MASH)) {
                time = this.mashHopTime;
            } else if (hop.getAdd().equalsIgnoreCase(Hop.DRY)) {
                time = this.dryHopTime;
            }

            if (hop.getMinutes() > 0) {
                adjPreSize = getPostBoilVol(Quantity.GAL)
                        + (getPreBoilVol(Quantity.GAL) - getPostBoilVol(Quantity.GAL))
                        / (getBoilMinutes() / hop.getMinutes());
            } else {
                adjPreSize = getPostBoilVol(Quantity.GAL);
            }

            aveOg = 1 + (((this.estOg - 1) + ((this.estOg - 1) / (adjPreSize / getPostBoilVol(Quantity.GAL)))) / 2);
            this.logger.warn(String.format("IBU Util: %.2f, OG: %.3f, adjPreSize: %.2f", this.ibuHopUtil, aveOg, adjPreSize));
            switch (this.ibuCalcMethod) {
                case BrewCalcs.TINSETH:
                    hop.setIBU(BrewCalcs.calcTinseth(hop.getAmountAs(Quantity.OZ), getPostBoilVol(Quantity.GAL), aveOg,
                            time, hop.getAlpha()));
                    break;
                case BrewCalcs.RAGER:
                    hop.setIBU(BrewCalcs.CalcRager(hop.getAmountAs(Quantity.OZ), getPostBoilVol(Quantity.GAL), aveOg,
                            time, hop.getAlpha()));
                    break;
                default:
                    hop.setIBU(BrewCalcs.CalcGaretz(hop.getAmountAs(Quantity.OZ), getPostBoilVol(Quantity.GAL), aveOg, time,
                            getPreBoilVol(Quantity.GAL), 1, hop.getAlpha()));
                    break;
            }
            this.logger.warn("Precalc: {}", hop.getIBU());
            if (hop.getType().equalsIgnoreCase(Hop.PELLET)) {
                hop.setIBU(hop.getIBU() * (1.0 + (getPelletHopPct() / 100)));
            }
            this.logger.warn("Postcalc: {}", hop.getIBU());

            ibuTotal += hop.getIBU();

            this.totalHopsCost += hop.getCostPerU() * hop.getAmountAs(hop.getUnits());
            this.totalHopsOz += hop.getAmountAs(Quantity.OZ);
        }

        this.ibu = ibuTotal;

    }

    private String addXMLHeader(String in) {
        in = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
                + "<?xml-stylesheet type=\"text/xsl\" href=\"http://strangebrewcloud.appspot.com/html/recipeToHtml.xslt\"?>"
                + in;
        return in;
    }

    public String toText() {
        return toText(false);
    }

    public String toText(boolean detailed) {
        MessageFormat mf;
        final StringBuilder sb = new StringBuilder();
        sb.append("StrangeBrew J v.").append(this.version).append(" recipe text output\n\n");
        sb.append("Details:\n");
        sb.append("Name: ").append(this.name).append("\n");
        sb.append("Brewer: ").append(this.brewer).append("\n");
        sb.append("Size: ").append(SBStringUtils.format(getPostBoilVol(getVolUnits()), 1)).append(" ").append(getVolUnits()).append("\n");
        sb.append("Style: ").append(this.style.getName()).append("\n");
        mf = new MessageFormat(
                "OG: {0,number,0.000},\tFG:{1,number,0.000}, \tAlc:{2,number,0.0}, \tIBU:{3,number,0.0}\n");
        final Object[] objs = {this.estOg, this.estFg, getAlcohol(), this.ibu};
        sb.append(mf.format(objs));
        sb.append("(Alc method: by ").append(getAlcMethod()).append("; IBU method: ").append(this.ibuCalcMethod).append(")\n");
        if (this.yeasts.size() == 1) {
            sb.append("\nYeast: ");
        } else {
            sb.append("\nYeasts: ");
        }
        for (Yeast yeast : this.yeasts) {
            sb.append(yeast.getName()).append("\n");
        }
        sb.append("\nFermentables:\n");
        sb.append(Recipe.padLeft("Name ", 30, ' ')).append(" amount units  pppg    lov   %\n");

        mf = new MessageFormat("{0} {1} {2} {3,number,0.000} {4} {5}%\n");
        for (Fermentable fermentable : this.fermentables) {

            final Object[] objf = {Recipe.padLeft(fermentable.getName(), 30, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(fermentable.getAmountAs(fermentable.getUnits()), 2), 6, ' '),
                    Recipe.padRight(" " + fermentable.getUnitsAbrv(), 5, ' '), fermentable.getPppg(),
                    Recipe.padRight(" " + SBStringUtils.format(fermentable.getLov(), 1), 6, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(fermentable.getPercent(), 1), 5, ' ')};
            sb.append(mf.format(objf));

        }

        sb.append("\nHops:\n");
        sb.append(Recipe.padLeft("Name ", 20, ' ')).append(" amount units  Alpha    Min   IBU\n");

        mf = new MessageFormat("{0} {1} {2} {3} {4} {5}\n");
        for (final Hop h : this.hops) {
            final Object[] objh = {Recipe.padLeft(h.getName() + " (" + h.getType() + ")", 20, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getAmountAs(h.getUnits()), 2), 6, ' '),
                    Recipe.padRight(" " + h.getUnitsAbrv(), 5, ' '), Recipe.padRight(" " + h.getAlpha(), 6, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getMinutes(), 1), 6, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getIBU(), 1), 5, ' ')};
            sb.append(mf.format(objh));

        }

        if (this.mash.getStepSize() > 0) {
            sb.append("\nMash:\n");
            sb.append(Recipe.padLeft("Step ", 10, ' ')).append("  TempProbe   End    Ramp    Min	Input	Output	Water TempProbe\n");

            mf = new MessageFormat("{0} {1} {2} {3} {4} {5} {6} {7}\n");
            for (int i = 0; i < this.mash.getStepSize(); i++) {

                final Object[] objm = {Recipe.padLeft(this.mash.getStepType(i), 10, ' '),
                        Recipe.padRight(" " + this.mash.getStepStartTemp(i), 6, ' '),
                        Recipe.padRight(" " + this.mash.getStepEndTemp(i), 6, ' '),
                        Recipe.padRight(" " + this.mash.getStepRampMin(i), 4, ' '),
                        Recipe.padRight(" " + this.mash.getStepMin(i), 6, ' '),
                        Recipe.padRight(" " + SBStringUtils.format(this.mash.getStepInVol(i), 2), 6, ' '),
                        Recipe.padRight(" " + SBStringUtils.format(this.mash.getStepOutVol(i), 2), 6, ' '),
                        Recipe.padRight(" " + this.mash.getStepTemp(i), 6, ' ')};

                sb.append(mf.format(objm));
            }
        }

        if (this.notes.size() > 0) {
            sb.append("\nNotes:\n");
            for (Note note : this.notes) {
                sb.append(note.toString());
            }
        }

        // only print this stuff for detailed text output:

        if (detailed) {
            // Fermentation Schedule
            if (this.fermentationSteps.size() > 0) {
                sb.append("\nFermentation Schedule:\n");
                sb.append(Recipe.padLeft("Step ", 10, ' ')).append("  Time   Days\n");
                mf = new MessageFormat("{0} {1} {2}\n");
                for (final FermentStep f : this.fermentationSteps) {
                    final Object[] objm = {Recipe.padLeft(f.getType(), 10, ' '),
                            Recipe.padRight(" " + f.getTime(), 6, ' '),
                            Recipe.padRight(" " + f.getTemp() + f.getTempU(), 6, ' ')};
                    sb.append(mf.format(objm));
                }
            }

            // Carb
            sb.append("\nCarbonation:  ").append(this.targetVol).append(" volumes CO2\n");
            sb.append(" Bottle TempProbe: ").append(this.bottleTemp).append(this.carbTempU).append("  Serving TempProbe:").append(this.servTemp).append(this.carbTempU).append("\n");
            sb.append(" Priming: ").append(SBStringUtils.format(this.primeSugar.getAmountAs(this.primeSugar.getUnits()), 1)).append(this.primeSugar.getUnitsAbrv()).append(" of ").append(this.primeSugar.getName()).append("\n");
            sb.append(" Or keg at: ").append(this.kegPSI).append("PSI\n");

            if ((!Objects.equals(this.sourceWater.getName(), "")) || (!Objects.equals(this.targetWater.getName(), ""))) {
                sb.append("\nWater Profile\n");
                sb.append(" Source Water: ").append(this.sourceWater.toString()).append("\n");
                sb.append(" Target Water: ").append(this.targetWater.toString()).append("\n");

                if (this.brewingSalts.size() > 0) {
                    sb.append(" Salt Additions per Gal\n");
                    for (Salt brewingSalt : this.brewingSalts) {
                        sb.append("  ").append(brewingSalt.toString()).append("\n");
                    }
                }
                sb.append(" Acid: ").append(SBStringUtils.format(getAcidAmount(), 2)).append(this.acid.getAcidUnit()).append(" per gal of ").append(this.acid.getName()).append(" Acid\n");
            }
        }

        return sb.toString();
    }

    /*
     * Scale the recipe up or down, so that the new OG = old OG, and new IBU =
     * old IBU
     */
    public void scaleRecipe(final Quantity newSize) {
        final double currentSize = getPostBoilVol(newSize.getUnits());
        final double conversionFactor = newSize.getValue() / currentSize;

        if (conversionFactor != 1) {
            // TODO: figure out a way to make sure old IBU = new IBU
            for (int i = 0; i < getHopsListSize(); i++) {
                final Hop h = getHop(i);
                h.setAmount(h.getAmountAs(h.getUnits()) * conversionFactor);
            }
            for (int i = 0; i < getMaltListSize(); i++) {
                final Fermentable f = getFermentable(i);
                f.setAmount(f.getAmountAs(f.getUnits()) * conversionFactor);
            }
            setPostBoil(newSize);
            setVolUnits(newSize.getUnits());
        }
    }

    public double getBottleTemp() {
        return this.bottleTemp;
    }

    public void setBottleTemp(final double bottleTemp) {
        this.isDirty = true;
        this.bottleTemp = bottleTemp;
        calcPrimeSugar();
    }

    public String getCarbTempU() {
        return this.carbTempU;
    }

    public void setCarbTempU(final String carbU) {
        this.isDirty = true;
        this.carbTempU = carbU;
    }

    public boolean isKegged() {
        return this.kegged;
    }

    public void setKegged(final boolean kegged) {
        this.isDirty = true;
        this.kegged = kegged;
        calcKegPSI();
    }

    public double getKegPSI() {
        return this.kegPSI;
    }

    public void setKegPSI(final double psi) {
        this.isDirty = true;
        this.kegPSI = psi;
    }

    public double getKegTubeLength() {
        double resistance;
        if (getKegTubeID().equals("3/16")) {
            resistance = 2.4;
        } else {
            resistance = 0.7;
        }
        return (getKegPSI() - (getKegTubeHeight() * 0.5) - 1) / resistance;
    }

    public double getKegTubeVol() {
        double mlPerFoot;
        if (getKegTubeID().equals("3/16")) {
            mlPerFoot = 4.9;
        } else {
            mlPerFoot = 9.9;
        }

        return getKegTubeLength() * mlPerFoot;
    }

    public double getKegTubeHeight() {
        return 0.0;
    }

    public String getKegTubeID() {
        return "";
    }

    public String getPrimeSugarName() {
        return this.primeSugar.getName();
    }

    public void setPrimeSugarName(final String n) {
        this.isDirty = true;
        this.primeSugar.setName(n);
    }

    public String getPrimeSugarU() {
        return this.primeSugar.getUnitsAbrv();
    }

    public void setPrimeSugarU(final String primeU) {
        this.isDirty = true;
        this.primeSugar.setUnits(primeU);
        calcPrimeSugar();
    }

    public double getPrimeSugarAmt() {
        return this.primeSugar.getAmountAs(this.primeSugar.getUnitsAbrv());
    }

    public void calcPrimeSugar() {
        if (!this.allowRecalcs) {
            return;
        }
        final double dissolvedCO2 = BrewCalcs.dissolvedCO2(getBottleTemp());
        final double primeSugarGL = BrewCalcs.PrimingSugarGL(dissolvedCO2, getTargetVol(), getPrimeSugar());

        // Convert to selected Units
        double neededPrime = Quantity.convertUnit(Quantity.G, getPrimeSugarU(), primeSugarGL);
        neededPrime *= Quantity.convertUnit(Quantity.L, getVolUnits(), primeSugarGL);
        neededPrime *= getFinalWortVol(getVolUnits());

        this.primeSugar.setAmount(neededPrime);
    }

    public void calcKegPSI() {
        if (!this.allowRecalcs) {
            return;
        }
        this.kegPSI = BrewCalcs.KegPSI(this.servTemp, getTargetVol());
    }

    public void setPrimeSugarYield(final double yield) {
        this.isDirty = true;
        this.primeSugar.setYield(yield);
    }

    public void setPrimeSugarAmount(final double q) {
        this.isDirty = true;
        this.primeSugar.setAmount(q);
    }

    public double getServTemp() {
        return this.servTemp;
    }

    public void setServTemp(final double servTemp) {
        this.isDirty = true;
        this.servTemp = servTemp;
        calcKegPSI();
    }

    public double getTargetVol() {
        return this.targetVol;
    }

    public void setTargetVol(final double targetVol) {
        this.isDirty = true;
        this.targetVol = targetVol;
        calcPrimeSugar();
        calcKegPSI();
    }

    public PrimeSugar getPrimeSugar() {
        return this.primeSugar;
    }

    public void setPrimeSugar(final PrimeSugar primeSugar) {
        this.primeSugar = primeSugar;
        calcPrimeSugar();
    }

    public WaterProfile getSourceWater() {
        return this.sourceWater;
    }

    public void setSourceWater(final WaterProfile sourceWater) {
        this.sourceWater = sourceWater;
    }

    public WaterProfile getTargetWater() {
        return this.targetWater;
    }

    public void setTargetWater(final WaterProfile targetWater) {
        this.targetWater = targetWater;
    }

    public WaterProfile getWaterDifference() {
        final WaterProfile diff = new WaterProfile();
        diff.setCa(getTargetWater().getCa() - getSourceWater().getCa());
        diff.setCl(getTargetWater().getCl() - getSourceWater().getCl());
        diff.setMg(getTargetWater().getMg() - getSourceWater().getMg());
        diff.setNa(getTargetWater().getNa() - getSourceWater().getNa());
        diff.setSo4(getTargetWater().getSo4() - getSourceWater().getSo4());
        diff.setHco3(getTargetWater().getHco3() - getSourceWater().getHco3());
        diff.setHardness(getTargetWater().getHardness() - getSourceWater().getHardness());
        diff.setAlkalinity(getTargetWater().getAlkalinity() - getSourceWater().getAlkalinity());
        diff.setTds(getTargetWater().getTds() - getSourceWater().getTds());
        diff.setPh(getTargetWater().getPh() - getSourceWater().getPh());

        return diff;
    }

    public void addSalt(final Salt s) {
        // Check if ths salt already exists!
        final Salt temp = getSaltByName(s.getName());
        if (temp != null) {
            this.delSalt(temp);
        }
        this.brewingSalts.add(s);
    }

    public void delSalt(final Salt s) {
        this.brewingSalts.remove(s);
    }

    public void delSalt(final int i) {
        if (!this.brewingSalts.isEmpty() && (i > -1) && (i < this.brewingSalts.size())) {
            this.brewingSalts.remove(i);
        }
    }

    public List<Salt> getSalts() {
        return this.brewingSalts;
    }

    public void setSalts(final ArrayList<Salt> s) {
        this.brewingSalts = s;
    }

    public Salt getSalt(final int i) {
        return this.brewingSalts.get(i);
    }

    public Salt getSaltByName(final String name) {
        for (final Salt s : this.brewingSalts) {
            if (s.getName().equals(name)) {
                return s;
            }
        }

        return null;
    }

    public Acid getAcid() {
        return this.acid;
    }

    public void setAcid(final Acid acid) {
        this.acid = acid;
    }

    public double getAcidAmount() {
        final double millEs = BrewCalcs.acidMillequivelantsPerLiter(getSourceWater().getPh(), getSourceWater()
                .getAlkalinity(), getTargetWater().getPh());
        final double moles = BrewCalcs.molesByAcid(getAcid(), millEs, getTargetWater().getPh());
        final double acidPerL = BrewCalcs.acidAmountPerL(getAcid(), moles);
        return Quantity.convertUnit(Quantity.GAL, Quantity.L, acidPerL);
    }

    public double getAcidAmount5_2() {
        final double millEs = BrewCalcs.acidMillequivelantsPerLiter(getSourceWater().getPh(), getSourceWater()
                .getAlkalinity(), 5.2);
        final double moles = BrewCalcs.molesByAcid(getAcid(), millEs, 5.2);
        final double acidPerL = BrewCalcs.acidAmountPerL(getAcid(), moles);
        return Quantity.convertUnit(Quantity.GAL, Quantity.L, acidPerL);
    }

    public List<Yeast> getYeasts() {
        return this.yeasts;
    }

    /**
     * Identify the type of this recipe.
     *
     * @return ALL GRAIN, PARTIAL MASH, or EXTRACT or UNKNOWN.
     */
    public final String getType() {
        int mashedCount = 0;

        for (Fermentable f : this.fermentables) {
            if (f.getMashed()) {
                mashedCount++;
            }
        }

        if (mashedCount == this.fermentables.size()) {
            return "ALL GRAIN";
        } else if (mashedCount > 0) {
            return "PARTIAL MASH";
        } else if (mashedCount == 0) {
            return "EXTRACT";
        }

        return "UNKNOWN";
    }

    public Equipment getEquipmentProfile() {
        return this.equipmentProfile;
    }

    public void setEquipmentProfile(Equipment equipmentProfile) {
        this.equipmentProfile = equipmentProfile;
    }

    public String getTasteNotes() {
        return this.tasteNotes;
    }

    public void setTasteNotes(String tasteNotes) {
        this.tasteNotes = tasteNotes;
    }

    public double getTasteRating() {
        return this.tasteRating;
    }

    public void setTasteRating(double tasteRating) {
        this.tasteRating = tasteRating;
    }

    public double getMeasuredOg() {
        return this.measuredOg;
    }

    public void setMeasuredOg(double measuredOg) {
        this.measuredOg = measuredOg;
    }

    public double getMeasuredFg() {
        return this.measuredFg;
    }

    public void setMeasuredFg(double measuredFg) {
        this.measuredFg = measuredFg;
    }

    public String getDateBrewed() {
        return this.dateBrewed;
    }

    public void setDateBrewed(String dateBrewed) {
        this.dateBrewed = dateBrewed;
    }

    public double getPrimeSugarEquiv() {
        return this.primeSugarEquiv;
    }

    public void setPrimeSugarEquiv(double primeSugarEquiv) {
        this.primeSugarEquiv = primeSugarEquiv;
    }

    public double getKegPrimingFactor() {
        return this.kegPrimingFactor;
    }

    public void setKegPrimingFactor(double kegPrimingFactor) {
        this.kegPrimingFactor = kegPrimingFactor;
    }

    public double getMeasuredAlcohol() {
        return BrewCalcs.calcAlcohol(getAlcMethod(), this.measuredOg, this.measuredFg);
    }

    public double getMeasuredEfficiency() {
        double possiblePoints = 0;
        for (final Fermentable m : this.fermentables) {
            possiblePoints += (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }
        return (this.measuredOg - 1) / possiblePoints * 100;
    }

    public String calcCalories() {
        double alcCal = 1881.22 * this.measuredFg * (this.measuredOg - this.measuredFg) / (1.775 - this.measuredOg);
        double carbCal = 3550.0 * this.measuredFg * ((0.1808 * this.measuredOg) + (0.8192 * this.measuredFg) - 1.0004);

        double totalCal = alcCal + carbCal;
        return Double.toString(totalCal) + "Cal/12oz";
    }

    public void setGravUnit(String newUnit) {
        this.gravUnits = newUnit;
    }

    public String getGravUnits() {
        return this.gravUnits;
    }

    public void setPostBoil(String display_batch_size) {
        if (display_batch_size == null || display_batch_size.equals("")) {
            return;
        }

        this.postBoilVol = new Quantity(display_batch_size);
    }

    public void setPreBoil(String preBoilString) {
        if (preBoilString == null || preBoilString.equals("")) {
            return;
        }
        Quantity p = new Quantity(preBoilString);
        // The one-true-volume is postBoil.. so calc it, and set it
        if (this.equipmentProfile != null && this.equipmentProfile.isCalcBoilVol()) {
            Quantity post = new Quantity(getVolUnits(), p.getValueAs(getVolUnits()) - getEvapVol(getVolUnits())
                    - getChillShrinkVol(getVolUnits()));
            setPostBoil(post);
        } else {
            this.preBoilVol = p;
        }
    }

    public String getCarbMethod() {
        return this.carbMethod;
    }

    public void setCarbMethod(String carbMethod) {
        this.carbMethod = carbMethod;
    }

    public void setMashProfile(TempProbe tempProbeProbe) {
        TriggerControl triggerControl = tempProbeProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        for (int i = 0; i < this.mash.getStepSize(); i++) {
            TemperatureTrigger temperatureTrigger = new TemperatureTrigger(i * 2, tempProbeProbe.getName(),
                    this.mash.getStepStartTemp(i), this.mash.getStepType(i), this.mash.getStepMethod(i));
            temperatureTrigger.setExitTemp(this.mash.getStepEndTemp(i));
            triggerControl.addTrigger(temperatureTrigger);

            WaitTrigger waitTrigger = new WaitTrigger((i * 2) + 1, this.mash.getStepMin(i), 0);
            waitTrigger.setNote(String.format(Messages.HOLD_STEP, String.format("%.2f", this.mash.getStepEndTemp(i)) + this.mash.getStepTempU(i)));
            triggerControl.addTrigger(waitTrigger);
        }
    }

    public void setBoilHops(TempProbe tempProbeProbe) {
        // Base this from the time different between steps. When they press activate the first step goes immediately (just annotate)
        TriggerControl triggerControl = tempProbeProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int j = 0;
        Hop prevHop = null;
        for (Hop h : this.hops) {
            // Skip to the next hop if it's not a boil hop
            if (!h.getAdd().equals(Hop.BOIL)) {
                continue;
            }

            double mins;
            if (prevHop != null) {
                mins = prevHop.getMinutes() - h.getMinutes();
            } else {
                mins = this.boilMinutes - h.getMinutes();
            }

            WaitTrigger waitTrigger = new WaitTrigger(j, mins, 0);
            waitTrigger.setNote(String.format(Messages.ADD_HOP, h.getAmount().toString(), h.getName()));
            triggerControl.addTrigger(waitTrigger);
            j++;
            prevHop = h;
        }
    }

    public void setFermProfile(TempProbe tempProbeProbe) {
        TriggerControl triggerControl = tempProbeProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int i = 0;
        for (FermentStep fermentStep : this.fermentationSteps) {
            TemperatureTrigger temperatureTrigger = new TemperatureTrigger(i * 2, tempProbeProbe.getName(),
                    fermentStep.getTemp(), fermentStep.getType(), "");
            triggerControl.addTrigger(temperatureTrigger);

            i++;
            WaitTrigger waitTrigger = new WaitTrigger(i, fermentStep.getTimeMins(), 0);
            waitTrigger.setNote(String.format(Messages.HOLD_STEP, String.format("%.2f", fermentStep.getTemp()) + fermentStep.getTempU()));
            triggerControl.addTrigger(waitTrigger);
            i++;
        }
    }

    public void setDryHops(TempProbe tempProbeProbe) {
        // Base this from the time different between steps. When they press activate the first step goes immediately (just annotate)
        TriggerControl triggerControl = tempProbeProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int j = 0;
        Hop prevHop = null;
        for (Hop h : this.hops) {
            // Skip to the next hop if it's not a boil hop
            if (!h.getAdd().equals(Hop.DRY)) {
                continue;
            }

            double mins;
            if (prevHop != null) {
                mins = prevHop.getMinutes() - h.getMinutes();
            } else {
                mins = h.getMinutes();
            }

            WaitTrigger waitTrigger = new WaitTrigger(j, mins, 0);
            waitTrigger.setNote(String.format(Messages.ADD_HOP, h.getAmount().toString(), h.getName()));
            triggerControl.addTrigger(waitTrigger);
            j++;
            prevHop = h;
        }
    }
}