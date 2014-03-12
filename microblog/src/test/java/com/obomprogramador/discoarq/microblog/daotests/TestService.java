package com.obomprogramador.discoarq.microblog.daotests;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.obomprogramador.discoarq.microblog.main.MicroBlog;
import com.obomprogramador.discoarq.microblog.persistence.implementation.DaoMongoDB;

public class TestService {

    @Test
    public void testGetInvalidUserSession() throws ClientProtocolException, IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:8080/mb/server/session/cleuton/teste");
        HttpResponse response = client.execute(request);
        assertTrue(response.getStatusLine().getStatusCode() == 200);
        BufferedReader rd = new BufferedReader(
        new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        JSONObject resposta = new JSONObject(result.toString());
        assertTrue(resposta.get("mensagem").equals("Invalid user: cleuton"));
    }
    
    @Test
    public void testGetValidUserSession() throws Exception {
        DaoMongoDB dao = new DaoMongoDB();
        JSONObject usuarioJson = new JSONObject(
                "{"
                + "username : "     +   "usuario1" + "," 
                + "password : "     +   "teste1"   + ","
                + "foto : "         +   "arquivo.png"
                + "}"
                );
        JSONObject usuario = this.createUser(dao, usuarioJson);
        usuario = null;

        usuario = dao.findUser("usuario1", "teste1");
        assertTrue(usuario.get("username").equals("usuario1"));
        boolean resposta = dao.addUser(usuario);
        assertFalse(resposta);
        
        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet("http://localhost:8080/mb/server/session/usuario1/teste1");
        HttpResponse response = client.execute(request);
        assertTrue(response.getStatusLine().getStatusCode() == 200);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        JSONObject resposta2 = new JSONObject(result.toString());
        assertTrue(resposta2.get("usuario_id").toString().equals(usuario.get("_id").toString()));
    }
    
    @Test
    public void testCreateUser() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost("http://localhost:8080/mb/server/user");
        JSONObject usuario = new JSONObject();
        usuario.put("username", "cleuton");
        usuario.put("password", "teste");
        request.setEntity(new StringEntity(usuario.toString(), 
                ContentType.create("application/json")));
        HttpResponse response = client.execute(request);
        assertTrue(response.getStatusLine().getStatusCode() == 200);
        BufferedReader rd = new BufferedReader(
                new InputStreamReader(response.getEntity().getContent()));
        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }
        JSONObject resposta2 = new JSONObject(result.toString());
        assertTrue(resposta2.get("username").toString().equals(usuario.get("username").toString()));        
    }
    
    private JSONObject createUser(DaoMongoDB dao, JSONObject usuario) throws Exception {
        boolean resposta = dao.addUser(usuario);
        String username = usuario.get("username").toString();
        String password = usuario.get("password").toString();
        if (resposta) {
            usuario = null;
            usuario = dao.findUser(username, password);
            assertTrue(usuario != null);
            assertTrue(usuario.get("username").equals(username) 
                    && usuario.get("password").equals(password));
        }
        return usuario;
    }
    

}
