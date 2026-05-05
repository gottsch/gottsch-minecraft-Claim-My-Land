/*
 * This file is part of Claim My Land.
 * Copyright (c) 2026 Mark Gottschling (gottsch)
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
 *
 */
package mod.gottsch.neo.claimmyland.core.persistence;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import mod.gottsch.neo.claimmyland.core.estate.Estate;
import mod.gottsch.neo.claimmyland.core.estate.EstateContext;
import mod.gottsch.neo.claimmyland.core.estate.NationEstate;
import mod.gottsch.neo.claimmyland.core.estate.NationEstateContext;
import mod.gottsch.neo.claimmyland.core.parcel.Parcel;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelType;
import mod.gottsch.neo.claimmyland.core.parcel.ParcelTypeRegistry;
import mod.gottsch.neo.gottschcore.spatial.Box;
import mod.gottsch.neo.gottschcore.spatial.Coords;
import mod.gottsch.neo.gottschcore.spatial.ICoords;
import net.minecraft.nbt.CompoundTag;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Builds the shared Gson instance used for parcel backup serialization and deserialization.
 *
 * Registered adapters handle types that GSON cannot handle automatically:
 *   - UUID      — serialized as a plain hyphenated string
 *   - ICoords   — serialized as {x, y, z}; deserialized via Coords.EMPTY.load()
 *   - Parcel    — polymorphic: dispatches to the concrete class by the "type" field
 *   - Estate    — polymorphic: EstateContext vs NationEstateContext by "accessType" presence
 *   - NationEstate — always NationEstateContext
 *
 * All other fields (strings, booleans, enums, Set<String>, Set<UUID>, Box) are handled
 * automatically by GSON reflection, so new fields added to parcels or estates are
 * picked up without any changes here.
 *
 * @author Mark Gottschling on May 4, 2026
 */
public class ParcelGson {

    public static Gson create() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(UUID.class, new UUIDAdapter())
                .registerTypeHierarchyAdapter(ICoords.class, new ICoordsAdapter())
                .registerTypeAdapter(Box.class, new BoxAdapter())
                .registerTypeAdapter(Parcel.class, new ParcelDeserializer())
                .registerTypeAdapter(Estate.class, new EstateDeserializer())
                .registerTypeAdapter(NationEstate.class, new NationEstateDeserializer())
                .create();
    }

    // -------------------------------------------------------------------------
    // UUID — plain hyphenated string on the wire
    // -------------------------------------------------------------------------

    private static class UUIDAdapter extends TypeAdapter<UUID> {
        @Override
        public void write(JsonWriter out, UUID value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.value(value.toString());
        }
        @Override
        public UUID read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            return UUID.fromString(in.nextString());
        }
    }

    // -------------------------------------------------------------------------
    // ICoords — {x, y, z} object; deserialized via Coords.EMPTY.load()
    // -------------------------------------------------------------------------

    private static class ICoordsAdapter extends TypeAdapter<ICoords> {
        @Override
        public void write(JsonWriter out, ICoords value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.beginObject();
            out.name("x"); out.value(value.getX());
            out.name("y"); out.value(value.getY());
            out.name("z"); out.value(value.getZ());
            out.endObject();
        }
        @Override
        public ICoords read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            int x = 0, y = 0, z = 0;
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "x" -> x = in.nextInt();
                    case "y" -> y = in.nextInt();
                    case "z" -> z = in.nextInt();
                    default  -> in.skipValue();
                }
            }
            in.endObject();
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            return Coords.EMPTY.load(tag);
        }
    }

    // -------------------------------------------------------------------------
    // Parcel — dispatches to the concrete class by the "type" field
    // -------------------------------------------------------------------------

    private static class ParcelDeserializer implements JsonDeserializer<Parcel> {
        @Override
        public Parcel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            if (!obj.has("type") || obj.get("type").isJsonNull())
                throw new JsonParseException("Missing 'type' field in parcel");
            ParcelType parcelType = ParcelType.fromString(obj.get("type").getAsString());
            // create() gives us the correct concrete instance; we only need the class
            Parcel template = ParcelTypeRegistry.create(parcelType)
                    .orElseThrow(() -> new JsonParseException("Unknown parcel type: " + parcelType));
            // context dispatches to GSON's reflective deserializer for the concrete class,
            // not back to this adapter (which is registered only for Parcel.class, not subtypes)
            return context.deserialize(json, template.getClass());
        }
    }

    // -------------------------------------------------------------------------
    // Estate — EstateContext unless the JSON has an "accessType" field (NationEstateContext)
    // -------------------------------------------------------------------------

    private static class EstateDeserializer implements JsonDeserializer<Estate> {
        @Override
        public Estate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            boolean isNation = obj.has("accessType");
            return context.deserialize(json, isNation ? NationEstateContext.class : EstateContext.class);
        }
    }

    // -------------------------------------------------------------------------
    // NationEstate — always NationEstateContext
    // -------------------------------------------------------------------------

    private static class NationEstateDeserializer implements JsonDeserializer<NationEstate> {
        @Override
        public NationEstate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return context.deserialize(json, NationEstateContext.class);
        }
    }

    // -------------------------------------------------------------------------
    // Box — uses Box.load(CompoundTag), the same factory as NBT loading, to
    // avoid relying on GSON reflection over Box's internal field structure.
    // -------------------------------------------------------------------------

    private static class BoxAdapter extends TypeAdapter<Box> {
        @Override
        public void write(JsonWriter out, Box value) throws IOException {
            if (value == null) { out.nullValue(); return; }
            out.beginObject();
            out.name("min"); writeCoords(out, value.getMinCoords());
            out.name("max"); writeCoords(out, value.getMaxCoords());
            out.endObject();
        }

        private void writeCoords(JsonWriter out, ICoords coords) throws IOException {
            out.beginObject();
            out.name("x"); out.value(coords.getX());
            out.name("y"); out.value(coords.getY());
            out.name("z"); out.value(coords.getZ());
            out.endObject();
        }

        @Override
        public Box read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) { in.nextNull(); return null; }
            CompoundTag tag = new CompoundTag();
            in.beginObject();
            while (in.hasNext()) {
                String name = in.nextName();
                switch (name) {
                    case "min", "minCoords" -> tag.put("min", readCoordsTag(in));
                    case "max", "maxCoords" -> tag.put("max", readCoordsTag(in));
                    default -> in.skipValue();
                }
            }
            in.endObject();
            return Box.load(tag);
        }

        private CompoundTag readCoordsTag(JsonReader in) throws IOException {
            CompoundTag tag = new CompoundTag();
            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                    case "x" -> tag.putInt("x", in.nextInt());
                    case "y" -> tag.putInt("y", in.nextInt());
                    case "z" -> tag.putInt("z", in.nextInt());
                    default  -> in.skipValue();
                }
            }
            in.endObject();
            return tag;
        }
    }
}
