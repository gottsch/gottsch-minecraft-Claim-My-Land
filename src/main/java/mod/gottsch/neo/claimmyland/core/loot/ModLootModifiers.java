package mod.gottsch.neo.claimmyland.core.loot;

import com.mojang.serialization.MapCodec;
import mod.gottsch.neo.claimmyland.ClaimMyLand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * @author Mark Gottschling on March 26, 2026
 */
public class ModLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    ClaimMyLand.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<DeedLootModifier>> DEED_LOOT_MODIFIER =
            LOOT_MODIFIERS.register("deed_loot_modifier", () -> DeedLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }
}