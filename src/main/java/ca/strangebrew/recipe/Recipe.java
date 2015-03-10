/*
 * $Id: Recipe.java,v 1.74 2012/06/03 19:29:17 dougedey Exp $
 * Created on Oct 4, 2004 @author aavis recipe class
 */

/**
 *  StrangeBrew Java - a homebrew recipe calculator
 Copyright (C) 2005  Drew Avis

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;
import com.sb.elsinore.BrewServer;
import com.sb.elsinore.Messages;
import com.sb.elsinore.Temp;
import com.sb.elsinore.TriggerControl;
import com.sb.elsinore.triggers.TemperatureTrigger;
import com.sb.elsinore.triggers.WaitTrigger;

import java.text.MessageFormat;
import java.util.*;

@SuppressWarnings("unused")
public class Recipe {

	// basics:
	private String version = "";

	private boolean isDirty = false;
	public boolean allowRecalcs = true;
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
	public Mash mash = null;

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
	private String carbTempU  = "";
	private boolean kegged = false;
	private double kegPSI = 0.0;

	// options:
	private String colourMethod  = "";
	private String hopUnits  = "";
	private String maltUnits  = "";
	private String ibuCalcMethod  = "";
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

	// Getters:
	public double getAlcohol() {
		return BrewCalcs.calcAlcohol(getAlcMethod(), estOg, getEstFg());
	}

	public String getAlcMethod() {
		return alcMethod;
	}

	public double getAttenuation() {
		if (yeasts.size() > 0 && yeasts.get(0).getAttenuation() > 0.0) {
            return yeasts.get(0).getAttenuation();
        }
        return attenuation;
	}

	public int getBoilMinutes() {
		return boilMinutes;
	}

	public String getBottleU() {
		return bottleU;
	}

	public double getBottleSize() {
		return bottleSize;
	}

	public double getBUGU() {
		double bugu = 0.0;
		if (estOg != 1.0) {
			bugu = ibu / ((estOg - 1) * 1000);
		}
		return bugu;
	}

	public String getBrewer() {
		return brewer;
	}

	public String getComments() {
		return comments;
	}

	public double getColour() {
		return getColour(getColourMethod());
	}

	public double getMcu() {
		double mcu = 0;
        for (final Fermentable m : fermentables) {
            mcu += m.getLov() * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }

		return mcu;
	}

	public double getColour(final String method) {
		return BrewCalcs.calcColour(getMcu(), method);
	}

	public String getColourMethod() {
		return colourMethod;
	}

	public GregorianCalendar getCreated() {
		return created;
	}

	public double getEfficiency() {
		return efficiency;
	}

	public double getEstOg() {
		return estOg;
	}

	public double getEstFg() {
		return estFg;
	}

	public double getEvap() {
		return this.equipmentProfile.getEvapRate();
	}

	public String getEvapMethod() {
		return evapMethod;
	}

	public Mash getMash() {
		return mash;
	}

	public double getIbu() {
		return ibu;
	}

	public String getIBUMethod() {
		return ibuCalcMethod;
	}

	public String getMaltUnits() {
		return maltUnits;
	}

	public String getName() {
		return name;
	}

	public double getOtherCost() {
		return otherCost;
	}

	public double getPelletHopPct() {
		return pelletHopPct;
	}

	// Water getters - the calculated version
	public double getKettleLoss(final String s) {
		return kettleLossVol.getValueAs(s);
	}

	public double getMiscLoss(final String s) {
		return miscLossVol.getValueAs(s);
	}

	public double getTotalWaterVol(final String s) {
		Quantity q = new Quantity(Quantity.QT, mash.getTotalWaterQts() + mash.getSpargeVol());
		return q.getValueAs(s);
	}

	public double getChillShrinkVol(final String s) {
        if (equipmentProfile != null) {
            return getPostBoilVol(s) * equipmentProfile.getChillPercent();
        }
        double CHILLPERCENT = 0.03;
        return getPostBoilVol(s) * CHILLPERCENT;
	}

	public double getPreBoilVol(final String s) {
        if (this.equipmentProfile != null && this.equipmentProfile.isCalcBoilVol()) {
            return getPostBoilVol(s)
                    + getEvapVol(s)
                    + getChillShrinkVol(s)
                    + equipmentProfile.getTopupKettle()
                    + equipmentProfile.getTrubChillerLoss().getValueAs(s)
                    + equipmentProfile.getLauterDeadspace().getValueAs(s);
        }

        return this.preBoilVol.getValueAs(s);
	}

	public double getFinalWortVol(final String s) {
		return getPostBoilVol(s) - (getKettleLoss(s) + getTrubLoss(s) + getMiscLoss(s));
	}

	public Quantity getPostBoilVol() {
		return postBoilVol;
	}
	
	public double getPostBoilVol(final String s) {
		return postBoilVol.getValueAs(s);
	}

	public double getTrubLoss(final String s) {
		return trubLossVol.getValueAs(s);
	}

	public double getEvapVol(final String s) {
		// JvH changing the boiltime, changes the post boil volume (NOT the pre
		// boil)
		double e;
		if (evapMethod.equals("Constant")) {
			e = getEvap() * getBoilMinutes() / 60;
			
			Quantity tVol = new Quantity(getVolUnits(), e);
			return tVol.getValueAs(s);
		} else { // %
			e = (getPostBoilVol(s)//  - equipmentProfile.getTopupKettle()
                    - equipmentProfile.getTrubChillerLoss().getValueAs(s)
                    - equipmentProfile.getLauterDeadspace().getValueAs(s))
                    * (getEvap() / 100) * getBoilMinutes() / 60;
		}
		return e;
	}

	public String getVolUnits() {
		if(postBoilVol != null){
			return postBoilVol.getUnits();
		} 
		return "";
	}

	public double getSparge() {
		// return (getVolConverted(spargeQTS));
		return mash.getSpargeVol();
	}

	public String getStyle() {
		return style.getName();
	}

	public Style getStyleObj() {
		return style;
	}

	public double getTotalHopsOz() {
		return totalHopsOz;
	}

	public double getTotalHops() {
		return Quantity.convertUnit(Quantity.OZ, hopUnits, totalHopsOz);
	}

	public double getTotalHopsCost() {
		return totalHopsCost;
	}

	public double getTotalMaltCost() {
		return totalMaltCost;
	}

	public double getTotalMashLbs() {
		return totalMashLbs;
	}

	public double getTotalMash() {
		return Quantity.convertUnit(Quantity.LB, getMaltUnits(), totalMashLbs);
	}

	public double getTotalMaltLbs() {
		return totalMaltLbs;
	}

	public double getTotalMalt() {
		return Quantity.convertUnit(Quantity.LB, maltUnits, totalMaltLbs);
	}

	public double getTotalMiscCost() {
		return totalMiscCost;
	}

	public String getYeast(int i) {
		return yeasts.get(i).getName();
	}

	public Yeast getYeastObj(int i) {
		return yeasts.get(i);
	}

	public boolean getDirty() {
		return isDirty;
	}

	// Setters:

	// Set saved flag
	public void setDirty(final boolean d) {
		isDirty = d;
	}

	/*
	 * Turn off allowRecalcs when you are importing a recipe, so that strange
	 * things don't happen. BE SURE TO TURN BACK ON!
	 */
	public void setAllowRecalcs(final boolean b) {
		allowRecalcs = b;
	}

	public void setAlcMethod(final String s) {
		isDirty = true;
		alcMethod = s;
	}

	public void setBoilMinutes(final int b) {
		isDirty = true;
		boilMinutes = b;
	}

	public void setBottleSize(final double b) {
		isDirty = true;
		bottleSize = b;
	}

	public void setBottleU(final String u) {
		isDirty = true;
		bottleU = u;
	}

	public void setBrewer(final String b) {
		isDirty = true;
		brewer = b;
	}

	public void setComments(final String c) {
		isDirty = true;
		comments = c;
	}

	public void setColourMethod(final String c) {
		isDirty = true;
		colourMethod = c;
		calcMaltTotals();
	}

	public void setCreated(final Date d) {
		isDirty = true;
		created.setTime(d);
	}

	public void setEvap(final double e) {
		isDirty = true;
        this.equipmentProfile.setEvapRate(e);
		calcMaltTotals();
		calcHopsTotals();
	}

	public void setEvapMethod(final String e) {
		isDirty = true;
		evapMethod = e;
	}

	public void setHopsUnits(final String h) {
		isDirty = true;
		hopUnits = h;
	}

	public void setIBUMethod(final String s) {
        if (s == null || s.equals("")) {
            return;
        }
		isDirty = true;
		ibuCalcMethod = s;
		calcHopsTotals();
	}

	public void setKettleLoss(final Quantity q) {
		isDirty = true;
		kettleLossVol = q;
		calcMaltTotals();
	}

	public void setMaltUnits(final String m) {
		isDirty = true;
		maltUnits = m;
	}

	public void setMashed(final boolean m) {
		isDirty = true;
		mashed = m;
	}

	public void setMashRatio(final double m) {
		isDirty = true;
		mash.setMashRatio(m);
	}

	public void setMashRatioU(final String u) {
		isDirty = true;
		mash.setMashRatioU(u);
	}

	public void setMiscLoss(final Quantity m) {
		isDirty = true;
		miscLossVol = m;
		calcMaltTotals();
	}

	public void setName(final String n) {
		isDirty = true;
		name = n;
	}

	public void setOtherCost(final double c) {
		isDirty = true;
		otherCost = c;
	}

	public void setPelletHopPct(final double p) {
		isDirty = true;
		pelletHopPct = p;
		calcHopsTotals();
	}

	public void setStyle(final String s) {
		isDirty = true;
		style.setName(s);
	}

	public void setStyle(final Style s) {
		isDirty = true;
		style = s;
		style.setComplete();
	}

	public void setTrubLoss(final Quantity t) {
		isDirty = true;
		trubLossVol = t;
		calcMaltTotals();
	}

	public void setYeastName(final int i, final String s) {
		isDirty = true;
		yeasts.get(i).setName(s);
	}

	public void setYeast(final int i, final Yeast y) {
		isDirty = true;
		yeasts.add(i, y);
	}

	public void setVersion(final String v) {
		isDirty = true;
		version = v;
	}

	// Fermentation Steps
	// Getters
	public int getFermentStepSize() {
		return fermentationSteps.size();
	}

	public String getFermentStepType(final int i) {
		return fermentationSteps.get(i).getType();
	}

	public int getFermentStepTime(final int i) {
		return fermentationSteps.get(i).getTime();
	}

    public int getFermentStepTimeMins(final int i) {
        return fermentationSteps.get(i).getTime()*24*60;
    }

	public double getFermentStepTemp(final int i) {
		return fermentationSteps.get(i).getTemp();
	}

	public String getFermentStepTempU(final int i) {
		return fermentationSteps.get(i).getTempU();
	}

	public FermentStep getFermentStep(final int i) {
		return fermentationSteps.get(i);
	}

    public FermentStep getFermentStep(final String stepType) {
        for(FermentStep fs: fermentationSteps) {
            if (fs.getType().equals(stepType)) {
                return fs;
            }
        }

        return new FermentStep();
    }

	public int getTotalFermentTime() {
		return totalFermentTime;
	}

	// Setters
	public void setFermentStepType(final int i, final String s) {
		isDirty = true;
		fermentationSteps.get(i).setType(s);
	}

	public void setFermentStepTime(final int i, final int t) {
		isDirty = true;
		fermentationSteps.get(i).setTime(t);
	}

	public void setFermentStepTemp(final int i, final double d) {
		isDirty = true;
		fermentationSteps.get(i).setTemp(d);
	}

	public void setFermentStepTempU(final int i, final String s) {
		isDirty = true;
		fermentationSteps.get(i).setTempU(s);
	}

	public void addFermentStep(final FermentStep fs) {
		isDirty = true;
		fermentationSteps.add(fs);
		calcFermentTotals();
	}

	public FermentStep delFermentStep(final int i) {
		isDirty = true;
		FermentStep temp = null;
		if (!fermentationSteps.isEmpty() && (i > -1) && (i < fermentationSteps.size())) {
			temp = fermentationSteps.remove(i);
			calcFermentTotals();
		}

		return temp;
	}

	public void calcFermentTotals() {
		totalFermentTime = 0;
        for (FermentStep fermentationStep : fermentationSteps) {
            totalFermentTime += fermentationStep.getTime();
        }
		
		Collections.sort(fermentationSteps);
	}

	// hop list get functions:
	public String getHopUnits() {
		return hopUnits;
	}

	public Hop getHop(final int i) {
		if(i < hops.size()) {
			return hops.get(i);
		} else {
			return null;
		}
	}

	public int getHopsListSize() {
		return hops.size();
	}

	public String getHopName(final int i) {
		return hops.get(i).getName();
	}

	public String getHopType(final int i) {
		return hops.get(i).getType();
	}

	public double getHopAlpha(final int i) {
		return hops.get(i).getAlpha();
	}

	public String getHopUnits(final int i) {
		return hops.get(i).getUnits();
	}

	public String getHopAdd(final int i) {
		return hops.get(i).getAdd();
	}

	public int getHopMinutes(final int i) {
		return hops.get(i).getMinutes();
	}

	public double getHopIBU(final int i) {
		return hops.get(i).getIBU();
	}

	public double getHopCostPerU(final int i) {
		return hops.get(i).getCostPerU();
	}

	public double getHopAmountAs(final int i, final String s) {
		return hops.get(i).getAmountAs(s);
	}

	public String getHopDescription(final int i) {
		return hops.get(i).getDescription();
	}

	// hop list set functions
	public void setHopUnits(final int i, final String u) {
		isDirty = true;
		hops.get(i).setUnits(u);
	}

	public void setHopName(final int i, final String n) {
		isDirty = true;
		hops.get(i).setName(n);
	}

	public void setHopType(final int i, final String t) {
		isDirty = true;
		hops.get(i).setType(t);
	}

	public void setHopAdd(final int i, final String a) {
		isDirty = true;
		hops.get(i).setAdd(a);
	}

	public void setHopAlpha(final int i, final double a) {
		isDirty = true;
		hops.get(i).setAlpha(a);
	}

	public void setHopMinutes(final int i, final int m) {
		isDirty = true;
		// have to re-sort hops
		hops.get(i).setMinutes(m);
	}

	public void setHopCost(final int i, final String c) {
		isDirty = true;
		hops.get(i).setCost(c);
	}

	public void setHopAmount(final int i, final double a) {
		isDirty = true;
		hops.get(i).setAmount(a);
	}

	// fermentable get methods
	// public ArrayList getFermentablesList() { return fermentables; }
	public Fermentable getFermentable(final int i) {
		if (i < fermentables.size()) {
            return fermentables.get(i);
        } else {
			return null;
		}
	}

	public int getMaltListSize() {
		return fermentables.size();
	}

	public String getMaltName(final int i) {
		return fermentables.get(i).getName();
	}

	public String getMaltUnits(final int i) {
		return fermentables.get(i).getUnits();
	}

	public double getMaltPppg(final int i) {
		return fermentables.get(i).getPppg();
	}

	public double getMaltLov(final int i) {
		return fermentables.get(i).getLov();
	}

	public double getMaltCostPerU(final int i) {
		return fermentables.get(i).getCostPerU();
	}
	
	public double getMaltCostPerUAs(final int i, String s) {
		return fermentables.get(i).getCostPerUAs(s);
	}

	public double getMaltPercent(final int i) {
		return fermentables.get(i).getPercent();
	}

	public double getMaltAmountAs(final int i, final String s) {
		return fermentables.get(i).getAmountAs(s);
	}

	public String getMaltDescription(final int i) {
		return fermentables.get(i).getDescription();
	}

	public boolean getMaltMashed(final int i) {
		return fermentables.get(i).getMashed();
	}

	public boolean getMaltSteep(final int i) {
		return fermentables.get(i).getSteep();
	}
	
	public boolean getMaltFerments(final int i) {
		return fermentables.get(i).ferments();
	}

	// fermentable set methods
	public void setMaltName(final int i, final String n) {
		// have to re-sort
		isDirty = true;
		fermentables.get(i).setName(n);
		
	}

	public void setMaltUnits(final int i, final String u) {
		isDirty = true;
		fermentables.get(i).setUnits(u);
		fermentables.get(i).setCost(fermentables.get(i).getCostPerUAs(u));
	}

	public void setMaltAmount(final int i, final double a) {
	    setMaltAmount(i, a, false);
	}
	
	public void setMaltAmount(final int i, final double a, final boolean sort) {
		isDirty = true;
		fermentables.get(i).setAmount(a);
		Comparator<Fermentable> c = new Comparator<Fermentable>()  {
			public int compare(Fermentable h1, Fermentable h2){
				if(h1.getAmountAs(Quantity.LB) > h2.getAmountAs(Quantity.LB))
					return 1;
				if(h1.getAmountAs(Quantity.LB) < h2.getAmountAs(Quantity.LB))
					return -1;
				if(h1.getAmountAs(Quantity.LB) == h2.getAmountAs(Quantity.LB))
					// same amount, check later
					return 0;
				return 0;
				
			}
		
		};
	
		if (sort) {
		    calcMaltTotals();
		}
	}

	public void setMaltAmountAs(final int i, final double a, final String u) {
		isDirty = true;
		fermentables.get(i).setAmountAs(a, u);
		fermentables.get(i).setCost(fermentables.get(i).getCostPerUAs(u));
	}

	public void setMaltPppg(final int i, final double p) {
		isDirty = true;
		fermentables.get(i).setPppg(p);
	}

	public void setMaltLov(final int i, final double l) {
		isDirty = true;
		
		fermentables.get(i).setLov(l);
	}

	public void setMaltCost(final int i, final String c) {
		isDirty = true;
		fermentables.get(i).setCost(c);
	}

	public void setMaltCost(final int i, final Double c) {
		isDirty = true;
		fermentables.get(i).setCost(c);
	}
	
	public void setMaltPercent(final int i, final double p) {
		isDirty = true;
		fermentables.get(i).setPercent(p);
	}

	public void setMaltSteep(final int i, final boolean c) {
		isDirty = true;
		fermentables.get(i).setSteep(c);
	}

	public void setMaltMashed(final int i, final boolean c) {
		isDirty = true;
		fermentables.get(i).setMashed(c);
	}
	
	public void setMaltFerments(final int i, final boolean c) {
		isDirty = true;
		fermentables.get(i).ferments(c);
	}

	// misc get/set functions
	public int getMiscListSize() {
		return misc.size();
	}

	public Misc getMisc(final int i) {
		return misc.get(i);
	}

	public String getMiscName(final int i) {
		return misc.get(i).getName();
	}

	public void setMiscName(final int i, final String n) {
		isDirty = true;
		misc.get(i).setName(n);
	}

	public double getMiscAmount(final int i) {
		final Misc m = misc.get(i);
		return m.getAmountAs(m.getUnits());
	}

	public void setMiscAmount(final int i, final double a) {
		isDirty = true;
		misc.get(i).setAmount(a);
		calcMiscCost();
	}

	public String getMiscUnits(final int i) {
		return misc.get(i).getUnits();
	}

	public void setMiscUnits(final int i, final String u) {
		isDirty = true;
		misc.get(i).setUnits(u);
		calcMiscCost();
	}

	public double getMiscCost(final int i) {
		return misc.get(i).getCostPerU();
	}

	public void setMiscCost(final int i, final double c) {
		isDirty = true;
		misc.get(i).setCost(c);
		calcMiscCost();
	}

	public String getMiscStage(final int i) {
		return misc.get(i).getStage();
	}

	public void setMiscStage(final int i, final String s) {
		isDirty = true;
		misc.get(i).setStage(s);
	}

	public int getMiscTime(final int i) {
		return misc.get(i).getTime();
	}

	public void setMiscTime(final int i, final int t) {
		isDirty = true;
		misc.get(i).setTime(t);
	}

	public String getMiscDescription(final int i) {
		return misc.get(i).getDescription();
	}

	public void setMiscComments(final int i, final String c) {
		isDirty = true;
		misc.get(i).setComments(c);
	}

	public String getMiscComments(final int i) {
		return misc.get(i).getComments();
	}

	// notes get/set methods
	public Note getNote(int i) {
		return notes.get(i);
	}

	public int getNotesListSize() {
		return notes.size();
	}

	public Date getNoteDate(final int i) {
		return notes.get(i).getDate();
	}

	public void setNoteDate(final int i, final Date d) {
		isDirty = true;
		notes.get(i).setDate(d);
	}

	public String getNoteType(final int i) {
		return notes.get(i).getType();
	}

	public void setNoteType(final int i, final String t) {
		isDirty = true;
		if (i > -1) {
			notes.get(i).setType(t);
		}
	}

	public String getNoteNote(final int i) {
		return notes.get(i).getNote();
	}

	public void setNoteNote(final int i, final String n) {
		isDirty = true;
		notes.get(i).setNote(n);
	}

	// Setters that need to do extra work:

	public void setVolUnits(final String v) {
		isDirty = true;
		kettleLossVol.convertTo(v);
		postBoilVol.convertTo(v);
		trubLossVol.convertTo(v);
		miscLossVol.convertTo(v);
		calcMaltTotals();
		calcHopsTotals();
	}
	
	public void setReadVolUnits(final String v) {
		isDirty = true;
		kettleLossVol.setUnits(v);
		postBoilVol.setUnits(v);
		trubLossVol.setUnits(v);
		miscLossVol.setUnits(v);
		calcMaltTotals();
		calcHopsTotals();
	}

	public void setEstFg(final double f) {
		isDirty = true;
		if ((f != estFg) && (f > 0)) {
			estFg = f;
			attenuation = 100 - ((estFg - 1) / (estOg - 1) * 100);
		}
	}

	public void setEstOg(final double o) {
		if (o == 0) {
			return;
		}
		isDirty = true;
		if ((o != estOg) && (o > 0)) {
			estOg = o;
			attenuation = 100 - ((estFg - 1) / (estOg - 1) * 100);
			calcEfficiency();
		}
	}

	public void setEfficiency(final double e) {
		isDirty = true;
		if ((e != efficiency) && (e > 0)) {
			efficiency = e;
			calcMaltTotals();
		}
	}

	public void setAttenuation(final double a) {
		isDirty = true;
		if ((a != attenuation) && (a > 0)) {
			attenuation = a;
			calcMaltTotals();
		}

	}

	public void setPreBoil(final Quantity p) {
		isDirty = true;

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
		isDirty = true;

		// One-true vol, set it and all the getters will go from there
		// Hack alert; chop off "double ugglyness" rounding errors at umtenth
		// decimal place
		// this is causing recalc problems:
		// long hackNumber = (long)(p.getValue() * 100);
		// p.setAmount((double)hackNumber / 100);
		postBoilVol = p;

		// Recalc all the bits
		calcMaltTotals();
		calcHopsTotals();
		calcPrimeSugar();
		calcKegPSI();
	}

	public void setFinalWortVol(final Quantity p) {
		isDirty = true;

		// The one-true-volume is postBoil.. so calc it, and set it
		Quantity post = new Quantity(getVolUnits(), p.getValueAs(getVolUnits()) + getKettleLoss(getVolUnits())
				+ getTrubLoss(getVolUnits()) + getMiscLoss(getVolUnits()));
		setPostBoil(post);
	}

	/*
	 * Functions that add/remove from ingredient lists
	 */
	public void addMalt(final Fermentable m) {
		isDirty = true;
		
		
		
		fermentables.add(m);
		calcMaltTotals();
	}

	public void delMalt(final int i) {
		isDirty = true;
		if (!fermentables.isEmpty() && (i > -1) && (i < fermentables.size())) {
			fermentables.remove(i);
			calcMaltTotals();
		}
	}

	public void addHop(final Hop h) {
		isDirty = true;
		hops.add(h);
		calcHopsTotals();
	}

	public void delHop(final int i) {
		isDirty = true;
		if (!hops.isEmpty() && (i > -1) && (i < hops.size())) {
			hops.remove(i);
			calcHopsTotals();
		}
	}

	public void addMisc(final Misc m) {
		isDirty = true;
		misc.add(m);
		calcMiscCost();
	}

	public void delMisc(final int i) {
		isDirty = true;
		if (!misc.isEmpty() && (i > -1) && (i < misc.size())) {
			misc.remove(i);
			calcMiscCost();
		}
	}

	private void calcMiscCost() {
		totalMiscCost = 0;
        for (final Misc m : misc) {
            totalMiscCost += m.getAmountAs(m.getUnits()) * m.getCostPerU();
        }
	}

	public void addNote(final Note n) {
		isDirty = true;
		notes.add(n);
	}

	public void delNote(final int i) {
		isDirty = true;
		if (!notes.isEmpty() && (i > -1) && (i < notes.size())) {
			notes.remove(i);
		}
	}

	/**
	 * Handles a string of the form "d u", where d is a double amount, and u is
	 * a string of units. For importing the quantity attribute from QBrew xml.
	 * 
	 * @param a The Amount and unit string to parse.
	 */
	public void setAmountAndUnits(final String a) {
		isDirty = true;
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
        for (final Fermentable m : fermentables) {
            possiblePoints += (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }
		efficiency = (estOg - 1) / possiblePoints * 100;
	}

	public void calcMaltTotals() {

		if (!allowRecalcs) {
			return;
		}
		double maltPoints = 0;
		double fermentingMaltPoints = 0;
		totalMaltLbs = 0;
		totalMaltCost = 0;
		totalMashLbs = 0;

		double curPoints = 0.0;
		
		// first figure out the total we're dealing with
        for (final Fermentable m : fermentables) {
            if (m.getName().equals("") || m.getAmountAs(Quantity.LB) <= 0.00) {
                continue;
            }
            totalMaltLbs += (m.getAmountAs(Quantity.LB));
            if (m.getMashed()) { // apply efficiency and add to mash weight
                curPoints = (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) * getEfficiency()
                        / getPostBoilVol(Quantity.GAL);
                totalMashLbs += (m.getAmountAs(Quantity.LB));
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
        for (Fermentable m : fermentables) {
            // Malt % By Weight
            if (m.getAmountAs(Quantity.LB) == 0) {
                m.setPercent(0);
            } else {
                m.setPercent((m.getAmountAs(Quantity.LB) / totalMaltLbs * 100));
            }

            totalMaltCost += m.getCostPerU() * m.getAmountAs(m.getUnits());
        }

		// set the fields in the object
		estOg = (maltPoints / 100) + 1;
        double estFermOg = (fermentingMaltPoints / 100) + 1;

		double attGrav = (estFermOg - 1) * (getAttenuation() / 100);

		// FG
		estFg = estOg - attGrav;
		// mash.setMaltWeight(totalMashLbs);
		
        Collections.sort(fermentables, fermCompare);
        Collections.reverse(fermentables);
	}

	public void calcHopsTotals() {

		if (!allowRecalcs) {
			return;
		}
	    Collections.sort(hops);
		double ibuTotal = 0;
		totalHopsCost = 0;
		totalHopsOz = 0;

        for (Hop hop : hops) {
            // calculate the average OG of the boil
            // first, the OG at the time of addition:
            double adjPreSize, aveOg;

            int time = hop.getMinutes();
            if (hop.getAdd().equalsIgnoreCase(Hop.FWH)) {
                time = time - fwhTime;
            } else if (hop.getAdd().equalsIgnoreCase(Hop.MASH)) {
                time = mashHopTime;
            } else if (hop.getAdd().equalsIgnoreCase(Hop.DRY)) {
                time = dryHopTime;
            }

            if (hop.getMinutes() > 0) {
                adjPreSize = getPostBoilVol(Quantity.GAL)
                        + (getPreBoilVol(Quantity.GAL) - getPostBoilVol(Quantity.GAL))
                        / (getBoilMinutes() / hop.getMinutes());
            } else {
                adjPreSize = getPostBoilVol(Quantity.GAL);
            }

            aveOg = 1 + (((estOg - 1) + ((estOg - 1) / (adjPreSize / getPostBoilVol(Quantity.GAL)))) / 2);
            BrewServer.LOG.warning(String.format("IBU Util: %.2f, OG: %.3f, adjPreSize: %.2f", ibuHopUtil, aveOg, adjPreSize));
            switch (ibuCalcMethod) {
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
            BrewServer.LOG.warning("Precalc: " + hop.getIBU());
            if (hop.getType().equalsIgnoreCase(Hop.PELLET)) {
                hop.setIBU(hop.getIBU() * (1.0 + (getPelletHopPct() / 100)));
            }
            BrewServer.LOG.warning("Postcalc: " + hop.getIBU());

            ibuTotal += hop.getIBU();

            totalHopsCost += hop.getCostPerU() * hop.getAmountAs(hop.getUnits());
            totalHopsOz += hop.getAmountAs(Quantity.OZ);
        }

		ibu = ibuTotal;

	}

	private String addXMLHeader(String in) {
		in = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" 
		        + "<?xml-stylesheet type=\"text/xsl\" href=\"http://strangebrewcloud.appspot.com/html/recipeToHtml.xslt\"?>"
		        + in;
		return in;
	}

	
	public static String buildString(final char ch, final int length) {
		final char newStr[] = new char[length];
		for (int i = 0; i < length; ++i) {
			newStr[i] = ch;
		}
		return new String(newStr);
	}

	public String toText() {
		return toText(false);
	}

	public static String padLeft(final String str, final int fullLength, final char ch) {
        return (fullLength > str.length()) ? str.concat(Recipe.buildString(ch, fullLength - str.length())) : str;
    }

    public static String padRight(final String str, final int fullLength, final char ch) {
        return (fullLength > str.length()) ? Recipe.buildString(ch, fullLength - str.length()).concat(str) : str;
    }
    
	public String toText(boolean detailed) {
		MessageFormat mf;
		final StringBuilder sb = new StringBuilder();
		sb.append("StrangeBrew J v.").append(version).append(" recipe text output\n\n");
		sb.append("Details:\n");
		sb.append("Name: ").append(name).append("\n");
		sb.append("Brewer: ").append(brewer).append("\n");
		sb.append("Size: ").append(SBStringUtils.format(getPostBoilVol(getVolUnits()), 1)).append(" ").append(getVolUnits()).append("\n");
		sb.append("Style: ").append(style.getName()).append("\n");
		mf = new MessageFormat(
				"OG: {0,number,0.000},\tFG:{1,number,0.000}, \tAlc:{2,number,0.0}, \tIBU:{3,number,0.0}\n");
		final Object[] objs = {estOg, estFg, getAlcohol(), ibu};
		sb.append(mf.format(objs));
		sb.append("(Alc method: by ").append(getAlcMethod()).append("; IBU method: ").append(ibuCalcMethod).append(")\n");
		if (yeasts.size() == 1) {
		    sb.append("\nYeast: ");
		} else {
		    sb.append("\nYeasts: ");
		}
		for (Yeast yeast: yeasts) {
		    sb.append(yeast.getName()).append("\n");
		}
		sb.append("\nFermentables:\n");
		sb.append(Recipe.padLeft("Name ", 30, ' ')).append(" amount units  pppg    lov   %\n");

		mf = new MessageFormat("{0} {1} {2} {3,number,0.000} {4} {5}%\n");
        for (Fermentable fermentable : fermentables) {

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
        for (final Hop h : hops) {
            final Object[] objh = {Recipe.padLeft(h.getName() + " (" + h.getType() + ")", 20, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getAmountAs(h.getUnits()), 2), 6, ' '),
                    Recipe.padRight(" " + h.getUnitsAbrv(), 5, ' '), Recipe.padRight(" " + h.getAlpha(), 6, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getMinutes(), 1), 6, ' '),
                    Recipe.padRight(" " + SBStringUtils.format(h.getIBU(), 1), 5, ' ')};
            sb.append(mf.format(objh));

        }

		if (mash.getStepSize() > 0) {
			sb.append("\nMash:\n");
			sb.append(Recipe.padLeft("Step ", 10, ' ')).append("  Temp   End    Ramp    Min	Input	Output	Water Temp\n");

			mf = new MessageFormat("{0} {1} {2} {3} {4} {5} {6} {7}\n");
			for (int i = 0; i < mash.getStepSize(); i++) {

				final Object[] objm = { Recipe.padLeft(mash.getStepType(i), 10, ' '),
						Recipe.padRight(" " + mash.getStepStartTemp(i), 6, ' '),
						Recipe.padRight(" " + mash.getStepEndTemp(i), 6, ' '),
						Recipe.padRight(" " + mash.getStepRampMin(i), 4, ' '),
						Recipe.padRight(" " + mash.getStepMin(i), 6, ' '),
						Recipe.padRight(" " + SBStringUtils.format(mash.getStepInVol(i), 2) , 6, ' '),
						Recipe.padRight(" " + SBStringUtils.format(mash.getStepOutVol(i), 2), 6, ' '),
						Recipe.padRight(" " + mash.getStepTemp(i), 6, ' ')};

				sb.append(mf.format(objm));
			}
		}

		if (notes.size() > 0) {
			sb.append("\nNotes:\n");
            for (Note note : notes) {
                sb.append(note.toString());
            }
		}

		// only print this stuff for detailed text output:

		if (detailed) {
			// Fermentation Schedule
			if (fermentationSteps.size() > 0) {
				sb.append("\nFermentation Schedule:\n");
				sb.append(Recipe.padLeft("Step ", 10, ' ')).append("  Time   Days\n");
				mf = new MessageFormat("{0} {1} {2}\n");
                for (final FermentStep f : fermentationSteps) {
                    final Object[] objm = {Recipe.padLeft(f.getType(), 10, ' '),
                            Recipe.padRight(" " + f.getTime(), 6, ' '),
                            Recipe.padRight(" " + f.getTemp() + f.getTempU(), 6, ' ')};
                    sb.append(mf.format(objm));
                }
			}

			// Carb
			sb.append("\nCarbonation:  ").append(targetVol).append(" volumes CO2\n");
			sb.append(" Bottle Temp: ").append(bottleTemp).append(carbTempU).append("  Serving Temp:").append(servTemp).append(carbTempU).append("\n");
			sb.append(" Priming: ").append(SBStringUtils.format(primeSugar.getAmountAs(primeSugar.getUnits()), 1)).append(primeSugar.getUnitsAbrv()).append(" of ").append(primeSugar.getName()).append("\n");
			sb.append(" Or keg at: ").append(kegPSI).append("PSI\n");

			if ((!Objects.equals(sourceWater.getName(), "")) || (!Objects.equals(targetWater.getName(), ""))) {
				sb.append("\nWater Profile\n");
				sb.append(" Source Water: ").append(sourceWater.toString()).append("\n");
				sb.append(" Target Water: ").append(targetWater.toString()).append("\n");

				if (brewingSalts.size() > 0) {
					sb.append(" Salt Additions per Gal\n");
                    for (Salt brewingSalt : brewingSalts) {
                        sb.append("  ").append(brewingSalt.toString()).append("\n");
                    }
				}
				sb.append(" Acid: ").append(SBStringUtils.format(getAcidAmount(), 2)).append(acid.getAcidUnit()).append(" per gal of ").append(acid.getName()).append(" Acid\n");
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
		return bottleTemp;
	}

	public void setBottleTemp(final double bottleTemp) {
		isDirty = true;
		this.bottleTemp = bottleTemp;
		calcPrimeSugar();
	}

	public String getCarbTempU() {
		return carbTempU;
	}

	public void setCarbTempU(final String carbU) {
		isDirty = true;
		this.carbTempU = carbU;
	}

	public boolean isKegged() {
		return kegged;
	}

	public void setKegged(final boolean kegged) {
		isDirty = true;
		this.kegged = kegged;
		calcKegPSI();
	}

	public double getKegPSI() {
		return this.kegPSI;
	}

	public void setKegPSI(final double psi) {
		isDirty = true;
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
		return primeSugar.getName();
	}

	public String getPrimeSugarU() {
		return primeSugar.getUnitsAbrv();
	}

	public void setPrimeSugarU(final String primeU) {
		isDirty = true;
		this.primeSugar.setUnits(primeU);
		calcPrimeSugar();
	}

	public double getPrimeSugarAmt() {
		return primeSugar.getAmountAs(primeSugar.getUnitsAbrv());
	}

	public void calcPrimeSugar() {
        if (!allowRecalcs) {
            return;
        }
		final double dissolvedCO2 = BrewCalcs.dissolvedCO2(getBottleTemp());
		final double primeSugarGL = BrewCalcs.PrimingSugarGL(dissolvedCO2, getTargetVol(), getPrimeSugar());

		// Convert to selected Units
		double neededPrime = Quantity.convertUnit(Quantity.G, getPrimeSugarU(), primeSugarGL);
		neededPrime *= Quantity.convertUnit(Quantity.L, getVolUnits(), primeSugarGL);
		neededPrime *= getFinalWortVol(getVolUnits());

		primeSugar.setAmount(neededPrime);
	}

	public void calcKegPSI() {
        if (!allowRecalcs) {
            return;
        }
        kegPSI = BrewCalcs.KegPSI(servTemp, getTargetVol());
	}

	public void setPrimeSugarName(final String n) {
		isDirty = true;
		this.primeSugar.setName(n);
	}
	
	public void setPrimeSugarYield(final double yield) {
        isDirty = true;
        this.primeSugar.setYield(yield);
    }

	public void setPrimeSugarAmount(final double q) {
		isDirty = true;
		this.primeSugar.setAmount(q);
	}

	public double getServTemp() {
		return servTemp;
	}

	public void setServTemp(final double servTemp) {
		isDirty = true;
		this.servTemp = servTemp;
		calcKegPSI();
	}

	public double getTargetVol() {
		return targetVol;
	}

	public void setTargetVol(final double targetVol) {
		isDirty = true;
		this.targetVol = targetVol;
		calcPrimeSugar();
		calcKegPSI();
	}

	public PrimeSugar getPrimeSugar() {
		return primeSugar;
	}

	public void setPrimeSugar(final PrimeSugar primeSugar) {
		this.primeSugar = primeSugar;
		calcPrimeSugar();
	}

	public WaterProfile getSourceWater() {
		return sourceWater;
	}

	public WaterProfile getTargetWater() {
		return targetWater;
	}

	public void setSourceWater(final WaterProfile sourceWater) {
		this.sourceWater = sourceWater;
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
		if(!brewingSalts.isEmpty() && (i > -1) && (i < brewingSalts.size()))
		this.brewingSalts.remove(i);
	}

	public void setSalts(final ArrayList<Salt> s) {
		this.brewingSalts = s;
	}

	public List<Salt> getSalts() {
		return this.brewingSalts;
	}

	public Salt getSalt(final int i) {
		return this.brewingSalts.get(i);
	}

	public Salt getSaltByName(final String name) {
        for (final Salt s : brewingSalts) {
            if (s.getName().equals(name)) {
                return s;
            }
        }

		return null;
	}

	public Acid getAcid() {
		return acid;
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

	/**
	 * Implement a local comparator.
	 */
	Comparator<Fermentable> fermCompare = new Comparator<Fermentable>()  {
        public int compare(Fermentable h1, Fermentable h2){
            if (h1.getAmountAs(Quantity.LB) > h2.getAmountAs(Quantity.LB))
                return 1;
            if (h1.getAmountAs(Quantity.LB) < h2.getAmountAs(Quantity.LB))
                return -1;
            if (h1.getAmountAs(Quantity.LB) == h2.getAmountAs(Quantity.LB))
                return 0;
            return 0;
        }
    };

    public List<Yeast> getYeasts() {
        return yeasts;
    }

    /**
     * Identify the type of this recipe.
     * @return ALL GRAIN, PARTIAL MASH, or EXTRACT or UNKNOWN.
     */
    public final String getType() {
        boolean hasMashed = false;
        boolean notMashed = false;

        for (int i = 0; i < this.fermentables.size(); i++) {
            hasMashed = getFermentable(i).getMashed();
        }

        if (hasMashed && !notMashed) {
            return "ALL GRAIN";
        } else if (hasMashed && notMashed) {
            return "PARTIAL MASH";
        } else if (!hasMashed && notMashed) {
            return "EXTRACT";
        }

        return "UNKNOWN";
    }

    public void setEquipmentProfile(Equipment equipmentProfile) {
        this.equipmentProfile = equipmentProfile;
    }

    public Equipment getEquipmentProfile() {
        return equipmentProfile;
    }

    public void setTasteNotes(String tasteNotes) {
        this.tasteNotes = tasteNotes;
    }

    public String getTasteNotes() {
        return tasteNotes;
    }

    public void setTasteRating(double tasteRating) {
        this.tasteRating = tasteRating;
    }

    public double getTasteRating() {
        return tasteRating;
    }

    public void setMeasuredOg(double measuredOg) {
        this.measuredOg = measuredOg;
    }

    public double getMeasuredOg() {
        return measuredOg;
    }

    public void setMeasuredFg(double measuredFg) {
        this.measuredFg = measuredFg;
    }

    public double getMeasuredFg() {
        return measuredFg;
    }

    public void setDateBrewed(String dateBrewed) {
        this.dateBrewed = dateBrewed;
    }

    public String getDateBrewed() {
        return dateBrewed;
    }

    public void setPrimeSugarEquiv(double primeSugarEquiv) {
        this.primeSugarEquiv = primeSugarEquiv;
    }

    public double getPrimeSugarEquiv() {
        return primeSugarEquiv;
    }

    public void setKegPrimingFactor(double kegPrimingFactor) {
        this.kegPrimingFactor = kegPrimingFactor;
    }

    public double getKegPrimingFactor() {
        return kegPrimingFactor;
    }

    public double getMeasuredAlcohol() {
        return BrewCalcs.calcAlcohol(getAlcMethod(), measuredOg, measuredFg);
    }

    public double getMeasuredEfficiency() {
        double possiblePoints = 0;
        for (final Fermentable m : fermentables) {
            possiblePoints += (m.getPppg() - 1) * m.getAmountAs(Quantity.LB) / getPostBoilVol(Quantity.GAL);
        }
        return (measuredOg - 1) / possiblePoints * 100;
    }

    public String calcCalories() {
        double alcCal = 1881.22 * measuredFg  * (measuredOg - measuredFg) / (1.775 - measuredOg);
        double carbCal = 3550.0 * measuredFg * ((0.1808 * measuredOg) + (0.8192 * measuredFg) - 1.0004);

        double totalCal = alcCal + carbCal;
        return Double.toString(totalCal) + "Cal/12oz";
    }

    public void setGravUnit(String newUnit) {
        gravUnits = newUnit;
    }
    public String getGravUnits() {
        return gravUnits;
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

    public void setCarbMethod(String carbMethod) {
        this.carbMethod = carbMethod;
    }

    public String getCarbMethod() {
        return carbMethod;
    }

    public void setMash(Mash mash) {
        this.mash = mash;
    }

    public void setMashProfile(Temp tempProbe) {
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        for (int i = 0; i < mash.getStepSize(); i++) {
            TemperatureTrigger temperatureTrigger = new TemperatureTrigger(i * 2, tempProbe.getName(),
                    mash.getStepStartTemp(i), mash.getStepType(i), mash.getStepMethod(i));
            temperatureTrigger.setExitTemp(mash.getStepEndTemp(i));
            triggerControl.addTrigger(temperatureTrigger);

            WaitTrigger waitTrigger = new WaitTrigger((i*2)+1, mash.getStepMin(i), 0);
            waitTrigger.setNote(String.format(Messages.HOLD_STEP, String.format("%.2f", mash.getStepEndTemp(i)) + mash.getStepTempU(i)));
            triggerControl.addTrigger(waitTrigger);
        }
    }

    public void setBoilHops(Temp tempProbe) {
        // Base this from the time different between steps. When they press activate the first step goes immediately (just annotate)
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int j = 0;
        Hop prevHop = null;
        for (Hop h : hops) {
            // Skip to the next hop if it's not a boil hop
            if (!h.getAdd().equals(Hop.BOIL)) {
                continue;
            }

            double mins;
            if (prevHop != null) {
                mins = prevHop.getMinutes() - h.getMinutes();
            } else {
                mins = boilMinutes - h.getMinutes();
            }

            WaitTrigger waitTrigger = new WaitTrigger(j, mins, 0);
            waitTrigger.setNote(String.format(Messages.ADD_HOP, h.getAmount().toString(), h.getName()));
            triggerControl.addTrigger(waitTrigger);
            j++;
            prevHop = h;
        }
    }

    public void setFermProfile(Temp tempProbe) {
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int i = 0;
        for (FermentStep fermentStep: fermentationSteps) {
            TemperatureTrigger temperatureTrigger = new TemperatureTrigger(i * 2, tempProbe.getName(),
                    fermentStep.getTemp(), fermentStep.getType(), "");
            triggerControl.addTrigger(temperatureTrigger);

            i++;
            WaitTrigger waitTrigger = new WaitTrigger(i, fermentStep.getTimeMins(), 0);
            waitTrigger.setNote(String.format(Messages.HOLD_STEP, String.format("%.2f", fermentStep.getTemp()) + fermentStep.getTempU()));
            triggerControl.addTrigger(waitTrigger);
            i++;
        }
    }

    public void setDryHops(Temp tempProbe) {
        // Base this from the time different between steps. When they press activate the first step goes immediately (just annotate)
        TriggerControl triggerControl = tempProbe.getTriggerControl();
        if (triggerControl.getTriggersSize() > 0) {
            triggerControl.clear();
        }

        int j = 0;
        Hop prevHop = null;
        for (Hop h : hops) {
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