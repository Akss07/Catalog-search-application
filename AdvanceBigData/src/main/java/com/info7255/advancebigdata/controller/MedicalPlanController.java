package com.info7255.advancebigdata.controller;

import com.info7255.advancebigdata.exception.*;
import com.info7255.advancebigdata.service.ETagService;
import com.info7255.advancebigdata.service.MedicalPlanService;
import com.info7255.advancebigdata.util.JsonValidator;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("v1/plan")
public class MedicalPlanController {
    private final static Logger logger = LoggerFactory.getLogger(MedicalPlanController.class);
    @Autowired
    JsonValidator jsonValidator;

    @Autowired
    MedicalPlanService medicalPlanService;

    @Autowired
    ETagService eTagService;

    @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity createPlan(@RequestBody(required = false) String planJson){
        if (planJson == null || planJson.isEmpty() || planJson.isBlank()){
            throw new BadRequestException("Request Body can not be empty");
        }
        JSONObject jsonObject = new JSONObject(planJson);
        try{
            jsonValidator.validateJson(jsonObject);
        }catch (ValidationException | IOException ex){
            throw new BadRequestException(ex.getMessage());
        }
        String key = jsonObject.get("objectType").toString() + "_" + jsonObject.getString("objectId").toString();
        logger.info("Key " + key);

        if(medicalPlanService.isKeyPresent(key)){
            throw new ConflictException("Plan already exists!", HttpStatus.CONFLICT.toString());
        }
        String newEtag = medicalPlanService.createPlan(key, jsonObject);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setETag(newEtag);
        return new ResponseEntity<>("{\"objectId\": \"" + jsonObject.getString("objectId")+ "\"}", httpHeaders, HttpStatus.CREATED);
    }

    @GetMapping("/{objectId}")
    public ResponseEntity<?> getPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId){
        String key = "plan_" + objectId;
        if (!medicalPlanService.isKeyPresent(key)){
            throw new ResourceNotFoundException("ObjectId not found!");
        }

        List<String> ifNoneMatch;
        try {
            ifNoneMatch = headers.getIfNoneMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        String eTag = medicalPlanService.getETag(key);
        logger.info("eTag " + eTag);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setETag(eTag);
        logger.info("if none match" + ifNoneMatch.contains(eTag));
        if(ifNoneMatch.contains(eTag)){
            return new ResponseEntity<>(null, httpHeaders, HttpStatus.NOT_MODIFIED);
        }else if(!ifNoneMatch.isEmpty() && !ifNoneMatch.contains(eTag)){
            throw new BadRequestException("ETag value is invalid!");
        }
        Map<String, Object> result = medicalPlanService.getPlan(key);
        return new ResponseEntity<>(result, httpHeaders, HttpStatus.OK);
    }

    @DeleteMapping("/{objectId}")
    public ResponseEntity<?> deletePlan(@PathVariable String objectId){
        String key = "plan" + "_" + objectId;
        if (!medicalPlanService.isKeyPresent(key)){
            throw new ResourceNotFoundException("ObjectId not found!");
        }

        medicalPlanService.deletePlan(key);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
