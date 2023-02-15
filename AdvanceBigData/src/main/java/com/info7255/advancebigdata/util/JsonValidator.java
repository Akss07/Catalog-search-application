package com.info7255.advancebigdata.util;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class JsonValidator{

    public void validateJson(JSONObject jsonObject) throws IOException{
        try(InputStream inputStream = getClass().getResourceAsStream("/payload-schema-validation.json")){
            JSONObject jsonSchema = new JSONObject(new JSONTokener(inputStream));
            Schema schema = SchemaLoader.load(jsonSchema);
            schema.validate(jsonObject);
        }
    }

}
