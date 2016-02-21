package com.sb.elsinore.recipes;

import ca.strangebrew.recipe.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sb.elsinore.BrewServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Write a BeerXML 1.0 format file.
 * @author Doug Edey
 *
 */
public class BeerXMLWriter {

    Recipe[] recipes;
    //String TAG = BeerXMLWriter.class.getName();

    public BeerXMLWriter(Recipe[] recipes) {
        this.recipes = recipes;
    }

    public int writeRecipes(File outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        return writeRecipes(outputStream);
    }

    public int writeRecipes(OutputStream recipeOutputStream) throws IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            BrewServer.LOG.warning(
                "Couldn't create DocumentBuilderFactory when writing XML File.");
            e1.printStackTrace();
            return -1;
        }

        Document recipeDocument;
        XPath xp ;
        // Create the Recipe Node
        recipeDocument = dBuilder.newDocument();
        Element recipesElement = recipeDocument.createElement("RECIPES");

        int success = 0;
        for (Recipe recipe: recipes) {
            try {
                Element recipeElement = writeRecipe(recipe, recipeDocument);
                if (recipeElement != null) {
                    recipesElement.appendChild(recipeElement);
                }
                success++;
            } catch (IOException ioe) {
                BrewServer.LOG.info("Couldn't add recipe");
                ioe.printStackTrace();
            }
        }

        recipeDocument.appendChild(recipesElement);
        try {
            TransformerFactory transformerFactory = TransformerFactory
                    .newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(recipeDocument);

            xp = XPathFactory.newInstance().newXPath();
            NodeList nl = (NodeList) xp.evaluate(
                    "//text()[normalize-space(.)='']", recipeDocument,
                    XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); ++i) {
                Node node = nl.item(i);
                node.getParentNode().removeChild(node);
            }

            StreamResult configResult = new StreamResult(recipeOutputStream);
            transformer.transform(source, configResult);
        } catch (TransformerConfigurationException e) {
            BrewServer.LOG.info("Could not transform config file");
            e.printStackTrace();
        } catch (TransformerException e) {
            BrewServer.LOG.info("Could not transformer file");
            e.printStackTrace();
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        } finally {
            recipeOutputStream.close();
        }
        return success;
    }

    /**
     * Write the recipe to the XML Document.
     * @param recipe The recipe to output.
     * @param recipeDocument The XMLDocument to append the recipe to.
     * @return The Element that represents the recipe.
     * @throws IOException If an element could not be added.
     */
    public final Element writeRecipe(final Recipe recipe,
            final Document recipeDocument)
            throws IOException {
        Element recipeElement = recipeDocument.createElement("RECIPE");

        // Generic recipe stuff
        Element tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(recipe.getName());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent(recipe.getType());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BREWER");
        tElement.setTextContent(recipe.getBrewer());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BATCH_SIZE");
        tElement.setTextContent(String.format("%.6f", recipe.getPostBoilVol(Quantity.LITRES)));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BOIL_SIZE");
        tElement.setTextContent(String.format("%.6f", recipe.getPreBoilVol(Quantity.LITRES)));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BOIL_TIME");
        tElement.setTextContent(Integer.toString(recipe.getBoilMinutes()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EFFICIENCY");
        tElement.setTextContent(String.format("%.6f", recipe.getEfficiency()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(recipe.getComments());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TASTE_NOTES");
        tElement.setTextContent(recipe.getTasteNotes());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TASTE_RATING");
        tElement.setTextContent(String.format("%.6f", recipe.getTasteRating()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("OG");
        tElement.setTextContent(String.format("%.3f", recipe.getMeasuredOg()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FG");
        tElement.setTextContent(String.format("%.3f", recipe.getMeasuredFg()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FERMENTATION_STAGES");
        tElement.setTextContent(Integer.toString(recipe.getFermentStepSize()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PRIMARY_AGE");
        tElement.setTextContent(Integer.toString(recipe.getFermentStep(FermentStep.PRIMARY).getTime()));
        recipeElement.appendChild(tElement);
        tElement = recipeDocument.createElement("PRIMARY_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getFermentStep(FermentStep.PRIMARY).getTemp()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SECONDARY_AGE");
        tElement.setTextContent(Integer.toString(recipe.getFermentStep(FermentStep.SECONDARY).getTime()));
        recipeElement.appendChild(tElement);
        tElement = recipeDocument.createElement("SECONDARY_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getFermentStep(FermentStep.SECONDARY).getTemp()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TERTIARY_AGE");
        tElement.setTextContent(Integer.toString(recipe.getFermentStep(FermentStep.CLEARING).getTime()));
        recipeElement.appendChild(tElement);
        tElement = recipeDocument.createElement("TERIARY_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getFermentStep(FermentStep.CLEARING).getTemp()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AGE");
        tElement.setTextContent(Integer.toString(recipe.getFermentStep(FermentStep.AGEING).getTime()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AGE_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getFermentStep(FermentStep.AGEING).getTemp()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DATE");
        tElement.setTextContent(recipe.getDateBrewed());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CARBONATION");
        tElement.setTextContent(String.format("%.6f", recipe.getTargetVol()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FORCED_CARBONATION");
        tElement.setTextContent(Boolean.toString(recipe.isKegged()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PRIMING_SUGAR_NAME");
        tElement.setTextContent(recipe.getDateBrewed());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CARBONATION_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getBottleTemp()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PRIMING_SUGAR_EQUIV");
        tElement.setTextContent(String.format("%.6f", recipe.getPrimeSugarEquiv()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("KEG_PRIMING_FACTOR");
        tElement.setTextContent(String.format("%.6f", recipe.getKegPrimingFactor()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EST_OG");
        tElement.setTextContent(String.format("%.3f", recipe.getEstOg()));
        recipeElement.appendChild(tElement);
        tElement = recipeDocument.createElement("EST_FG");
        tElement.setTextContent(String.format("%.3f", recipe.getEstFg()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EST_COLOR");
        tElement.setTextContent(String.format("%.6f", recipe.getColour()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU");
        tElement.setTextContent(String.format("%.6f", recipe.getIbu()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU_METHOD");
        tElement.setTextContent(recipe.getIBUMethod());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EST_ABV");
        tElement.setTextContent(String.format("%.6f", recipe.getAlcohol()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ABV");
        tElement.setTextContent(String.format("%.6f", recipe.getMeasuredAlcohol()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ACTUAL_EFFICIENCY");
        tElement.setTextContent(String.format("%.6f", recipe.getMeasuredEfficiency()));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CALORIES");
        tElement.setTextContent(recipe.calcCalories());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_BATCH_SIZE");
        tElement.setTextContent(recipe.getPostBoilVol().toString());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_BOIL_SIZE");
        tElement.setTextContent(recipe.getPreBoilVol(recipe.getPostBoilVol().getUnits()) + " " + recipe.getPostBoilVol().getUnits());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_OG");
        tElement.setTextContent(recipe.getMeasuredOg() + recipe.getGravUnits());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_FG");
        tElement.setTextContent(recipe.getMeasuredFg() + recipe.getGravUnits());
        recipeElement.appendChild(tElement);

        FermentStep fs = recipe.getFermentStep(FermentStep.PRIMARY);
        tElement = recipeDocument.createElement("DISPLAY_PRIMARY_TEMP");
        tElement.setTextContent(String.format("%.6f", fs.getTemp()) + " " + fs.getTempU());
        recipeElement.appendChild(tElement);

        fs = recipe.getFermentStep(FermentStep.SECONDARY);
        tElement = recipeDocument.createElement("DISPLAY_SECONDARY_TEMP");
        tElement.setTextContent(String.format("%.6f", fs.getTemp()) + " " + fs.getTempU());
        recipeElement.appendChild(tElement);

        fs = recipe.getFermentStep(FermentStep.CLEARING);
        tElement = recipeDocument.createElement("DISPLAY_TERTIARY_TEMP");
        tElement.setTextContent(String.format("%.6f", fs.getTemp()) + " " + fs.getTempU());
        recipeElement.appendChild(tElement);

        fs = recipe.getFermentStep(FermentStep.AGEING);
        tElement = recipeDocument.createElement("DISPLAY_AGE_TEMP");
        tElement.setTextContent(String.format("%.6f", fs.getTemp()) + " " + fs.getTempU());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_CARB_TEMP");
        tElement.setTextContent(String.format("%.6f", recipe.getBottleTemp()) + " " + recipe.getCarbTempU());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CARBONATION_USED");
        tElement.setTextContent(recipe.getCarbMethod());
        recipeElement.appendChild(tElement);

        Element hopsElement = recipeDocument.createElement("HOPS");

        for (int i = 0; i < recipe.getHopsListSize(); i++) {
            Hop hopAddition = recipe.getHop(i);
            Element hopElement = createHopElement(hopAddition, recipeDocument);
            hopsElement.appendChild(hopElement);
        }

        recipeElement.appendChild(hopsElement);

        Element fermentablesElement =
                recipeDocument.createElement("FERMENTABLES");

        for (int i = 0; i < recipe.getMaltListSize(); i++) {
            Fermentable maltAddition = recipe.getFermentable(i);
            Element fermentableElement = createFermentableElement(maltAddition,
                    recipeDocument);
            fermentablesElement.appendChild(fermentableElement);
        }

        recipeElement.appendChild(fermentablesElement);

        Element yeastsElement = recipeDocument.createElement("YEASTS");

        for (Yeast yeast : recipe.getYeasts()) {
            Element yeastElement = createYeastElement(yeast, recipeDocument);
            yeastsElement.appendChild(yeastElement);
        }
        recipeElement.appendChild(yeastsElement);
        recipeElement.appendChild(createStyleElement(recipe.getStyleObj(),
                recipeDocument));

        Element miscElement = recipeDocument.createElement("MISCS");
        for (int i = 0; i < recipe.getMiscListSize(); i++) {
            Misc miscAddition = recipe.getMisc(i);
            miscElement.appendChild(miscAddition.createElement(recipeDocument));
        }
        recipeElement.appendChild(miscElement);

        Element watersElement = recipeDocument.createElement("WATERS");
        if (recipe.getTargetWater() != null) {
            watersElement.appendChild(recipe.getTargetWater().getElement(recipeDocument));
        }
        recipeElement.appendChild(watersElement);

        if (recipe.getEquipmentProfile() != null) {
            recipeElement.appendChild(this.createEquipmentProfile(recipe.getEquipmentProfile(), recipeDocument));
        }

        if (recipe.getMash() != null && recipe.getMash().getStepSize() > 0) {
            recipeElement.appendChild(this.createMashProfile(recipe.getMash(), recipeDocument));
        }
        return recipeElement;
    }

    /**
     * Create a hop element.
     * @param hopAddition The hop to create an element for.
     * @param recipeDocument The base document to create the element for.
     * @return The Hop XML Element.
     */
    private Element createHopElement(final Hop hopAddition,
            final Document recipeDocument) {
        Element hopElement = recipeDocument.createElement("HOP");

        Element tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(hopAddition.getName());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ALPHA");
        tElement.setTextContent(String.format("%.6f", hopAddition.getAlpha()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT");
        tElement.setTextContent(String.format("%.6f", hopAddition.getAmountAs(
                Quantity.KILOGRAMS)));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("USE");
        tElement.setTextContent(hopAddition.getAdd());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TIME");
        tElement.setTextContent(Integer.toString(hopAddition.getMinutes()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(hopAddition.getDescription());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent(hopAddition.getType());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FORM");
        tElement.setTextContent(hopAddition.getType());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BETA");
        tElement.setTextContent(String.format("%.6f", hopAddition.getBeta()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("HSI");
        tElement.setTextContent(String.format("%.6f", hopAddition.getHsi()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ORIGIN");
        tElement.setTextContent(hopAddition.getOrigin());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SUBSTITUTES");
        tElement.setTextContent(hopAddition.getSubstitutes());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("HUMULENE");
        tElement.setTextContent(String.format("%.6f", hopAddition.getHumulene()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CARYOPHYLLENE");
        tElement.setTextContent(String.format("%.6f", hopAddition.getCaryophyllene()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COHUMULONE");
        tElement.setTextContent(String.format("%.6f", hopAddition.getCohumulone()));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("MYRCENE");
        tElement.setTextContent(String.format("%.6f", hopAddition.getMyrcene()));
        hopElement.appendChild(tElement);

        // Add Extensions
        tElement = recipeDocument.createElement("DISPLAY_AMOUNT");
        tElement.setTextContent(hopAddition.getAmount().toString());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("INVENTORY");
        tElement.setTextContent(hopAddition.getInventory().toString());
        hopElement.appendChild(tElement);

        String unit = "min";
        double hopTime = (double) hopAddition.getMinutes();
        double daysDivider = 60 * 24;
        if (hopTime > daysDivider) {
            unit = "days";
            hopTime = hopTime / daysDivider;
        }

        tElement = recipeDocument.createElement("DISPLAY_TIME");
        tElement.setTextContent(hopTime + " " + unit);
        hopElement.appendChild(tElement);

        return hopElement;
    }

    /**
     * Create a new Fermentable Element.
     * @param maltAddition The Fermentable to add.
     * @param recipeDocument The Base document.
     * @return The Fermentable Element.
     */
    private Element createFermentableElement(final Fermentable maltAddition,
            final Document recipeDocument) {
        Element fermentableElement = recipeDocument.createElement(
                "FERMENTABLE");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(maltAddition.getName());
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        if (maltAddition.getMashed()) {
            tElement.setTextContent("Grain");
        } else {
            tElement.setTextContent("Extract");
        }
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT");
        tElement.setTextContent(Double.toString(
                maltAddition.getAmountAs(Quantity.KILOGRAMS)));
        fermentableElement.appendChild(tElement);

        double gravity = maltAddition.getPppg();
        double yield = ((gravity - 1) / (1.046 - 1)) * 100;
        tElement = recipeDocument.createElement("YIELD");
        tElement.setTextContent(String.format("%.0f", yield));
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR");
        tElement.setTextContent(String.format("%.6f", maltAddition.getLov()));
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ADD_AFTER_BOIL");
        tElement.setTextContent(Boolean.toString(maltAddition.addAfterBoil()));
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(maltAddition.getDescription());
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ORIGIN");
        tElement.setTextContent(maltAddition.getOrigin());
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SUPPLIER");
        tElement.setTextContent(maltAddition.getSupplier());
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COARSE_FINE_DIFF");
        tElement.setTextContent(String.format("%.6f", maltAddition.getCoarseFineDiff()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("MOISTURE");
        tElement.setTextContent(String.format("%.6f", maltAddition.getMoisture()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("DIASTATIC_POWER");
        tElement.setTextContent(String.format("%.6f", maltAddition.getDiastaticPower()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("PROTEIN");
        tElement.setTextContent(String.format("%.6f", maltAddition.getProtein()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("MAX_IN_BATCH");
        tElement.setTextContent(String.format("%.6f", maltAddition.getMaxInBatch()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("RECOMMEND_MASH");
        tElement.setTextContent(Boolean.toString(maltAddition.getSteep()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("IBU_GAL_PER_LB");
        tElement.setTextContent(String.format("%.6f", maltAddition.getIbuGalPerLb()));
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("DISPLAY_AMOUNT");
        tElement.setTextContent(maltAddition.getAmount().toString());
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("INVENTORY");
        tElement.setTextContent(maltAddition.getInventory().toString());
        fermentableElement.appendChild(tElement);
        tElement = recipeDocument.createElement("POTENTIAL");
        tElement.setTextContent(String.format("%.6f", maltAddition.getPppg()));
        fermentableElement.appendChild(tElement);

        return fermentableElement;
    }

    /**
     * Create a yeast Element.
     * @param yeast The yeast object to make into an XML Element.
     * @param recipeDocument The Document to create elements for.
     * @return The Yeast Object as an XML Element.
     */
    private Element createYeastElement(final Yeast yeast,
            final Document recipeDocument) {
        Element yeastElement = recipeDocument.createElement("YEAST");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(yeast.getName());
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ATTENUATION");
        tElement.setTextContent(String.format("%.6f", yeast.getAttenuation()));
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent("ALE");
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FORM");
        tElement.setTextContent("DRY");
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT");
        tElement.setTextContent(String.format("%.6f", yeast.getAmount().getValue()));
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT_IS_WEIGHT");
        tElement.setTextContent(Boolean.toString(yeast.getAmount().getUnits().equals(Quantity.KG)));
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("LABORATORY");
        tElement.setTextContent(yeast.getLaboratory());
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PRODUCT_ID");
        tElement.setTextContent(yeast.getProductId());
        yeastElement.appendChild(tElement);

        if (yeast.getMinTemperature() != 0.0) {
            tElement = recipeDocument.createElement("MIN_TEMPERATURE");
            tElement.setTextContent(String.format("%.6f", yeast.getMinTemperature()));
            yeastElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DISP_MIN_TEMP");
            tElement.setTextContent(yeast.getMinTemperatureString());
            yeastElement.appendChild(tElement);
        }

        if (yeast.getMaxTemperature() != 0.0) {
            tElement = recipeDocument.createElement("MAX_TEMPERATURE");
            tElement.setTextContent(String.format("%.6f", yeast.getMaxTemperature()));
            yeastElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DISP_MAX_TEMP");
            tElement.setTextContent(yeast.getMaxTemperatureString());
            yeastElement.appendChild(tElement);
        }

        if (yeast.getFlocculation() != null) {
            tElement = recipeDocument.createElement("FLOCCULATION");
            tElement.setTextContent(yeast.getFlocculation());
            yeastElement.appendChild(tElement);
        }

        if (yeast.getDescription() != null) {
            tElement = recipeDocument.createElement("NOTES");
            tElement.setTextContent(yeast.getDescription());
            yeastElement.appendChild(tElement);
        }

        if (yeast.getBestFor() != null) {
            tElement = recipeDocument.createElement("BEST_FOR");
            tElement.setTextContent(yeast.getBestFor());
            yeastElement.appendChild(tElement);
        }

        if (yeast.getTimesCultured() > 0) {
            tElement = recipeDocument.createElement("TIMES_CULTURED");
            tElement.setTextContent(String.format("%.6d", yeast.getTimesCultured()));
            yeastElement.appendChild(tElement);
        }

        if (yeast.getMaxReuse() > 0) {
            tElement = recipeDocument.createElement("MAX_REUSE");
            tElement.setTextContent(Integer.toString(yeast.getMaxReuse()));
            yeastElement.appendChild(tElement);
        }
        
        tElement = recipeDocument.createElement("ADD_TO_SECONDARY");
        tElement.setTextContent(Boolean.toString(yeast.isAddToSecondary()));
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_AMOUNT");
        tElement.setTextContent(yeast.getAmount().toString());
        yeastElement.appendChild(tElement);


        if (!yeast.getInventory().toString().equals("")) {
            tElement = recipeDocument.createElement("INVENTORY");
            tElement.setTextContent(yeast.getInventory().toString());
            yeastElement.appendChild(tElement);
        }

        if (!yeast.getCultureDate().equals("")) {
            tElement = recipeDocument.createElement("CULTURE_DATE");
            tElement.setTextContent(yeast.getCultureDate());
            yeastElement.appendChild(tElement);
        }
        return yeastElement;
    }

    private Element createStyleElement(Style style, Document recipeDocument) {
        Element styleElement = recipeDocument.createElement("STYLE");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(style.getName());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CATEGORY");
        tElement.setTextContent(style.getCategory());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CATEGORY_NUMBER");
        tElement.setTextContent(style.getCatNum());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("STYLE_GUIDE");
        tElement.setTextContent(style.getYear());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent(style.getType());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("OG_MIN");
        tElement.setTextContent(String.format("%.6f", style.getOgLow()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("OG_MAX");
        tElement.setTextContent(String.format("%.6f", style.getOgHigh()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FG_MIN");
        tElement.setTextContent(String.format("%.6f", style.getFgLow()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FG_MAX");
        tElement.setTextContent(String.format("%.6f", style.getFgHigh()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU_MIN");
        tElement.setTextContent(String.format("%.6f", style.getIbuLow()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU_MAX");
        tElement.setTextContent(String.format("%.6f", style.getIbuHigh()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR_MIN");
        tElement.setTextContent(String.format("%.6f", style.getSrmLow()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR_MAX");
        tElement.setTextContent(String.format("%.6f", style.getSrmHigh()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ABV_MIN");
        tElement.setTextContent(String.format("%.6f", style.getAlcLow()));
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ABV_MAX");
        tElement.setTextContent(String.format("%.6f", style.getAlcHigh()));
        styleElement.appendChild(tElement);

        return styleElement;
    }

    private Element createEquipmentProfile(Equipment equipment, Document recipeDocument) {
        Element equipElement = recipeDocument.createElement("EQUIPMENT");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(equipment.getName());
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BOIL_SIZE");
        tElement.setTextContent(Double.toString(equipment.getBoilSize().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BATCH_SIZE");
        tElement.setTextContent(Double.toString(equipment.getBatchSize().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_VOLUME");
        tElement.setTextContent(Double.toString(equipment.getTunVolume().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_WEIGHT");
        tElement.setTextContent(Double.toString(equipment.getTunWeight().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_SPECIFIC_HEAT");
        tElement.setTextContent(Double.toString(equipment.getTunSpecificHeat()));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TRUB_CHILLER_LOSS");
        tElement.setTextContent(Double.toString(equipment.getTrubChillerLoss().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("CALC_BOIL_VOLUME");
        tElement.setTextContent(Boolean.toString(equipment.isCalcBoilVol()));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("LAUTER_DEADSPACE");
        tElement.setTextContent(Double.toString(equipment.getLauterDeadspace().getValueAs(Quantity.L)));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TOP_UP_KETTLE");
        tElement.setTextContent(Double.toString(equipment.getTopupKettle()));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("HOP_UTILIZATION");
        tElement.setTextContent(Double.toString(equipment.getHopUtilization()));
        equipElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(equipment.getNotes());
        equipElement.appendChild(tElement);

        return equipElement;
    }

    private Element createMashProfile(Mash mash, Document recipeDocument) {
        Element mashElement = recipeDocument.createElement("MASH");

        Element tElement = recipeDocument.createElement("VERSION");
        tElement.setTextContent("1");
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NAME");
        tElement.setTextContent(mash.getName());
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("GRAIN_TEMP");
        tElement.setTextContent(Double.toString(mash.getGrainTemp()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(mash.getNotes());
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_TEMP");
        tElement.setTextContent(Double.toString(mash.getTunTemp()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("SPARGE_TEMP");
        tElement.setTextContent(Double.toString(mash.getSpargeTemp()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("PH");
        tElement.setTextContent(Double.toString(mash.getPh()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_WEIGHT");
        tElement.setTextContent(Double.toString(mash.getTunWeight().getValueAs(Quantity.KG)));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TUN_SPECIFIC_HEAT");
        tElement.setTextContent(Double.toString(mash.getTunSpecificHeat()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EQUIP_ADJUST");
        tElement.setTextContent(Boolean.toString(mash.isTunAdjust()));
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_GRAIN_TEMP");
        String tempString;
        if (mash.getMashTempUnits().equalsIgnoreCase("F")) {
            tempString = Double.toString(BrewCalcs.cToF(mash.getGrainTemp()));
        } else {
            tempString = Double.toString(mash.getGrainTemp());
        }
        tElement.setTextContent(tempString + " " + mash.getMashTempUnits());
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_TUN_TEMP");
        if (mash.getMashTempUnits().equalsIgnoreCase("F")) {
            tempString = Double.toString(BrewCalcs.cToF(mash.getTunTemp()));
        } else {
            tempString = Double.toString(mash.getTunTemp());
        }
        tElement.setTextContent(tempString + " " + mash.getMashTempUnits());
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_SPARGE_TEMP");
        if (mash.getMashTempUnits().equalsIgnoreCase("F")) {
            tempString = Double.toString(BrewCalcs.cToF(mash.getSpargeTemp()));
        } else {
            tempString = Double.toString(mash.getSpargeTemp());
        }
        tElement.setTextContent(tempString + " " + mash.getMashTempUnits());
        mashElement.appendChild(tElement);

        tElement = recipeDocument.createElement("DISPLAY_TUN_WEIGHT");
        tElement.setTextContent(mash.getTunWeight().toString());
        mashElement.appendChild(tElement);

        Element stepsElement = recipeDocument.createElement("MASH_STEPS");
        for (int i = 0; i < mash.getStepSize(); i++) {
            Element stepElement = recipeDocument.createElement("MASH_STEP");

            tElement = recipeDocument.createElement("NAME");
            tElement.setTextContent(mash.getStepName(i));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("TYPE");
            tElement.setTextContent(mash.getStepType(i));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("INFUSE_AMOUNT");
            if (!mash.getStepMethod(i).startsWith(Mash.DECOCTION)) {
                tElement.setTextContent(Double.toString(mash.getStepInQuantity(i).getValueAs(Quantity.LITRES)));
            }
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("STEP_TEMP");
            tElement.setTextContent(Double.toString(mash.getStepStartTemp(i)));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("STEP_TIME");
            tElement.setTextContent(Integer.toString(mash.getStepMin(i)));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("RAMP_TIME");
            tElement.setTextContent(Integer.toString(mash.getStepRampMin(i)));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("END_TEMP");
            tElement.setTextContent(Double.toString(mash.getStepEndTemp(i)));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DESCRIPTION");
            tElement.setTextContent(mash.getStepDirections(i));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("WATER_GRAIN_RATIO");
            tElement.setTextContent(mash.getMashRatio(i) + " " + mash.getMashRatioU(i));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DECOCTION_AMOUNT");
            if (mash.getStepMethod(i).startsWith(Mash.DECOCTION)) {
                tElement.setTextContent(mash.getStepInQuantity(i).toString());
            }
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("INFUSE_TEMP");
            if (!mash.getStepMethod(i).startsWith(Mash.DECOCTION)) {
                tElement.setTextContent(mash.getStepInfuseTemp(i));
            }
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DISPLAY_STEP_TEMP");
            tElement.setTextContent(mash.getDisplayStepStartTemp(i));
            stepElement.appendChild(tElement);

            tElement = recipeDocument.createElement("DISPLAY_INFUSE_AMT");
            tElement.setTextContent(mash.getStepInQuantity(i).toString());
            stepElement.appendChild(tElement);

            stepsElement.appendChild(stepElement);
        }

        mashElement.appendChild(stepsElement);
        return mashElement;
    }
}