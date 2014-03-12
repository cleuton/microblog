package com.obomprogramador.discoarq.microblog.persistence;

import java.util.List;

import org.json.JSONObject;

public interface JsonDao {
    boolean addUser(JSONObject user) throws Exception;
    JSONObject findUser(String username, String password) throws Exception;
    List<JSONObject> getMessages(JSONObject user) throws Exception;
    boolean follow(JSONObject user, String followUserName) throws Exception;
    boolean postMessage(JSONObject user, String message) throws Exception;
    boolean abandon(JSONObject user, String followUserName) throws Exception;
    boolean validateSession(JSONObject session) throws Exception;
    JSONObject getSession(JSONObject user) throws Exception;
    boolean updateSession(JSONObject session) throws Exception;
    JSONObject pullSession(JSONObject session) throws Exception;
    JSONObject getUserFromSession(JSONObject session) throws Exception;
}
