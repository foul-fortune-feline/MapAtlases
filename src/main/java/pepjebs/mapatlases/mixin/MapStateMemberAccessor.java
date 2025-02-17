package pepjebs.mapatlases.mixin;

import com.google.common.collect.Maps;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pepjebs.mapatlases.utils.MapStateIntrfc;

import java.util.Map;

@Mixin(MapState.class)
public class MapStateMemberAccessor implements MapStateIntrfc {
    @Shadow
    Map<String, MapIcon> icons = Maps.newLinkedHashMap();

    public Map<String, MapIcon> getFullIcons(){
        return icons;
    }
}
