import ca.strangebrew.recipe.Quantity;
import ca.strangebrew.recipe.Recipe;
import com.sb.elsinore.recipes.BeerXMLReader;
import com.sb.elsinore.recipes.BeerXMLWriter;
import org.junit.Test;

import javax.xml.xpath.XPathException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * Created by doug on 03/02/15.
 */
public class XMLTest {

    @Test
    public void readXMLFile() {
        testYellowSnow();
        testGreenhouseIPA();
        testBarrelAgedStout();
        testRobertsMild();
    }

    public void testYellowSnow() {
        String yellowSnowRecipePath = "recipes/YellowSnow.xml";
        File yellowSnowFile = new File(yellowSnowRecipePath);

        assert(yellowSnowFile.exists());
        assert(yellowSnowFile.isFile());
        assert(yellowSnowFile.canRead());
        BeerXMLReader beerXMLReader = BeerXMLReader.getInstance();
        assert(beerXMLReader.readFile(yellowSnowFile));

        ArrayList<String> recipeArrayList = beerXMLReader.getListOfRecipes();
        assert (recipeArrayList.size() == 1);

        System.out.println("Read file");
        Recipe[] readRecipe = null;
        try {
            readRecipe = beerXMLReader.readAllRecipes();
        } catch (XPathException e) {
            e.printStackTrace();
            assert(false);
        }

        assertNotSame(readRecipe, null);
        assertEquals(readRecipe.length, 1);
        Recipe yellowSnowRecipe = readRecipe[0];

        assertEquals("Yellow Snow Clone", yellowSnowRecipe.getName());
        assertEquals("Marc Sloan", yellowSnowRecipe.getBrewer());
        assertEquals(16.6889186, yellowSnowRecipe.getEvap());
        assertEquals(60, yellowSnowRecipe.getBoilMinutes());
        assertEquals(0.04, yellowSnowRecipe.getEquipmentProfile().getChillPercent());
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
            assert(true);
        }
    }

    public void testGreenhouseIPA() {
        File greenhouseFile = new File("recipes/GreenHouseIPA.xml");
        assert(greenhouseFile.exists());
        assert(greenhouseFile.canRead());
        assert(BeerXMLReader.getInstance().readFile(greenhouseFile));
        Recipe[] recipes = null;
        try {
            recipes = BeerXMLReader.getInstance().readAllRecipes();
            assertEquals(recipes.length, 1);
        } catch (XPathException e) {
            e.printStackTrace();
            assert(true);
        }

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/greenhouseoutput.xml"));
        } catch (IOException e) {
            e.printStackTrace();
            assert(true);
        }
    }

    public void testRobertsMild() {
        File robertsMildFile = new File("recipes/RobertsMild.xml");
        assert(robertsMildFile.exists());
        assert(robertsMildFile.canRead());
        assert(BeerXMLReader.getInstance().readFile(robertsMildFile));
        Recipe[] recipes = null;
        try {
            recipes = BeerXMLReader.getInstance().readAllRecipes();
            assertEquals(recipes.length, 1);
        } catch (XPathException e) {
            e.printStackTrace();
            assert(true);
        }

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/robertsmildoutput.xml"));
        } catch (IOException e) {
            e.printStackTrace();
            assert(true);
        }
    }

    public void testBarrelAgedStout() {
        File barrelAgedStoutFile = new File("recipes/barrel-aged-stout.xml");
        assert(barrelAgedStoutFile.exists());
        assert(barrelAgedStoutFile.canRead());
        assert(BeerXMLReader.getInstance().readFile(barrelAgedStoutFile));
        Recipe[] recipes = null;
        try {
            recipes = BeerXMLReader.getInstance().readAllRecipes();
            assertEquals(recipes.length, 1);
        } catch (XPathException e) {
            e.printStackTrace();
            assert(true);
        }

        Recipe[] output = new Recipe[1];
        output[0] = recipes[0];
        BeerXMLWriter recipeWriter = new BeerXMLWriter(output);
        try {
            recipeWriter.writeRecipes(new File("recipes/barrelagedstoutoutput.xml"));
        } catch (IOException e) {
            e.printStackTrace();
            assert(true);
        }
    }
}
