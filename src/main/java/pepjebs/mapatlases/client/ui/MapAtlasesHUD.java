package pepjebs.mapatlases.client.ui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;

@Environment(EnvType.CLIENT)
public class MapAtlasesHUD extends DrawableHelper {

    public static final Identifier MAP_CHKRBRD =
            new Identifier("minecraft:textures/map/map_background_checkerboard.png");
    private static MinecraftClient client;
    private static MapRenderer mapRenderer;
    private static String currentMapId = "";

    public MapAtlasesHUD() {
        client = MinecraftClient.getInstance();
        mapRenderer = client.gameRenderer.getMapRenderer();
    }

    public void render(MatrixStack matrices) {
        ItemStack atlas;
        if (!(atlas = shouldDraw(client)).isEmpty()) {
            renderMapHUDFromItemStack(matrices, atlas);
        }
    }

    private ItemStack shouldDraw(MinecraftClient client) {
        // Forcibly only render on Overworld since player trackers don't disappear from Overworld
        // in other dimensions in vanilla MC
        if (client.player == null || client.player.world.getRegistryKey() != World.OVERWORLD) return ItemStack.EMPTY;
        PlayerInventory inv = client.player.inventory;
        // Check the hot-bar for an Atlas
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isItemEqual(new ItemStack(MapAtlasesMod.MAP_ATLAS))) return inv.getStack(i);
        }
        return ItemStack.EMPTY;
    }

    private void renderMapHUDFromItemStack(MatrixStack matrices, ItemStack atlas) {
        if (client.world == null) {
            MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (client.world)");
            return;
        }
        MapState state = MapAtlasesAccessUtils.getActiveAtlasMapState(client.world, atlas);
        if (state == null) {
            if (currentMapId != null) {
                MapAtlasesMod.LOGGER.warn("renderMapHUDFromItemStack: Current map id - null (state)");
                currentMapId = null;
            }
            return;
        }
        if (currentMapId == null || state.getId().compareTo(currentMapId) != 0) {
            if (currentMapId != null && currentMapId.compareTo("") != 0) {
                client.world.playSound(client.player.getX(), client.player.getY(), client.player.getZ(),
                        MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
            }
            currentMapId = state.getId();
        }
        // Draw map background
        int y = 0;
        int x = client.getWindow().getScaledWidth()-64;
        client.getTextureManager().bindTexture(MAP_CHKRBRD);
        drawTexture(matrices,x,y,0,0,64,64, 64, 64);

        // Draw map data
        x += 4;
        y += 4;
        VertexConsumerProvider.Immediate vcp;
        vcp = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        matrices.push();
        matrices.translate(x, y, 0.0);
        matrices.scale(0.45f, 0.45f, 0);
        mapRenderer.draw(matrices, vcp, state, false, Integer.parseInt("0000000011110000", 2));
        vcp.draw();
        matrices.pop();
    }
}
