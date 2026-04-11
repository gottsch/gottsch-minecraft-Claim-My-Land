/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2024 Mark Gottschling (gottsch)
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land. If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.claimmyland.client.renderer;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/**
 * Mod-bus subscriber that resolves {@link ParcelBorderRenderer} texture atlas sprites
 * after the block atlas is stitched. Registered on the mod event bus from
 * {@code ClaimMyLand} constructor so it fires during mod loading, before the
 * first render frame.
 *
 * <p>Kept separate from {@link ParcelBorderRenderer} because that class subscribes
 * to Forge bus events ({@code RenderLevelStageEvent}) which cannot coexist with
 * mod bus events in the same registered class.</p>
 *
 * @author Mark Gottschling on March 10, 2026
 */
@OnlyIn(Dist.CLIENT)
public class ParcelBorderRendererSetup {

    /**
     * Resolves and caches all texture atlas sprites used by {@link ParcelBorderRenderer}.
     * Called every time the block atlas is stitched, including resource pack reloads.
     */
    @SubscribeEvent
    public static void onTextureStitchPost(TextureAtlasStitchedEvent event) {
        if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) return;

        ParcelBorderRenderer.resolveSprites(event.getAtlas());
    }
}