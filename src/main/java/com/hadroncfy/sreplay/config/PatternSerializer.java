package com.hadroncfy.sreplay.config;

import java.lang.reflect.Type;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PatternSerializer implements JsonDeserializer<Pattern>, JsonSerializer<Pattern> {

    @Override
    public JsonElement serialize(Pattern src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.pattern());
    }

    @Override
    public Pattern deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return Pattern.compile(json.getAsString());
    }

}