package mod.gottsch.forge.claimmyland.core.loot;

import com.mojang.serialization.Codec;
import mod.gottsch.forge.claimmyland.ClaimMyLand;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * @author Mark Gottschling on March 26, 2026
 */
public class ModLootModifiers {

    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    ClaimMyLand.MOD_ID);

    public static final RegistryObject<Codec<DeedLootModifier>> DEED_LOOT_MODIFIER =
            LOOT_MODIFIERS.register("deed_loot_modifier", () -> DeedLootModifier.CODEC);

    public static void register(IEventBus bus) {
        LOOT_MODIFIERS.register(bus);
    }
}