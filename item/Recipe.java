package minicraft.item;

import java.util.Map;

/**
 * Defines a crafting transformation.
 */
public class Recipe {
    public enum Category { TOOLS, ARMOR, BLOCKS, SURVIVAL }

    private final String name;
    private final Map<Item, Integer> ingredients;
    private final Item result;
    private final int resultCount;
    private final Category category;

    public Recipe(String name, Category category, Map<Item, Integer> ingredients, Item result, int resultCount) {
        this.name = name;
        this.category = category;
        this.ingredients = ingredients;
        this.result = result;
        this.resultCount = resultCount;
    }

    public String getName() { return name; }
    public Category getCategory() { return category; }
    public Map<Item, Integer> getIngredients() { return ingredients; }
    public Item getResult() { return result; }
    public int getResultCount() { return resultCount; }
}
