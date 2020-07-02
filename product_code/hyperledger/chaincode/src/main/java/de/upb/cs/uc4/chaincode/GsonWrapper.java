package de.upb.cs.uc4.chaincode;

import com.google.gson.*;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;

import java.lang.reflect.Type;

public class GsonWrapper {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    LocalDate.class,
                    new JsonDeserializer<LocalDate>() {
                        @Override
                        public LocalDate deserialize(
                                JsonElement json,
                                Type type,
                                JsonDeserializationContext jsonDeserializationContext
                        ) throws JsonParseException {
                            try {
                                return LocalDate.parse(json.getAsJsonPrimitive().getAsString());
                            } catch (DateTimeParseException e) {
                                return null;
                            }
                        }
                    })
            .registerTypeAdapter(
                    LocalDate.class,
                    new JsonSerializer<LocalDate>() {
                        @Override
                        public JsonElement serialize(LocalDate date, Type typeOfSrc, JsonSerializationContext context) {
                            return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE)); // "yyyy-mm-dd"
                        }
                    })
            .registerTypeAdapter(
                    Integer.class,
                    new JsonDeserializer<Integer>() {
                        @Override
                        public Integer deserialize(
                                JsonElement json,
                                Type type,
                                JsonDeserializationContext jsonDeserializationContext
                        ) throws JsonParseException {
                            try {
                                return json.getAsInt();
                            } catch (RuntimeException e) {
                                return null;
                            }
                        }
                    })
            .create();

    public <T> T fromJson(String json, Class<T> t) throws JsonSyntaxException {
        return gson.fromJson(json, t);
    }

    public <T> String toJson(T object) {
        return gson.toJson(object);
    }
}
