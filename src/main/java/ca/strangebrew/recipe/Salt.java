package ca.strangebrew.recipe;

import com.sb.common.SBStringUtils;

import java.text.MessageFormat;

public class Salt {
	private String name;
	private String commonName;
	private String chemicalName;
	private double amount = 0.0;
	private String amountU = Quantity.G;
	private double gramsPerTsp = 0.0;
	private ChemicalEffect[] chemicalEffects = null;
	
	public static final String MAGNESIUM = "Mg";
	public static final String CHLORINE = "Cl";
	public static final String SODIUM = "Na";
	public static final String SULPHATE = "So4"; 
	public static final String CARBONATE = "Co3";
	public static final String CALCIUM = "Ca";
	public static final String HARDNESS = "Hardness";
	public static final String ALKALINITY = "Alkalinity";
	
	public static final String MAGNESIUM_SULPHATE = "Magnesium Sulfate";
	public static final String CALCIUM_CARBONATE = "Calcium Carbonate";
	public static final String SODIUM_CHLORIDE = "Sodium Chloride";
	public static final String SODIUM_BICARBONATE = "Sodium Bicarbonate";
	public static final String CALCIUM_SULPHATE = "Calcium Sulphate";
	public static final String CALCIUM_CHLORIDE = "Calcium Chloride";
	
	public static final String EPSOM_SALT = "Epsom Salt"; 
	public static final String GYPSUM = "Gypsum"; 
	public static final String SALT = "Salt"; 
	public static final String BAKING_SODA = "Baking Soda"; 
	public static final String CHALK = "Chalk"; 
//	public static final String CALCIUM_CHLORIDE = "Calcium Chloride"; 
	
	public static final Salt[] salts = {
		new Salt(MAGNESIUM_SULPHATE, EPSOM_SALT, "MgSo4", 4.5, new ChemicalEffect[]{
			new ChemicalEffect(SULPHATE, 103.0),
			new ChemicalEffect(MAGNESIUM, 26.1),
			new ChemicalEffect(HARDNESS, 107.8)
		}),
		new Salt(CALCIUM_CARBONATE, GYPSUM, "CaCo3", 1.8, new ChemicalEffect[]{
			new ChemicalEffect(CALCIUM, 61.5),
			new ChemicalEffect(SULPHATE, 147.4),
			new ChemicalEffect(HARDNESS, 153.6)
		}),
		new Salt(SODIUM_CHLORIDE, SALT, "NaCl", 6.5, new ChemicalEffect[]{
			new ChemicalEffect(SODIUM, 103.9),
			new ChemicalEffect(CHLORINE, 160.3),
			new ChemicalEffect(HARDNESS, 107.8)
		}),
		new Salt(SODIUM_BICARBONATE, BAKING_SODA, "NaHCo3", 4.4, new ChemicalEffect[]{
			new ChemicalEffect(SODIUM, 72.3),
			new ChemicalEffect(CARBONATE, 188.7),
			new ChemicalEffect(ALKALINITY, 157.4)
		}),
		new Salt(CALCIUM_SULPHATE, CHALK, "CaSo4", 4.0, new ChemicalEffect[]{
			new ChemicalEffect(CALCIUM, 105.8),
			new ChemicalEffect(CARBONATE, 158.4),
			new ChemicalEffect(HARDNESS, 264.2),
			new ChemicalEffect(ALKALINITY, 264.2)
		}),
		new Salt(CALCIUM_CHLORIDE, CALCIUM_CHLORIDE, "CaCl2", 3.4, new ChemicalEffect[]{
			new ChemicalEffect(CALCIUM, 72.0),
			new ChemicalEffect(CARBONATE, 127.4),
			new ChemicalEffect(HARDNESS, 179.8)
		})
	};
	
	public Salt() {		
	}
	
	public Salt(String name, String commonName, String chemicalName, double gramsPerTsp, ChemicalEffect[] chemEff) {
		this.name = name;
		this.commonName = commonName;
		this.chemicalName = chemicalName;
		this.amount = 0.0;
		this.amountU = Quantity.G;
		this.gramsPerTsp = gramsPerTsp;
		// this.chemicalEffects = Arrays.copyOf(chemEff, chemEff.length);
		this.chemicalEffects = chemEff.clone();
	}
	
	public Salt(Salt s) {
		this.name = s.getName();
		this.commonName = s.getCommonName();
		this.chemicalName = s.getChemicalName();
		this.amount = s.getAmount();
		this.amountU = s.getAmountU();
		this.gramsPerTsp = s.getGramsPerTsp();
		this.chemicalEffects = s.getChemicalEffects().clone();
	}

	public double getAmount() {
		return amount;
	}

	public String getAmountU() {
		return amountU;
	}

	public String getChemicalName() {
		return chemicalName;
	}

	public String getCommonName() {
		return commonName;
	}

	public String getName() {
		return name;
	}

	public ChemicalEffect[] getChemicalEffects() {
		return chemicalEffects;
	}
	
	public void setAmount(double amount) {
		this.amount = amount;
	}
	
	public String toString() {
		MessageFormat mf;
		
		mf = new MessageFormat(name + "(" + commonName + ") {0,number,0.000}" + amountU);		
		Object[] objs = {new Double(amount)};
				
		return mf.format(objs);
	}
	
	public String toXML(int indent) {
		String xml = "";
		
		xml += SBStringUtils.xmlElement("NAME", name, indent);
		xml += SBStringUtils.xmlElement("COMMONNAME", commonName, indent);
		xml += SBStringUtils.xmlElement("CHEM", chemicalName, indent);
		xml += SBStringUtils.xmlElement("AMOUNT", SBStringUtils.format(amount, 2), indent);
		xml += SBStringUtils.xmlElement("AMOUNTU", amountU, indent);		
			
		return xml;
	}

	// TODO currently only in grams
	/*public void setAmountU(String amountU) {
		this.amountU = amountU;
	}*/

	public void setChemicalEffects(ChemicalEffect[] effs) {
		// this.chemicalEffects = Arrays.copyOf(effs, effs.length);
		this.chemicalEffects = effs.clone();
	}

	public void setChemicalName(String chemicalName) {
		this.chemicalName = chemicalName;
	}

	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static class ChemicalEffect {
		final private String elem;
		final private double effect;
		
		public ChemicalEffect(String elem, double effect) {
			this.elem = elem;
			this.effect = effect;
		}

		public double getEffect() {
			return effect;
		}

		public String getElem() {
			return elem;
		}
	}


	public double getEffectByChem(String chem) {
		for (int i = 0; i < chemicalEffects.length; i++) {
			if (chemicalEffects[i].getElem().equals(chem)) {
				return chemicalEffects[i].getEffect();
			}
		}
		
		return 0;
	}
	
	static public Salt getSaltByName(String name) {
		for (int i = 0; i < Salt.salts.length; i++) {
			if (Salt.salts[i].getName().equals(name)) {
				return Salt.salts[i];
			}
		}
		
		return null;
	}

	public double getGramsPerTsp() {
		return gramsPerTsp;
	}

	public void setGramsPerTsp(double gramsPerTsp) {
		this.gramsPerTsp = gramsPerTsp;
	}
	
	public String getVolU() {
		return "tsp";
	}
	
	public double getVol() {
		double grams = this.getAmount();
		double tsp = 0;
		
		tsp = grams / this.getGramsPerTsp();
		
		return tsp;
	}
}
