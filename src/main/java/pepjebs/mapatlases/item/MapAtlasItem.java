package pepjebs.mapatlases.item;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.map.MapState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.screen.MapAtlasesAtlasOverviewScreenHandler;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapAtlasItem extends Item implements ExtendedScreenHandlerFactory {

    public MapAtlasItem(Settings settings) {
        super(settings);
    }

    public static int getMaxMapCount() {
        if (MapAtlasesMod.CONFIG != null) {
            return MapAtlasesMod.CONFIG.maxMapCount;
        }
        return 128;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        if (world != null && world.isClient) {
            MapState mapState = MapAtlasesAccessUtils.getFirstMapStateFromAtlas(world, stack);
            if (mapState == null) {
                tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_err"))
                        .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
                return;
            }
            int mapSize = MapAtlasesAccessUtils.getMapCountFromItemStack(stack);
            int empties = MapAtlasesAccessUtils.getEmptyMapCountFromItemStack(stack);
            if (mapSize + empties >= getMaxMapCount()) {
                tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_full"))
                        .formatted(Formatting.ITALIC).formatted(Formatting.GRAY));
            }
            tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_1", mapSize))
                    .formatted(Formatting.GRAY));
            if (MapAtlasesMod.CONFIG == null || MapAtlasesMod.CONFIG.enableEmptyMapEntryAndFill) {
                tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_2", empties))
                        .formatted(Formatting.GRAY));
            }
            tooltip.add(MutableText.of(new TranslatableTextContent("item.map_atlases.atlas.tooltip_3", 1 << mapState.scale))
                    .formatted(Formatting.GRAY));
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        player.openHandledScreen(this);
        world.playSound(player.getX(), player.getY(), player.getZ(), MapAtlasesMod.ATLAS_OPEN_SOUND_EVENT,
                SoundCategory.PLAYERS, 1.0F, 1.0F, false);
        return TypedActionResult.consume(player.getStackInHand(hand));
    }

    @Override
    public Text getDisplayName() {
        return MutableText.of(new TranslatableTextContent(getTranslationKey()));
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(player.getInventory());
        Map<Integer, List<Integer>> idsToCenters = new HashMap<>();
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(player.world, atlas);
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            idsToCenters.put(
                    MapAtlasesAccessUtils.getMapIntFromString(state.getKey()),
                    Arrays.asList(state.getValue().centerX, state.getValue().centerZ));
        }
        return new MapAtlasesAtlasOverviewScreenHandler(syncId, inv, idsToCenters);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity serverPlayerEntity, PacketByteBuf packetByteBuf) {
        ItemStack atlas = MapAtlasesAccessUtils.getAtlasFromPlayerByConfig(serverPlayerEntity.getInventory());
        if (atlas.isEmpty()) return;
        Map<String, MapState> mapInfos = MapAtlasesAccessUtils.getAllMapInfoFromAtlas(serverPlayerEntity.world, atlas);
        if (mapInfos.isEmpty()) return;
        packetByteBuf.writeInt(mapInfos.size());
        for (Map.Entry<String, MapState> state : mapInfos.entrySet()) {
            packetByteBuf.writeInt(MapAtlasesAccessUtils.getMapIntFromString(state.getKey()));
            packetByteBuf.writeInt(state.getValue().centerX);
            packetByteBuf.writeInt(state.getValue().centerZ);
        }
    }

    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getPlayer() == null || context.getWorld().isClient) return super.useOnBlock(context);
        BlockState blockState = context.getWorld().getBlockState(context.getBlockPos());
        if (blockState.isIn(BlockTags.BANNERS)) {
            if (!context.getWorld().isClient) {
                MapState mapState =
                        MapAtlasesAccessUtils.getActiveAtlasMapState(
                                context.getWorld(),
                                context.getStack(),
                                context.getPlayer().getName().getString()).getValue();
                if (mapState != null) {
                    mapState.addBanner(context.getWorld(), context.getBlockPos());
                }
            }
            return ActionResult.success(context.getWorld().isClient);
        } else {
            return super.useOnBlock(context);
        }
    }
}
