package com.sb.elsinore.recipes;

import ca.strangebrew.recipe.*;
import com.sb.elsinore.LaunchControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;

/**
 * Read in a BeerXML file and provide a UI form to the user.
 *
 * @author Doug Edey
 */
public class BeerXMLReader {
    /**
     * A static reference to the beerXML Instance.
     */
    private static BeerXMLReader instance = null;
    private Logger logger = LoggerFactory.getLogger(BeerXMLReader.class);
    /**
     * The Document object that has been read in.
     **/
    private Document recipeDocument = null;

    /**
     * Get the singleton instance of this reader.
     *
     * @return The current BeerXMLReader instance.
     */
    public static BeerXMLReader getInstance() {
        if (BeerXMLReader.instance == null) {
            BeerXMLReader.instance = new BeerXMLReader();
        }

        return BeerXMLReader.instance;
    }

    /**
     * Set the file to read, and read it.
     *
     * @param inputFile The file to read in.
     * @return True if file is read OK.
     */
    public final boolean readFile(final File inputFile) {
        // Assume that it's a valid file.

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            this.logger.warn("Couldn't create Doc Builder");
            return false;
        }

        try {
            this.recipeDocument = dBuilder.parse(inputFile);
        } catch (Exception e) {
            String output = String.format(
                    "Couldn't read beerXML File at: %1s",
                    inputFile.getAbsolutePath());
            this.logger.warn(output);
            LaunchControl.setMessage(output);
            return false;
        }
        return true;
    }

    public final ArrayList<String> getListOfRecipes() {
        ArrayList<String> nameList = new ArrayList<>();
        XPath xp;
        try {
            xp = XPathFactory.newInstance().newXPath();
            NodeList recipeList =
                    (NodeList) xp.evaluate(
                            "/RECIPES/RECIPE", this.recipeDocument, XPathConstants.NODESET);
            if (recipeList.getLength() == 0) {
                LaunchControl.setMessage("No Recipes found in file");
                return null;
            }

            for (int i = 0; i < recipeList.getLength(); i++) {
                Node recipeNode = recipeList.item(i);
                String recipeName = (String) xp.evaluate("NAME/text()",
                        recipeNode, XPathConstants.STRING);
                nameList.add(recipeName);
            }
        } catch (XPathException xpe) {
            this.logger.warn("Couldn't run XPATH: " + xpe.getMessage());
            return null;
        }
        return nameList;
    }

    public Recipe[] readRecipe(Document beerDocument, String name) throws XPathException {
        String recipeSelector = "";

        if (name != null) {
            recipeSelector = "[NAME[text()=\"" + name + "\"]]";
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList recipeData =
                (NodeList) xp.evaluate(
                        "/RECIPES/RECIPE" + recipeSelector,
                        beerDocument, XPathConstants.NODESET);

        Recipe recipeList[] = new Recipe[recipeData.getLength()];
        for (int i = 0; i < recipeData.getLength(); i++) {
            try {
                recipeList[i] = readSingleRecipe(recipeData.item(i));
            } catch (XPathException xpe) {
                this.logger.warn("Couldn't read the recipe at index "
                        + i + " - " + xpe.getMessage());
                xpe.printStackTrace();
            } catch (NumberFormatException nfe) {
                this.logger.warn("Couldn't read the recipe at index "
                        + i + " due to a bad number " + nfe.getMessage());
                nfe.printStackTrace();
            }
        }
        return recipeList;
    }

    public Recipe readSingleRecipe(Node recipeNode) throws XPathException, NumberFormatException {
        XPath xp = XPathFactory.newInstance().newXPath();
        Recipe recipe = new Recipe();
        recipe.allowRecalcs = false;

        // otherwise get the details from the recipe
        String recipeName = (String) xp.evaluate("NAME/text()", recipeNode, XPathConstants.STRING);
        String brewerName = (String) xp.evaluate("BREWER/text()", recipeNode, XPathConstants.STRING);
        String notes = (String) xp.evaluate("NOTES/text()", recipeNode, XPathConstants.STRING);

        double efficiency = getDouble(recipeNode, "EFFICIENCY", xp);
        double batchSize = getDouble(recipeNode, "BATCH_SIZE", xp);
        double boilSize = getDouble(recipeNode, "BOIL_SIZE", xp);
        int boilTime = getInteger(recipeNode, "BOIL_TIME", xp);
        String tasteNotes = getString(recipeNode, "TASTE_NOTES", xp);
        double tasteRating = getDouble(recipeNode, "TASTE_RATING", xp);
        double measuredOg = getDouble(recipeNode, "OG", xp);
        double measuredFg = getDouble(recipeNode, "FG", xp);
        int fermentationStages = getInteger(recipeNode, "FERMENTATION_STAGES", xp);
        int primaryAge = getInteger(recipeNode, "PRIMARY_AGE", xp);
        double primaryTemp = getDouble(recipeNode, "PRIMARY_TEMP", xp);
        int secondaryAge = getInteger(recipeNode, "SECONDARY_AGE", xp);
        double secondaryTemp = getDouble(recipeNode, "SECONDARY_TEMP", xp);
        int tertiaryAge = getInteger(recipeNode, "TERTIARY_AGE", xp);
        double tertiaryTemp = getDouble(recipeNode, "TERTIARY_TEMP", xp);
        int bottleAge = getInteger(recipeNode, "AGE", xp);
        double bottleAgeTemp = getDouble(recipeNode, "AGE_TEMP", xp);
        String dateBrewed = getString(recipeNode, "DATE", xp);
        double carbonation = getDouble(recipeNode, "CARBONATION", xp);
        boolean forcedCarbonation = getBoolean(recipeNode, "FORCED_CARBONATION", xp, false);
        String primingSugarName = getString(recipeNode, "PRIMING_SUGAR_NAME", xp);
        double carbonationTemp = getDouble(recipeNode, "CARBONATION_TEMP", xp);
        double primingSugarEquiv = getDouble(recipeNode, "PRIMING_SUGAR_EQUIV", xp);
        double kegPrimingFactor = getDouble(recipeNode, "KEG_PRIMING_FACTOR", xp);
        String displayPrimaryTemp = getString(recipeNode, "DISPLAY_PRIMARY_TEMP", xp);
        String displaySecondaryTemp = getString(recipeNode, "DISPLAY_SECONDARY_TEMP", xp);
        String displayTertiaryTemp = getString(recipeNode, "DISPLAY_TERTIARY_TEMP", xp);
        String displayAgeTemp = getString(recipeNode, "DISPLAY_AGE_TEMP", xp);

        recipe.setName(recipeName);
        recipe.setBrewer(brewerName);
        recipe.setBoilMinutes(boilTime);
        recipe.setEfficiency(efficiency);
        recipe.setComments(notes);
        recipe.setPreBoil(new Quantity(Quantity.LITRES, boilSize));
        recipe.setPreBoil(getString(recipeNode, "DISPLAY_BOIL_SIZE", xp));
        recipe.setPostBoil(new Quantity(Quantity.LITRES, batchSize));
        recipe.setPostBoil(getString(recipeNode, "DISPLAY_BATCH_SIZE", xp));
        recipe.setTasteNotes(tasteNotes);
        recipe.setTasteRating(tasteRating);
        recipe.setMeasuredOg(measuredOg);
        recipe.setMeasuredFg(measuredFg);
        // Add primary fermentation.
        if (primaryTemp != 0.0 && primaryAge != 0.0) {
            FermentStep primary = new FermentStep();
            primary.setType(FermentStep.PRIMARY);
            primary.setTemp(primaryTemp);
            primary.setTempU("C");
            if (displayPrimaryTemp != null && displayPrimaryTemp.trim().toUpperCase().endsWith("F")) {
                primary.convertTo("F");
            }
            primary.setTime(primaryAge);
            recipe.addFermentStep(primary);
        }
        // Add Secondary fermentation.
        if (secondaryTemp != 0.0 && secondaryAge != 0.0) {
            FermentStep secondary = new FermentStep();
            secondary.setType(FermentStep.SECONDARY);
            secondary.setTemp(secondaryTemp);
            secondary.setTempU("C");
            secondary.setTime(secondaryAge);
            if (displaySecondaryTemp != null && displaySecondaryTemp.trim().toUpperCase().endsWith("F")) {
                secondary.convertTo("F");
            }
            recipe.addFermentStep(secondary);
        }
        // Add clearing fermentation.
        if (tertiaryTemp != 0.0 && tertiaryAge != 0.0) {
            FermentStep tertiary = new FermentStep();
            tertiary.setType(FermentStep.CLEARING);
            tertiary.setTemp(tertiaryTemp);
            tertiary.setTempU("C");
            tertiary.setTime(tertiaryAge);
            if (displayTertiaryTemp != null && displayTertiaryTemp.trim().toUpperCase().endsWith("F")) {
                tertiary.convertTo("F");
            }
            recipe.addFermentStep(tertiary);
        }
        // Add aging fermentation.
        if (bottleAge != 0.0 && bottleAgeTemp != 0.0) {
            FermentStep aging = new FermentStep();
            aging.setType(FermentStep.AGEING);
            aging.setTemp(bottleAgeTemp);
            aging.setTempU("C");
            aging.setTime(bottleAge);
            if (displayAgeTemp != null && displayAgeTemp.trim().toUpperCase().endsWith("F")) {
                aging.convertTo("F");
            }
            recipe.addFermentStep(aging);
        }

        if (recipe.getFermentStepSize() != fermentationStages) {
            this.logger.warn("Fermentation Steps invalid! Expected to find: " + fermentationStages
                    + " but the recipe has: " + recipe.getFermentStepSize());
        }
        recipe.setDateBrewed(dateBrewed);
        recipe.setCarbTempU("C");
        recipe.setBottleTemp(carbonationTemp);
        recipe.setTargetVol(carbonation);
        recipe.setKegged(forcedCarbonation);
        recipe.setPrimeSugarName(primingSugarName);
        recipe.setPrimeSugarEquiv(primingSugarEquiv);
        recipe.setKegPrimingFactor(kegPrimingFactor);
        recipe.setIBUMethod(getString(recipeNode, "IBU_METHOD", xp));
        recipe.setCarbMethod(getString(recipeNode, "CARBONATION_USED", xp));

        NodeList hopsList = (NodeList) xp.evaluate("HOPS", recipeNode, XPathConstants.NODESET);
        if (hopsList != null && hopsList.getLength() > 0) {
            parseHops(recipe, hopsList);
        }
        NodeList maltList = (NodeList) xp.evaluate("FERMENTABLES", recipeNode, XPathConstants.NODESET);
        if (maltList != null && maltList.getLength() > 0) {
            parseMalts(recipe, maltList);
        }
        NodeList yeastList = (NodeList) xp.evaluate("YEASTS", recipeNode, XPathConstants.NODESET);
        if (yeastList != null && yeastList.getLength() > 0) {
            parseYeasts(recipe, yeastList);
        }
        NodeList styleList = (NodeList) xp.evaluate("STYLES", recipeNode, XPathConstants.NODESET);
        if (styleList != null && styleList.getLength() == 1) {
            parseStyle(recipe, styleList);
        } else {
            parseStyleDetail(recipe,
                    (Node) xp.evaluate("STYLE", recipeNode, XPathConstants.NODE));
        }
        NodeList miscList = (NodeList) xp.evaluate("MISCS", recipeNode, XPathConstants.NODESET);
        if (miscList != null && miscList.getLength() > 0) {
            parseMisc(recipe, miscList);
        }
        NodeList waterList = (NodeList) xp.evaluate("WATERS", recipeNode, XPathConstants.NODESET);
        if (waterList != null && waterList.getLength() > 0) {
            parseWaters(recipe, waterList);
        }
        Node equipmentList = (Node) xp.evaluate("EQUIPMENT", recipeNode, XPathConstants.NODE);
        if (equipmentList != null && equipmentList.hasChildNodes()) {
            parseEquipment(recipe, equipmentList, xp);
        }

        Node mashProfile = (Node) xp.evaluate("MASH", recipeNode, XPathConstants.NODE);
        if (mashProfile != null && mashProfile.hasChildNodes()) {
            parseMashProfile(recipe, mashProfile, xp);
        }
        recipe.setAllowRecalcs(true);
        recipe.calcMaltTotals();
        recipe.calcHopsTotals();
        recipe.calcFermentTotals();
        recipe.calcKegPSI();
        recipe.calcPrimeSugar();
        if (recipe.getMash() != null) {
            recipe.getMash().calcMashSchedule();
        }
        return recipe;
    }

    /**
     * Read in the hops.
     *
     * @param recipe The Recipe being used.
     * @param hops   The Hops NodeList
     * @throws XPathException        If there's an XPAth issue.
     * @throws NumberFormatException if there's a bad number
     */
    private void parseHops(Recipe recipe, NodeList hops)
            throws XPathException, NumberFormatException {
        if (hops == null || hops.getLength() == 0) {
            return;
        }
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList hopList = (NodeList) xp.evaluate("HOP", hops.item(0), XPathConstants.NODESET);

        for (int i = 0; i < hopList.getLength(); i++) {
            Node hop = hopList.item(i);

            // Get the values
            String name = getString(hop, "NAME", xp);
            double amount = getDouble(hop, "AMOUNT", xp);
            double alpha = getDouble(hop, "ALPHA", xp);

            int time = (int) Math.round(getDouble(hop, "TIME", xp));
            String use = getString(hop, "USE", xp);
            String displayAmount = getString(hop, "DISPLAY_AMOUNT", xp);
            String inventory = getString(hop, "INVENTORY", xp);
            String form = getString(hop, "FORM", xp);
            String type = getString(hop, "TYPE", xp);
            double beta = getDouble(hop, "BETA", xp);
            double hsi = getDouble(hop, "HSI", xp);
            String origin = getString(hop, "ORIGIN", xp);
            String substitutes = getString(hop, "SUBSTITUTES", xp);
            double humulene = getDouble(hop, "HUMULENE", xp);
            double caryophyllene = getDouble(hop, "CARYOPHYLLENE", xp);
            double cohumulone = getDouble(hop, "COHUMULONE", xp);
            double myrcene = getDouble(hop, "MYRCENE", xp);

            Hop hopObject = new Hop();
            hopObject.setName(name);
            hopObject.setAlpha(alpha);
            hopObject.setUnits(Quantity.KG);
            hopObject.setAmount(amount);
            hopObject.setType(type);
            hopObject.setForm(form);
            hopObject.setBeta(beta);
            hopObject.setHsi(hsi);
            hopObject.setOrigin(origin);
            hopObject.setSubstitutes(substitutes);
            hopObject.setHumulene(humulene);
            hopObject.setCaryophyllene(caryophyllene);
            hopObject.setCohumulone(cohumulone);
            hopObject.setMyrcene(myrcene);
            // Extensions
            if (displayAmount != null && !displayAmount.equals("")) {
                hopObject.setAmountAndUnits(displayAmount);
            }

            // Not all of these are used by beerxml 1.0
            if (use == null) {
                // Do nothing here
            } else if (use.equalsIgnoreCase("boil") || use.equalsIgnoreCase("aroma")
                    || use.equalsIgnoreCase("whirlpool")) {
                hopObject.setAdd(Hop.BOIL);
                hopObject.setMinutes(time);
            } else if (use.equalsIgnoreCase("dry hop")) {
                hopObject.setAdd(Hop.DRY);
                hopObject.setMinutes(time);
            } else if (use.equalsIgnoreCase("mash")) {
                hopObject.setAdd(Hop.MASH);
                hopObject.setMinutes(time);
            } else if (use.equalsIgnoreCase("first wort")) {
                hopObject.setAdd(Hop.FWH);
                hopObject.setMinutes(time);
            }

            if (inventory != null && !inventory.equals("")) {
                try {
                    hopObject.setInventory(inventory);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            // Everything is OK here, so add it in.
            recipe.addHop(hopObject);
        }
    }

    /**
     * Get the list of malts into the recipe object.
     *
     * @param recipe The recipe to add the malts to
     * @param malts  The NodeList of the malts.
     * @throws XPathException        If there's an XPath issue
     * @throws NumberFormatException If there's a bad number.
     */
    private void parseMalts(Recipe recipe, NodeList malts)
            throws XPathException, NumberFormatException {
        if (malts == null || malts.getLength() == 0) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList fermentableList = (NodeList) xp.evaluate("FERMENTABLE",
                malts.item(0), XPathConstants.NODESET);

        for (int i = 0; i < fermentableList.getLength(); i++) {
            try {
                Node fermentable = fermentableList.item(i);

                // Get the values
                String name = (String) xp.evaluate("NAME",
                        fermentable, XPathConstants.STRING);
                String type = (String) xp.evaluate("TYPE",
                        fermentable, XPathConstants.STRING);
                type = type.toLowerCase();
                boolean mashed = type.contains("malt")
                        || type.contains("grain");

                double amount = getDouble(fermentable, "AMOUNT", xp);
                double color = getDouble(fermentable, "COLOR", xp);
                double yield = getDouble(fermentable, "YIELD", xp);
                boolean addAfterBoil = getBoolean(fermentable, "ADD_AFTER_BOIL", xp, false);
                String notes = getString(fermentable, "NOTES", xp);
                String origin = getString(fermentable, "ORIGIN", xp);
                String supplier = getString(fermentable, "SUPPLIER", xp);
                double coarseFineDiff = getDouble(fermentable, "COARSE_FINE_DIFF", xp);
                double moisture = getDouble(fermentable, "MOISTURE", xp);
                double diastaticPower = getDouble(fermentable, "DIASTATIC_POWER", xp);
                double protein = getDouble(fermentable, "PROTEIN", xp);
                double maxInBatch = getDouble(fermentable, "MAX_IN_BATCH", xp);
                boolean recommendMash = getBoolean(fermentable, "RECOMMEND_MASH", xp, mashed);
                double ibuGalPerLb = getDouble(fermentable, "IBU_GAL_PER_LB", xp);
                String displayAmount = getString(fermentable, "DISPLAY_AMOUNT", xp);
                String inventory = getString(fermentable, "INVENTORY", xp);
                double potential = getDouble(fermentable, "POTENTIAL", xp);

                Fermentable malt = new Fermentable();
                malt.setName(name);
                malt.setPppg(yield);
                malt.setLov(color);
                malt.setMashed(mashed);
                malt.setAmount(amount);
                malt.setUnits(Quantity.KG);
                malt.setSteep(recommendMash);
                malt.addAfterBoil(addAfterBoil);
                malt.setOrigin(origin);
                malt.setSupplier(supplier);
                malt.setDescription(notes);
                malt.setCoarseFineDiff(coarseFineDiff);
                malt.setMoisture(moisture);
                malt.setDiastaticPower(diastaticPower);
                malt.setProtein(protein);
                malt.setMaxInBatch(maxInBatch);
                malt.setIbuGalPerLb(ibuGalPerLb);
                malt.setAmountAndUnits(displayAmount);
                malt.setInventory(inventory);
                malt.setPppg(potential);

                recipe.addMalt(malt);
            } catch (NumberFormatException nfe) {
                this.logger.warn("Couldn't parse a number: "
                        + nfe.getMessage());
            } catch (Exception e) {
                if (e instanceof XPathException) {
                    throw (XPathException) e;
                } else {
                    this.logger.warn(
                            "Couldn't read the weight for a malt" + e.getMessage());
                }
            }
        }
    }

    /**
     * Parse the yeasts.
     *
     * @param recipe The new Recipe.
     * @param yeasts The Yeast List.
     * @throws XPathException        If we couldn't find the a value we expect
     * @throws NumberFormatException If the yeast numbers are not numbers
     */
    private void parseYeasts(Recipe recipe, NodeList yeasts)
            throws XPathException, NumberFormatException {
        if (yeasts == null || yeasts.getLength() == 0) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList yeastList = (NodeList) xp.evaluate("YEAST", yeasts.item(0), XPathConstants.NODESET);

        for (int i = 0; i < yeastList.getLength(); i++) {
            try {
                Node yeastItem = yeastList.item(i);

                String name = getString(yeastItem, "NAME", xp);
                String type = getString(yeastItem, "TYPE", xp);
                String form = getString(yeastItem, "FORM", xp);
                double attenuation = getDouble(yeastItem, "ATTENUATION", xp);
                double amount = getDouble(yeastItem, "AMOUNT", xp);
                boolean amountIsWeight = getBoolean(yeastItem, "AMOUNT_IS_WEIGHT", xp, false);
                String laboratory = getString(yeastItem, "LABORATORY", xp);
                String productId = getString(yeastItem, "PRODUCT_ID", xp);
                double minTemperature = getDouble(yeastItem, "MIN_TEMPERATURE", xp);
                double maxTemperature = getDouble(yeastItem, "MAX_TEMPERATURE", xp);
                String flocculation = getString(yeastItem, "FLOCCULATION", xp);
                String notes = getString(yeastItem, "NOTES", xp);
                String bestFor = getString(yeastItem, "BEST_FOR", xp);
                int timesCultured = getInteger(yeastItem, "TIMES_CULTURED", xp);
                int maxReuse = getInteger(yeastItem, "MAX_REUSE", xp);
                boolean addToSecondary = getBoolean(yeastItem, "ADD_TO_SECONDARY", xp, false);
                String displayAmount = getString(yeastItem, "DISPLAY_AMOUNT", xp);
                String dispMinTemp = getString(yeastItem, "DISP_MIN_TEMP", xp);
                String dispMaxTemp = getString(yeastItem, "DISP_MAX_TEMP", xp);
                String inventory = getString(yeastItem, "INVENTORY", xp);
                String cultureDate = getString(yeastItem, "CULTURE_DATE", xp);

                Yeast yeast = new Yeast();
                yeast.setName(name);
                yeast.setForm(form);
                yeast.setType(type);
                yeast.setAttenuation(attenuation);
                recipe.setYeast(i, yeast);
                if (amountIsWeight) {
                    yeast.setUnits(Quantity.KG);
                    yeast.setAmount(amount);
                } else {
                    yeast.setUnits(Quantity.L);
                    yeast.setAmount(amount);
                }
                yeast.setLaboratory(laboratory);
                yeast.setProductId(productId);
                yeast.setMinTemperature(minTemperature);
                yeast.setMaxTemperature(maxTemperature);
                yeast.setMinTemperature(dispMinTemp);
                yeast.setMaxTemperature(dispMaxTemp);
                yeast.setFlocculation(flocculation);
                yeast.setDescription(notes);
                yeast.setBestFor(bestFor);
                yeast.setTimesCultured(timesCultured);
                yeast.setMaxReuse(maxReuse);
                yeast.addToSecondary(addToSecondary);
                yeast.setInventory(inventory);
                yeast.setAmountAndUnits(displayAmount);
                yeast.setCultureDate(cultureDate);
            } catch (NumberFormatException nfe) {
                this.logger.warn("Couldn't parse a number: "
                        + nfe.getMessage());
            } catch (Exception e) {
                this.logger.warn(e.getMessage());
            }
        }
    }

    private void parseStyle(Recipe recipe, NodeList styles) throws XPathExpressionException {
        if (styles == null) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList styleList = (NodeList) xp.evaluate("STYLE", styles.item(0), XPathConstants.NODESET);
        for (int i = 0; i < styleList.getLength(); i++) {
            parseStyleDetail(recipe, styleList.item(i));
        }
    }

    private void parseStyleDetail(Recipe recipe, Node style) throws XPathExpressionException {
        if (style == null) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        String name = (String) xp.evaluate("NAME", style, XPathConstants.STRING);
        String notes = (String) xp.evaluate("NOTES", style, XPathConstants.STRING);
        String categoryNumber = (String) xp.evaluate("CATEGORY_NUMBER", style, XPathConstants.STRING);
        String styleLetter = (String) xp.evaluate("STYLE_LETTER", style, XPathConstants.STRING);
        String styleGuide = (String) xp.evaluate("STYLE_GUIDE", style, XPathConstants.STRING);
        String type = (String) xp.evaluate("TYPE", style, XPathConstants.STRING);

        double ogMin = getDouble(style, "OG_MIN", xp);
        double ogMax = getDouble(style, "OG_MAX", xp);
        double fgMin = getDouble(style, "FG_MIN", xp);
        double fgMax = getDouble(style, "FG_MAX", xp);
        double ibuMin = getDouble(style, "IBU_MIN", xp);
        double ibuMax = getDouble(style, "IBU_MAX", xp);
        double colorMin = getDouble(style, "COLOR_MIN", xp);
        double colorMax = getDouble(style, "COLOR_MAX", xp);
        double abvMin = getDouble(style, "ABV_MIN", xp);
        double abvMax = getDouble(style, "ABV_MAX", xp);

        // Check to see if we have this style
        Style beerStyle = new Style();
        beerStyle.setName(name);
        beerStyle.setCategory(styleLetter);
        beerStyle.setCatNum(categoryNumber);
        beerStyle.setYear(styleGuide);
        beerStyle.setComments(notes);
        beerStyle.setType(type);
        beerStyle.setAlcHigh(abvMax);
        beerStyle.setAlcLow(abvMin);
        beerStyle.setFgHigh(fgMax);
        beerStyle.setFgLow(fgMin);
        beerStyle.setOgHigh(ogMax);
        beerStyle.setOgLow(ogMin);
        beerStyle.setSrmHigh(colorMax);
        beerStyle.setSrmLow(colorMin);
        beerStyle.setIbuHigh(ibuMax);
        beerStyle.setIbuLow(ibuMin);
        recipe.setStyle(beerStyle);
    }

    private void parseMisc(Recipe recipe, NodeList miscs) throws XPathExpressionException {
        if (miscs == null || miscs.getLength() == 0) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList miscList = (NodeList) xp.evaluate("MISC", miscs.item(0), XPathConstants.NODESET);

        for (int i = 0; i < miscList.getLength(); i++) {
            Node misc = miscList.item(i);
            String name = getString(misc, "NAME", xp);
            String notes = getString(misc, "NOTES", xp);
            String type = getString(misc, "TYPE", xp);
            String use = getString(misc, "USE", xp);
            int time = getInteger(misc, "TIME", xp);
            double amount = getDouble(misc, "AMOUNT", xp);
            boolean isWeight = getBoolean(misc, "AMOUNT_IS_WEIGHT", xp, false);
            String useFor = getString(misc, "USE_FOR", xp);

            Misc newMisc = new Misc();
            newMisc.setName(name);
            newMisc.setComments(notes);
            newMisc.setType(type);
            newMisc.setUse(use);
            newMisc.setTime(time);
            if (isWeight) {
                newMisc.setUnits(Quantity.KG);
                newMisc.setAmount(amount);
            } else {
                newMisc.setUnits(Quantity.LITRES);
                newMisc.setAmount(amount);
            }
            newMisc.setUseFor(useFor);
            recipe.addMisc(newMisc);
        }
    }

    public void parseWaters(Recipe recipe, NodeList waters) throws XPathExpressionException {
        if (waters == null || waters.getLength() == 0) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList waterList = (NodeList) xp.evaluate("WATER", waters.item(0), XPathConstants.NODESET);

        for (int i = 0; i < waterList.getLength(); i++) {
            Node water = waterList.item(i);
            parseWater(recipe, water, xp);
        }
    }

    private void parseWater(Recipe recipe, Node water, XPath xp) {
        String name = getString(water, "NAME", xp);
        double amount = getDouble(water, "AMOUNT", xp);
        double calcium = getDouble(water, "CALCIUM", xp);
        double bicarbonate = getDouble(water, "BICARBONATE", xp);
        double sulfate = getDouble(water, "SULFATE", xp);
        double chloride = getDouble(water, "CHLORIDE", xp);
        double sodium = getDouble(water, "SODIUM", xp);
        double magnesium = getDouble(water, "MAGNESIUM", xp);
        double ph = getDouble(water, "PH", xp);
        String displayAmount = getString(water, "DISPLAY_AMOUNT", xp);
        String notes = getString(water, "NOTES", xp);
        WaterProfile waterProfile = new WaterProfile(name);
        waterProfile.setCa(calcium);
        waterProfile.setCl(chloride);
        waterProfile.setSo4(sulfate);
        waterProfile.setNa(sodium);
        waterProfile.setHco3(bicarbonate);
        waterProfile.setMg(magnesium);
        waterProfile.setPh(ph);
        waterProfile.setNotes(notes);
        if (displayAmount != null) {
            waterProfile.setAmount(displayAmount);
        } else {
            waterProfile.setAmount(Double.toString(amount));
        }
        recipe.setTargetWater(waterProfile);
    }

    public void parseEquipments(Recipe recipe, NodeList equipments) throws XPathExpressionException {
        if (equipments == null || equipments.getLength() == 0) {
            return;
        }

        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList equipmentList = (NodeList) xp.evaluate("EQUIPMENT", equipments.item(0), XPathConstants.NODESET);

        for (int i = 0; i < equipmentList.getLength(); i++) {
            Node equipment = equipmentList.item(i);
            parseEquipment(recipe, equipment, xp);
        }
    }

    private void parseEquipment(Recipe recipe, Node equipment, XPath xp) {
        String name = getString(equipment, "NAME", xp);
        double boilSize = getDouble(equipment, "BOIL_SIZE", xp);
        double batchSize = getDouble(equipment, "BATCH_SIZE", xp);
        double tunVolume = getDouble(equipment, "TUN_VOLUME", xp);
        double tunWeight = getDouble(equipment, "TUN_WEIGHT", xp);
        double tunSpecificHeat = getDouble(equipment, "TUN_SPECIFIC_HEAT", xp);
        double topupWater = getDouble(equipment, "TOP_UP_WATER", xp);
        double trubChillerLoss = getDouble(equipment, "TRUB_CHILLER_LOSS", xp);
        double evapRate = getDouble(equipment, "EVAP_RATE", xp);
        double boilTime = getDouble(equipment, "BOIL_TIME", xp);
        boolean calcBoilVol = getBoolean(equipment, "CALC_BOIL_VOLUME", xp, true);
        double lauterDeadspace = getDouble(equipment, "LAUTER_DEADSPACE", xp);
        double topupKettle = getDouble(equipment, "TOP_UP_KETTLE", xp);
        double hopUtilization = getDouble(equipment, "HOP_UTILIZATION", xp);
        double chillPercent = getDouble(equipment, "COOLING_LOSS_PCT", xp);
        String notes = getString(equipment, "NOTES", xp);

        Equipment equipProfile = new Equipment();
        equipProfile.setName(name);
        equipProfile.setBoilSize(boilSize);
        equipProfile.setBatchSize(batchSize);
        equipProfile.setTunVolume(tunVolume);
        equipProfile.setTunWeight(tunWeight);
        equipProfile.setTunSpecificHeat(tunSpecificHeat);
        equipProfile.setTopupKettle(topupKettle);
        equipProfile.setTopupWater(topupWater);
        equipProfile.setTrubChillerLoss(trubChillerLoss);
        equipProfile.setEvapRate(evapRate);
        recipe.setEvapMethod("percent");
        equipProfile.setBoilTime(boilTime);
        equipProfile.setCalcBoilVol(calcBoilVol);
        equipProfile.setLauterDeadspace(lauterDeadspace);
        equipProfile.setHopUtilization(hopUtilization);
        equipProfile.setNotes(notes);
        equipProfile.setChillPercent(chillPercent / 100);
        recipe.setEquipmentProfile(equipProfile);
    }

    /**
     * Add a mash Profile to a recipe.
     *
     * @param recipe      The @{Recipe} object to add the mash profile to.
     * @param mashProfile The node containing the beerXML Mash element.
     * @param xp          an XPath object to use to run XPath expressions.
     * @throws XPathException If an XPath expression could not be run.
     */
    private void parseMashProfile(Recipe recipe, Node mashProfile, XPath xp) throws XPathException {
        String name = getString(mashProfile, "NAME", xp);
        double grainTemp = getDouble(mashProfile, "GRAIN_TEMP", xp);
        Node mashSteps = (Node) xp.evaluate("MASH_STEPS", mashProfile, XPathConstants.NODE);
        String notes = getString(mashProfile, "NOTES", xp);
        double tunTemp = getDouble(mashProfile, "TUN_TEMP", xp);
        double spargeTemp = getDouble(mashProfile, "SPARGE_TEMP", xp);
        double ph = getDouble(mashProfile, "PH", xp);
        double tunWeight = getDouble(mashProfile, "TUN_WEIGHT", xp);
        double tunSpecificHeat = getDouble(mashProfile, "TUN_SPECIFIC_HEAT", xp);
        boolean tunAdjust = getBoolean(mashProfile, "TUN_ADJUST", xp, false);

        Mash mash = recipe.getMash();
        if (mash == null) {
            mash = new Mash(name, recipe);
            recipe.setMash(mash);
        } else {
            mash.setName(name);
        }

        mash.setGrainTemp(grainTemp);
        mash.setNotes(notes);
        mash.setTunTemp(tunTemp);
        mash.setSpargeTemp(spargeTemp);
        mash.setPh(ph);
        mash.setTunWeight(tunWeight);
        mash.setTunSpecificHeat(tunSpecificHeat);
        mash.setTunAdjust(tunAdjust);
        mash.setTunWeight(getString(mashProfile, "DISPLAY_TUN_WEIGHT", xp));
        mash.setMashTempUnits(getString(mashProfile, "DISPLAY_GRAIN_TEMP", xp));

        parseMashSteps(mash, mashSteps, xp);
    }

    /**
     * Iterate a node containing mash steps to add them to a recipe.
     *
     * @param mash      The Mash object to add steps to.
     * @param mashSteps The Node containing multiple child MASH_STEP elements.
     * @param xp        An XPath object.
     * @throws XPathExpressionException If an XPath couldn't be run.
     */
    private void parseMashSteps(Mash mash, Node mashSteps, XPath xp) throws XPathExpressionException {
        if (mashSteps == null) {
            return;
        }

        NodeList stepList = (NodeList) xp.evaluate("MASH_STEP", mashSteps, XPathConstants.NODESET);
        for (int i = 0; i < stepList.getLength(); i++) {
            Node step = stepList.item(i);
            String name = getString(step, "NAME", xp);
            String type = getString(step, "TYPE", xp);
            double infuseAmount = getDouble(step, "INFUSE_AMOUNT", xp);
            double stepTemp = getDouble(step, "STEP_TEMP", xp);
            int stepTime = getInteger(step, "STEP_TIME", xp);
            int rampTime = getInteger(step, "RAMP_TIME", xp);
            double endTemp = getDouble(step, "END_TEMP", xp);

            // Add it in
            Mash.MashStep newStep = mash.addStep(name, stepTemp, endTemp, type, stepTime, rampTime, mash.getTotalMashLbs());
            newStep.setName(name);
            newStep.setInVol(infuseAmount);
            newStep.setDirections(getString(step, "DESCRIPTION", xp));
            if (type != null &&
                    (type.equals(Mash.DECOCTION) || type.equals(Mash.DECOCTION_THICK) || type.equals(Mash.DECOCTION_THIN))) {
                String decoctionAmount = getString(step, "DECOCTION_AMT", xp);
                newStep.setInVol(new Quantity(decoctionAmount));
            } else {
                String infuseTemp = getString(step, "INFUSE_TEMP", xp);
                newStep.setInfuseTemp(infuseTemp);
                if (infuseTemp != null && infuseTemp.endsWith("F")) {
                    newStep.convertTo("F");
                }
            }
            String waterRatio = getString(step, "WATER_GRAIN_RATIO", xp);
            if (waterRatio != null) {
                String[] mashRatio = waterRatio.split(" ");
                newStep.setMashRatio(mashRatio[0]);
                newStep.setMashRatioU(Mash.QT_PER_LB);
                if (mashRatio.length == 2) {
                    newStep.setMashRatioU(mashRatio[1]);
                }
            }
            String displayInfuseAmount = getString(step, "DISPLAY_INFUSE_AMT", xp);
            if (displayInfuseAmount != null && displayInfuseAmount.length() > 0) {
                newStep.setInVol(new Quantity(displayInfuseAmount));
            }

            String displayMashTemp = getString(step, "DISPLAY_STEP_TEMP", xp);
            if (displayMashTemp != null && !displayMashTemp.equals("") && displayMashTemp.endsWith("F")) {
                newStep.convertTo("F");
            }
        }
    }

    private String getString(Node element, String name, XPath xp) {
        try {
            return (String) xp.evaluate(name.toUpperCase(), element, XPathConstants.STRING);
        } catch (XPathException xpe) {
            return null;
        }
    }

    private double getDouble(Node element, String name, XPath xp) {
        try {
            String temp = getString(element, name, xp);
            if (temp == null || temp.equals("")) {
                return 0.0;
            }
            return Double.parseDouble(temp);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return 0.0;
        }
    }

    private int getInteger(Node element, String name, XPath xp) {
        try {
            String temp = getString(element, name, xp);
            if (temp == null || temp.equals("")) {
                return 0;
            }
            return (int) Double.parseDouble(temp);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
            return 0;
        }
    }

    private boolean getBoolean(Node element, String name, XPath xp, boolean defaultValue) {
        String inValue = getString(element, name, xp);
        if (inValue == null || inValue.equals("")) {
            return defaultValue;
        }
        return Boolean.parseBoolean(inValue);
    }

    public Recipe[] readAllRecipes() throws XPathException {
        return readRecipe(this.recipeDocument, null);
    }

    public Recipe readRecipe(String name) throws XPathException {
        Recipe[] recipes = readRecipe(this.recipeDocument, name);
        if (recipes != null && recipes.length > 0) {
            return recipes[0];
        }
        return null;
    }
}
