package com.hadroncfy.sreplay.recording.mcpr;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class Marker {
    public Marker(String name, int time) {
        this.name = name;
        this.time = time;
    }

    public Marker() {
        this("untitled", 0);
    }

    public int time;

    public String name;

    public double x = 0;

    public double y = 0;

    public double z = 0;

    public float phi = 0;

    public float theta = 0;

    public float varphi = 0;

    public static class Serializer implements JsonSerializer<Marker> {

        @Override
        public JsonElement serialize(Marker src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject ret = new JsonObject();
            JsonObject value = new JsonObject();
            JsonObject position = new JsonObject();
            ret.add("realTimestamp", new JsonPrimitive(src.time));
            ret.add("value", value);

            value.add("name", new JsonPrimitive(src.name));
            value.add("position", position);

            position.add("x", new JsonPrimitive(src.x));
            position.add("y", new JsonPrimitive(src.y));
            position.add("z", new JsonPrimitive(src.z));
            position.add("yaw", new JsonPrimitive(src.phi));
            position.add("pitch", new JsonPrimitive(src.theta));
            position.add("roll", new JsonPrimitive(src.varphi));
            return ret;
        }
        
    }
}