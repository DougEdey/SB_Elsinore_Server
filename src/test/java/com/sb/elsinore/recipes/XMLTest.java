package com.sb.elsinore.recipes;

import ca.strangebrew.recipe.Quantity;
import ca.strangebrew.recipe.Recipe;
import org.junit.Test;

import javax.validation.constraints.NotNull;
import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Test the XML Read and write for recipes
 * Created by doug on 03/02/15.
 */
public class XMLTest {

    @Test
    public void testYellowSnow() {
        String yellowSnowRecipePath = "recipes/YellowSnow.xml";
        File yellowSnowFile = new File(yellowSnowRecipePath);

        assertTrue(yellowSnowFile.exists());
        assertTrue(yellowSnowFile.isFile());
        assertTrue(yellowSnowFile.canRead());
        BeerXMLReader beerXMLReader = BeerXMLReader.getInstance();
        assertTrue(beerXMLReader.readFile(yellowSnowFile));

        ArrayList<String> recipeArrayList = beerXMLReader.getListOfRecipes();
        assertNotNull(recipeArrayList);
        assertEquals(1, recipeArrayList.size());

        Recipe[] readRecipe = null;
        try {
            readRecipe = beerXMLReader.readAllRecipes();
        } catch (XPathException e) {
            e.printStackTrace();
            fail("Failed to read recipes");
        }

        assertNotNull(readRecipe);
        assertEquals(readRecipe.length, 1);
        Recipe yellowSnowRecipe = readRecipe[0];

        assertEquals("Yellow Snow Clone", yellowSnowRecipe.getName());
        assertEquals("Marc Sloan", yellowSnowRecipe.getBrewer());
        assertEquals(16.6889186, yellowSnowRecipe.getEvap(), 0.01);
        assertEquals(60, yellowSnowRecipe.getBoilMinutes());
        assertEquals(0.04, yellowSnowRecipe.getEquipmentProfile().getChillPercent(), 0.001);
        assertEquals("0.50", String.format("%.2f", yellowSnowRecipe.getEquipmentProfile().getTrubChillerLoss().getValueAs(Quantity.GALLONS_US)));
        assertEquals("0.50", String.format("%.2f", yellowSnowRecipe.getEquipmentProfile().getLauterDeadspace().getValueAs(Quantity.GALLONS_US)));
        assertEquals("percent", yellowSnowRecipe.getEvapMethod().toLowerCase());
        assertEquals("5.50", String.format("%.2f", yellowSnowRecipe.getPostBoilVol().getValueAs(Quantity.GAL)));
        assertTrue(7.47 <= yellowSnowRecipe.getPreBoilVol(Quantity.GAL));
        assertTrue(7.52 >= yellowSnowRecipe.getPreBoilVol(Quantity.GAL));
        assertEquals("72.0", "" + yellowSnowRecipe.getEfficiency());
        assertEquals(9, yellowSnowRecipe.getHopsListSize());
        assertEquals(3, yellowSnowRecipe.getMaltListSize());
        assertEquals(1, yellowSnowRecipe.getYeasts().size());
        assertEquals("28.3528977 l", yellowSnowRecipe.getEquipmentProfile().getBoilSize().toString());
        assertEquals("1.061", String.format("%.3f", yellowSnowRecipe.getEstOg()));
        assertEquals("1.014", String.format("%.3f", yellowSnowRecipe.getEstFg()));
        assertEquals("1.046", String.format("%.3f", yellowSnowRecipe.getMeasuredOg()));
        assertEquals("1.010", String.format("%.3f", yellowSnowRecipe.getMeasuredFg()));

        Recipe[] output = new Recipe[1];
        output[0] = yellowSnowRecipe;
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/yellowsnowoutput.xml"));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Shouldn't fail to write the file");
        }

        File outputFile = new File("recipes/yellowsnowoutput.xml");
        assertTrue("Output file doesn't exist!", outputFile.exists());
        assertTrue("Failed to delete test file!", outputFile.delete());
    }

    @Test
    public void testGreenhouseIPA() {
        File greenhouseFile = new File("recipes/GreenHouseIPA.xml");
        assertTrue(greenhouseFile.exists());
        assertTrue(greenhouseFile.canRead());
        assertTrue(BeerXMLReader.getInstance().readFile(greenhouseFile));
        Recipe[] recipes = getRecipes();

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/greenhouseoutput.xml"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to write file");
        }
        File outputFile = new File("recipes/greenhouseoutput.xml");
        assertTrue("Output file doesn't exist!", outputFile.exists());
        assertTrue("Failed to delete test file!", outputFile.delete());
    }

    @NotNull
    private Recipe[] getRecipes() {
        Recipe[] recipes = new Recipe[0];

        try {
            recipes = BeerXMLReader.getInstance().readAllRecipes();
            assertEquals(recipes.length, 1);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to read file");
        }
        return recipes;
    }

    @Test
    public void testRobertsMild() {
        File robertsMildFile = new File("recipes/RobertsMild.xml");
        assertTrue(robertsMildFile.exists());
        assertTrue(robertsMildFile.canRead());
        assertTrue(BeerXMLReader.getInstance().readFile(robertsMildFile));
        Recipe[] recipes = getRecipes();

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/robertsmildoutput.xml"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to write file");
        }
        File outputFile = new File("recipes/robertsmildoutput.xml");
        assertTrue("Output file doesn't exist! " + outputFile.getAbsolutePath(), outputFile.exists());
        assertTrue("Failed to delete test file! " + outputFile.getAbsolutePath(), outputFile.delete());
    }

    @Test
    public void testBarrelAgedStout() {
        File barrelAgedStoutFile = new File("recipes/barrel-aged-stout.xml");

        assertTrue(barrelAgedStoutFile.exists());
        assertTrue(barrelAgedStoutFile.canRead());
        assertTrue(BeerXMLReader.getInstance().readFile(barrelAgedStoutFile));

        Recipe[] recipes = getRecipes();

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/barrelagedstoutoutput.xml"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to write file");
        }
        File outputFile = new File("recipes/barrelagedstoutoutput.xml");
        assertTrue("Output file doesn't exist!", outputFile.exists());
        assertTrue("Failed to delete test file!", outputFile.delete());
    }
}
