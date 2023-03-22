package com.info7255.advancebigdata.controller;

import com.info7255.advancebigdata.dao.ErrorResponse;
import com.info7255.advancebigdata.dao.JwtResponse;
import com.info7255.advancebigdata.exception.*;
import com.info7255.advancebigdata.service.ETagService;
import com.info7255.advancebigdata.service.MedicalPlanService;
import com.info7255.advancebigdata.util.JsonValidator;
import com.info7255.advancebigdata.util.JwtUtil;
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
import java.util.*;


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
    private final JwtUtil jwtUtil;

    public MedicalPlanController(MedicalPlanService medicalPlanService, JwtUtil jwtUtil) {
        this.medicalPlanService = medicalPlanService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/token")
    public ResponseEntity<JwtResponse> generateToken() {
        String token = jwtUtil.generateToken();
        return new ResponseEntity<>(new JwtResponse(token), HttpStatus.CREATED);
    }

    @PostMapping("/validate")
    public boolean validateToken(@RequestHeader HttpHeaders requestHeader) {
        boolean result;
        String authorization = requestHeader.getFirst("Authorization");
        if (authorization == null || authorization.isBlank()) throw new UnauthorizedException("Missing token!");
        try {
            String token = authorization.split(" ")[1];
            result = jwtUtil.validateToken(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid Token");
        }
        return result;
    }

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
    public ResponseEntity<?> deletePlan(@PathVariable String objectId, @RequestHeader HttpHeaders headers){
        String key = "plan" + "_" + objectId;
        if (!medicalPlanService.isKeyPresent(key)){
            throw new ResourceNotFoundException("ObjectId not found!");
        }
        String eTag = medicalPlanService.getETag(key);
        List<String> ifMatch;
        try {
            ifMatch = headers.getIfMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        if (ifMatch.size() == 0) throw new ETagParseException("ETag is not provided with request!");
        if (!ifMatch.contains(eTag)) return preConditionFailed(eTag);

        medicalPlanService.deletePlan(key);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/{objectId}")
    public ResponseEntity<?> patchPlan(@PathVariable String objectId,
                                       @RequestBody(required = false) String planJson,
                                       @RequestHeader HttpHeaders headers){
        if (planJson == null || planJson.isEmpty() || planJson.isBlank()){
            throw new BadRequestException("Request Body can not be empty");
        }
        JSONObject jsonObject = new JSONObject(planJson);
        String key = "plan_" + objectId;

        if (!medicalPlanService.isKeyPresent(key)){
            throw new ResourceNotFoundException("ObjectId not found!");
        }

        List<String> ifMatch;
        try {
            ifMatch = headers.getIfMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        String eTag = medicalPlanService.getETag(key);

        if (ifMatch.size() == 0) throw new ETagParseException("ETag is not provided with request!");
        if (!ifMatch.contains(eTag)) return preConditionFailed(eTag);

//        String oldPlan = medicalPlanService.getPlan(key).toString();
//        String newPlan = jsonObject.toString();

        try {
            jsonValidator.validateJson(jsonObject);
        } catch (ValidationException | IOException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        String updatedEtag = medicalPlanService.createPlan(key, jsonObject);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setETag(updatedEtag);
        return ResponseEntity.ok()
                .eTag(updatedEtag)
                .body(new JSONObject().put("message: ", "Plan updated successfully!!").toString());
    }
    @PutMapping("/{objectId}")
    public ResponseEntity<?> putPlan(@PathVariable String objectId,
                                     @RequestBody String planJson,
                                     @RequestHeader HttpHeaders headers) {
        if (planJson == null || planJson.isBlank()) throw new BadRequestException("Request body is missing!");
        String key = "plan_" + objectId;
        if (!medicalPlanService.isKeyPresent(key)) {
            throw new ResourceNotFoundException("ObjectId not found!");
        }

        String eTag = medicalPlanService.getETag(key);

        List<String> ifMatch;
        try {
            ifMatch = headers.getIfMatch();
        } catch (Exception e) {
            throw new ETagParseException("ETag value invalid! Make sure the ETag value is a string!");
        }

        if (ifMatch.size() == 0) throw new ETagParseException("ETag is not provided with request!");
        if (!ifMatch.contains(eTag)) return preConditionFailed(eTag);

        JSONObject jsonObject = new JSONObject(planJson);
        try {
            jsonValidator.validateJson(jsonObject);
        } catch (ValidationException | IOException ex) {
            throw new BadRequestException(ex.getMessage());
        }

        medicalPlanService.deletePlan(key);
        String updatedEtag = medicalPlanService.createPlan(key, jsonObject);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setETag(updatedEtag);
        return ResponseEntity.ok()
                .eTag(updatedEtag)
                .body(new JSONObject().put("message: ", "Plan updated successfully!!").toString());
    }

    private ResponseEntity preConditionFailed(String eTag) {
        HttpHeaders headersToSend = new HttpHeaders();
        headersToSend.setETag(eTag);
        ErrorResponse errorResponse = new ErrorResponse(
                "Plan has not been updated",
                HttpStatus.PRECONDITION_FAILED.value(),
                new Date(),
                HttpStatus.PRECONDITION_REQUIRED.getReasonPhrase()
        );
        return new ResponseEntity<>(errorResponse, headersToSend, HttpStatus.PRECONDITION_FAILED);
    }
}
