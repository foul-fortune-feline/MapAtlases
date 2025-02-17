package pepjebs.mapatlases.recipe;

import com.google.common.primitives.Ints;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.*;

public class MapAtlasesAddRecipe extends SpecialCraftingRecipe {

    private World world = null;

    public MapAtlasesAddRecipe(Identifier id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        this.world = world;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils
                .getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(itemStacks).copy();

        // Ensure there's an Atlas
        if (atlas.isEmpty()) {
            return false;
        }
        MapState sampleMap = MapAtlasesAccessUtils.getFirstMapStateFromAtlas(world, atlas);

        // Ensure only correct ingredients are present
        List<Item> additems = new ArrayList<>(Arrays.asList(Items.FILLED_MAP, MapAtlasesMod.MAP_ATLAS));
        if (MapAtlasesMod.CONFIG == null || MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill)
            additems.add(Items.MAP);
        if (!(itemStacks.size() > 1 && MapAtlasesAccessUtils.isListOnlyIngredients(
                itemStacks,
                additems))) {
            return false;
        }
        List<MapState> mapStates = MapAtlasesAccessUtils.getMapStatesFromItemStacks(world, itemStacks);

        // Ensure we're not trying to add too many Maps
        int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(atlas);
        int mapCount = MapAtlasesAccessUtils.getMapCountFromItemStack(atlas);
        if (empties + mapCount + itemStacks.size() - 1 > MapAtlasItem.getMaxMapCount()) {
            return false;
        }

        // Ensure Filled Maps are all same Scale & Dimension
        if(!(MapAtlasesAccessUtils.areMapsSameScale(sampleMap, mapStates) &&
                MapAtlasesAccessUtils.areMapsSameDimension(sampleMap, mapStates))) return false;

        // Ensure there's only one Atlas
        long atlasCount = itemStacks.stream().filter(i ->
                i.isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))).count();
        return atlasCount == 1;
    }

    @Override
    public ItemStack craft(CraftingInventory inv) {
        if (world == null) return ItemStack.EMPTY;
        List<ItemStack> itemStacks = MapAtlasesAccessUtils.getItemStacksFromGrid(inv)
                .stream()
                .map(ItemStack::copy)
                .toList();
        // Grab the Atlas in the Grid
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromItemStacks(itemStacks).copy();
        // Get the Map Ids in the Grid
        Set<Integer> mapIds = MapAtlasesAccessUtils.getMapIdsFromItemStacks(world, itemStacks);
        // Set NBT Data
        int emptyMapCount = (int)itemStacks.stream().filter(i -> i != null && i.isItemEqual(new ItemStack(Items.MAP))).count();
        NbtCompound compoundTag = atlas.getOrCreateNbt();
        Set<Integer> existingMaps = new HashSet<>(Ints.asList(compoundTag.getIntArray("maps")));
        existingMaps.addAll(mapIds);
        compoundTag.putIntArray("maps", existingMaps.stream().filter(Objects::nonNull).mapToInt(i->i).toArray());
        compoundTag.putInt("empty", emptyMapCount + compoundTag.getInt("empty"));
        atlas.setNbt(compoundTag);
        return atlas;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MapAtlasesMod.MAP_ATLAS_ADD_RECIPE;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }
}
