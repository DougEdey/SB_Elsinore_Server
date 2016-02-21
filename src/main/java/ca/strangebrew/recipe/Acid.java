package ca.strangebrew.recipe;

public class Acid {
	final private String name;
	final private double pK1;
	final private double pK2;
	final private double pK3;
	final private double molWt;
	final private double mgPerML;

	static final public String ACETIC = "Acetic";
	static final public String CITRIC = "Citric";
	static final public String HYDROCHLORIC = "Hydrochloric";
	static final public String LACTIC = "Lactic";
	static final public String PHOSPHORIC = "Phosphoric";
	static final public String SULFURIC = "Sulfuric";
	static final public String TARTRIC = "Tartric";
	static final public String[] acidNames = {ACETIC, CITRIC, HYDROCHLORIC, LACTIC, PHOSPHORIC, SULFURIC, TARTRIC};	
	
	static final public Acid[] acids = {
		new Acid(ACETIC, 4.75, 20, 20, 60.05, 0),
		new Acid(CITRIC, 3.14, 4.77, 6.39, 192.13, 0),
		new Acid(HYDROCHLORIC, -10, 20, 20, 36.46, 319.8), // 28% hcl
		new Acid(LACTIC, 3.08, 20, 20, 90.08, 1068), // 88% lactic
		new Acid(PHOSPHORIC, 2.12, 7.20, 12.44, 98.00, 292.5), // 25% phosphoric
		new Acid(SULFURIC, -10, 1.92, 20, 98.07, 0),
		new Acid(TARTRIC, 2.98, 4.34, 20, 150.09, 0)
	};		
	
	public Acid(String name, double pK1, double pK2, double pK3, double molWt, double mgPerML) {
		this.name = name;
		this.pK1 = pK1;
		this.pK2 = pK2;
		this.pK3 = pK3;
		this.molWt = molWt;
		this.mgPerML = mgPerML;
	}

	public double getMolWt() {
		return molWt;
	}

	public String getName() {
		return name;
	}

	public double getPK1() {
		return pK1;
	}

	public double getPK2() {
		return pK2;
	}

	public double getPK3() {
		return pK3;
	}

	public double getMgPerL() {
		return mgPerML;
	}
	
	public boolean isLiquid() {
		return mgPerML != 0;
	}
	
	public String getAcidUnit() {
		if (isLiquid()) {
			return Quantity.ML;
		} else {
			return Quantity.MG;
		}
	}
	
	public static Acid getAcidByName(String name) { 
		for (int i = 0; i < Acid.acids.length; i++) {
			if (Acid.acids[i].getName().equals(name)) {
				return Acid.acids[i];
			}
		}
		
		return null;
	}
}