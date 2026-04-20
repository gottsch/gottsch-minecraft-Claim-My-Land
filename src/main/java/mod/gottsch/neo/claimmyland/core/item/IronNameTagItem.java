/*
 * This file is part of  Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
 *
 * All rights reserved.
 *
 * Claim My Land is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Claim My Land is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Claim My Land.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */
package mod.gottsch.neo.claimmyland.core.item;

import net.minecraft.world.item.Item;

/**
 * A CML-exclusive name tag used to rename a parcel via right-click on a Border Stone.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Player names the tag in an anvil.</li>
 *   <li>Player right-clicks a Border Stone while holding the named tag.</li>
 *   <li>The parcel's name is updated, the tag is consumed.</li>
 * </ol>
 *
 * <p>This item has no mob-naming functionality. All interaction logic lives in
 * {@link mod.gottsch.neo.claimmyland.core.block.BorderStoneBlock#useItemOn}.
 *
 * <p>Obtained via {@code /cml give iron_name_tag} or crafted with a vanilla name tag
 * and an iron nugget.
 *
 * @author Mark Gottschling on Apr 19, 2026
 * @see GoldNameTagItem
 */
public class IronNameTagItem extends Item {

    public IronNameTagItem(Properties properties) {
        super(properties);
    }
}