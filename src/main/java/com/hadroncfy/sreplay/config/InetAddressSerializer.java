package com.hadroncfy.sreplay.config;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class InetAddressSerializer implements JsonDeserializer<InetAddress>, JsonSerializer<InetAddress> {

    @Override
    public JsonElement serialize(InetAddress src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toString());
    }

    @Override
    public InetAddress deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        try {
            return InetAddress.getByName(json.getAsString());
        } catch (UnknownHostException e) {
            throw new JsonParseException("Failed to parse address", e);
        }
    }

}