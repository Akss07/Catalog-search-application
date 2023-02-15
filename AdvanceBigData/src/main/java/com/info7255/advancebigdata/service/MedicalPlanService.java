package com.info7255.advancebigdata.service;

import com.info7255.advancebigdata.repositorey.MedicalPlanRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.*;

@Service
public class MedicalPlanService {

private final static Logger logger= LoggerFactory.getLogger(MedicalPlanService.class);
    @Autowired
    MedicalPlanRepository medicalPlanRepository;

    @Autowired
    ETagService eTagService;
    public boolean isKeyPresent(String key){
        return medicalPlanRepository.isKeyPresent(key);
    }

    public String createPlan(String key, JSONObject jsonObject){
        savePlan(key, jsonObject);
        String eTag = generateEtag(key, jsonObject);
        return eTag;
    }

    private String generateEtag(String key, JSONObject planObject) {
        String newEtag = eTagService.getETag(planObject);
        hSet(key, "eTag", newEtag);
        return newEtag;
    }

    private void savePlan(String key, JSONObject planObject){
        convertToMap(planObject);
        Map<String, Object> result = new HashMap<String, Object>();
        getOrDeleteData(key, result, false);
        //return result;
    }

    public Map<String, Object> getPlan(String key){
        Map<String, Object> result = new HashMap<>();
        getOrDeleteData(key,result, false);
        return result;
    }

    public void deletePlan(String key){
        getOrDeleteData(key,null, true);

    }


    private Map<String, Map<String, Object>> convertToMap(JSONObject jsonObject){
        Map<String, Map<String, Object>> map = new HashMap<>();
        Map<String, Object> valueMap = new HashMap<>();

        for(String key: jsonObject.keySet()){
            String redisKey = jsonObject.get("objectType") + "_" + jsonObject.get("objectId");
            Object value = jsonObject.get(key);

            if(value instanceof JSONObject){
                value = convertToMap((JSONObject) value);
                HashMap<String, Map<String, Object>> val = (HashMap<String, Map<String, Object>>) value;
                medicalPlanRepository.saveToRedis(redisKey + "_" + key, val.entrySet().iterator().next().getKey());
            }else if(value instanceof JSONArray){
                value = convertToList((JSONArray) value);
                for (HashMap<String, HashMap<String, Object>> entry : (List<HashMap<String, HashMap<String, Object>>>) value) {
                    for (String listKey : entry.keySet()) {
                        medicalPlanRepository.addSetValue(redisKey + "_" + key, listKey);
                        logger.info("All keys " + redisKey + "_" + key + " : " + listKey);
                    }
                }
            }else{
                hSet(redisKey, key, value.toString());
                valueMap.put(key, value);
                map.put(redisKey, valueMap);
            }
        }
        return map;
    }

    private List<Object> convertToList(JSONArray array) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof JSONArray) {
                value = convertToList((JSONArray) value);
            } else if (value instanceof JSONObject) {
                value = convertToMap((JSONObject) value);
            }
            list.add(value);
        }
        return list;
    }

    private Map<String, Object> getOrDeleteData(String redisKey, Map<String, Object> result, boolean isDelete) {
        Set<String> keys = medicalPlanRepository.getKeys(redisKey + "*");
        for (String key : keys) {
            if (key.equals(redisKey)) {
                if (isDelete) {
                    medicalPlanRepository.deleteKeys(new String[] {key});
                } else {
                    Map<String, String> val = medicalPlanRepository.getAllValuesByKey(key);
                    for (String name : val.keySet()) {
                        if (!name.equalsIgnoreCase("eTag")) {
                            result.put(name,
                                    isStringDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                        }
                    }
                }

            } else {
                String newStr = key.substring((redisKey + "_").length());
                logger.info("Key to be serched :" +key+"--------------"+newStr);
                Set<String> members = medicalPlanRepository.sMembers(key);
                if (members.size() > 1) {
                    List<Object> listObj = new ArrayList<Object>();
                    for (String member : members) {
                        if (isDelete) {
                            getOrDeleteData(member, null, true);
                        } else {
                            Map<String, Object> listMap = new HashMap<String, Object>();
                            listObj.add(getOrDeleteData(member, listMap, false));

                        }
                    }
                    if (isDelete) {
                        medicalPlanRepository.deleteKeys(new String[] {key});
                    } else {
                        result.put(newStr, listObj);
                    }

                } else {
                    if (isDelete) {
                        medicalPlanRepository.deleteKeys(new String[]{members.iterator().next(), key});
                    } else {
                        Map<String, String> val = medicalPlanRepository.getAllValuesByKey(members.iterator().next());
                        Map<String, Object> newMap = new HashMap<String, Object>();
                        for (String name : val.keySet()) {
                            newMap.put(name,
                                    isStringDouble(val.get(name)) ? Double.parseDouble(val.get(name)) : val.get(name));
                        }
                        result.put(newStr, newMap);
                    }
                }
            }
        }
        return result;
    }

    private boolean isStringDouble(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public void hSet(String key, String field, String value ) {
        medicalPlanRepository.hSet(key, field, value);
    }

    public String getETag(String key) {
        return medicalPlanRepository.getEtag(key);
    }

}
