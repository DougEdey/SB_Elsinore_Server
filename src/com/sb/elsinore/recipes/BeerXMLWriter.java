package com.sb.elsinore.recipes;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sb.elsinore.BrewServer;

import ca.strangebrew.recipe.Fermentable;
import ca.strangebrew.recipe.Hop;
import ca.strangebrew.recipe.Quantity;
import ca.strangebrew.recipe.Recipe;
import ca.strangebrew.recipe.Style;
import ca.strangebrew.recipe.Yeast;

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
    String TAG = BeerXMLWriter.class.getName();

    public BeerXMLWriter(Recipe[] recipes) {
        this.recipes = recipes;
    }

    public int writeRecipes(File outputFile) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(outputFile);
        return writeRecipes(outputStream, false);
    }

    public int writeRecipes(OutputStream recipeOutputStream, boolean publish) throws IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            BrewServer.LOG.warning(
                "Couldn't create DocumentBuilderFactory when writing XML File.");
            e1.printStackTrace();
            return -1;
        }

        Document recipeDocument = null;
        XPath xp = null;
        // Create the Recipe Node
        recipeDocument = dBuilder.newDocument();
        Element recipesElement = recipeDocument.createElement("RECIPES");

        int success = 0;
        for (int i = 0; i < recipes.length; i++) {
            Recipe recipe = recipes[i];
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
        tElement.setTextContent("" + recipe.getPostBoilVol(Quantity.LITRES));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BOIL_SIZE");
        tElement.setTextContent("" + recipe.getPreBoilVol(Quantity.LITRES));
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("BOIL_TIME");
        tElement.setTextContent("" + recipe.getBoilMinutes());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("EFFICIENCY");
        tElement.setTextContent("" + recipe.getEfficiency());
        recipeElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent(recipe.getComments());
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
        tElement.setTextContent("" + hopAddition.getAlpha());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT");
        tElement.setTextContent(Double.toString(hopAddition.getAmountAs(
                Quantity.KILOGRAMS)));
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("USE");
        tElement.setTextContent(hopAddition.getAdd());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TIME");
        tElement.setTextContent("" + hopAddition.getMinutes());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("NOTES");
        tElement.setTextContent("" + hopAddition.getDescription());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent("" + hopAddition.getType());
        hopElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FORM");
        tElement.setTextContent("" + hopAddition.getType());
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
        tElement.setTextContent("" + yield);
        fermentableElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR");
        tElement.setTextContent("" + maltAddition.getLov());
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
        tElement.setTextContent("" + yeast.getAttenuation());
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("TYPE");
        tElement.setTextContent("ALE");
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FORM");
        tElement.setTextContent("DRY");
        yeastElement.appendChild(tElement);

        tElement = recipeDocument.createElement("AMOUNT");
        tElement.setTextContent("1");
        yeastElement.appendChild(tElement);

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
        tElement.setTextContent("" + style.getOgLow());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("OG_MAX");
        tElement.setTextContent("" + style.getOgHigh());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FG_MIN");
        tElement.setTextContent("" + style.getFgLow());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("FG_MAX");
        tElement.setTextContent("" + style.getFgHigh());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU_MIN");
        tElement.setTextContent("" + style.getIbuLow());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("IBU_MAX");
        tElement.setTextContent("" + style.getIbuHigh());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR_MIN");
        tElement.setTextContent("" + style.getSrmLow());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("COLOR_MAX");
        tElement.setTextContent("" + style.getSrmHigh());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ABV_MIN");
        tElement.setTextContent("" + style.getAlcLow());
        styleElement.appendChild(tElement);

        tElement = recipeDocument.createElement("ABV_MAX");
        tElement.setTextContent("" + style.getAlcHigh());
        styleElement.appendChild(tElement);

        return styleElement;
    }
}