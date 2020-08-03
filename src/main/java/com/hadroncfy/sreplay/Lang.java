package com.hadroncfy.sreplay;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class Lang {
    private static Map<String, String> language;
    private static String selectedLang = "";
    private static final Gson GSON = new Gson();

    public static void load(String lang) throws IOException {
        if (!selectedLang.equals(lang)){
            try(Reader reader = new InputStreamReader(Lang.class.getClassLoader().getResourceAsStream(String.format("lang/%s.json", lang)))){
                language = GSON.fromJson(reader, HashMap.class);
                selectedLang = lang;
            }
        }
    }
    public static String getString(String key){
        String val = key;
        if (language != null){
            val = language.get(key);
            if (val == null){
                val = key;
            }
        }
        return val;
    }
}