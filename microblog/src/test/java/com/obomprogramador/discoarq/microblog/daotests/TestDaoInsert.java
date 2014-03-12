package com.obomprogramador.discoarq.microblog.daotests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.obomprogramador.discoarq.microblog.persistence.implementation.DaoMongoDB;

public class TestDaoInsert {
    
    private static DB db = null;
    private static JSONObject newSessionJson = null;
    private static JSONObject oldSessionJson = null;

    @BeforeClass
    public static void setup() {
        
        try {
            
            db = TestDaoInsert.getDb();
           
            
            DBCollection userColl = db.getCollection("usuarios");
            BasicDBObject usuario1 = new BasicDBObject();
            usuario1.put("username", "firstuser");
            usuario1.put("password", "senha");
            usuario1.put("foto", "foto.png");
            userColl.insert(usuario1);
            BasicDBObject  usuario2 = new BasicDBObject();
            usuario2.put("username", "seconduser");
            usuario2.put("password", "senha2");
            usuario2.put("foto", "foto2.png");
            userColl.insert(usuario2);
            
            DBCollection messagesColl = db.getCollection("mensagem");
            DBObject msgIndex = new BasicDBObject();
            msgIndex.put("usuario_id", 1);
            msgIndex.put("data", -1);
            messagesColl.ensureIndex(msgIndex);
            BasicDBObject mensagem1 = new BasicDBObject();
            mensagem1.put("usuario_id", usuario1.get("_id"));
            mensagem1.put("data", new Date());
            mensagem1.put("texto", "mensagem de teste do firstuser");
            messagesColl.insert(mensagem1);
            
            mensagem1 = new BasicDBObject();
            mensagem1.put("usuario_id", usuario2.get("_id"));
            mensagem1.put("data", new Date());
            mensagem1.put("texto", "mensagem de teste do seconduser");
            messagesColl.insert(mensagem1);
            
            DBCollection sessionsDb = db.getCollection("session");
            BasicDBObject newSession = new BasicDBObject();
            newSession.put("usuario_id", usuario1.get("_id"));
            DateTime dataOriginal = new DateTime(new Date());
            dataOriginal = dataOriginal.minusMinutes(5);
            newSession.put("lastUpdate",dataOriginal.toDate());
            BasicDBObject varObject = new BasicDBObject();
            varObject.put("variavel", "valor");
            List<BasicDBObject> listaVariaveis = new ArrayList<BasicDBObject>();
            listaVariaveis.add(varObject);
            newSession.put("variaveis", listaVariaveis);
            sessionsDb.insert(newSession);
            newSessionJson = new JSONObject(newSession.toString());

            BasicDBObject oldSession = new BasicDBObject();
            oldSession.put("usuario_id", usuario1.get("_id"));
            dataOriginal = new DateTime(new Date());
            dataOriginal = dataOriginal.minusMinutes(15);
            oldSession.put("lastUpdate",dataOriginal.toDate());
            sessionsDb.insert(oldSession);
            oldSessionJson = new JSONObject(oldSession.toString());
           
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    
    private static DB getDb() throws UnknownHostException {
        DB db = null;
        MongoClient mongoClient;
        String server = "localhost";
        int port = 27017;
        if (System.getProperty("dbServer") != null) {
            server = System.getProperty("dbServer");    
        }
        if (System.getProperty("dbServerPort") != null) {
            port = Integer.parseInt(System.getProperty("dbServerPort"));
        }
        
        mongoClient = new MongoClient( server , port );
        db = mongoClient.getDB("microblog");
        db.dropDatabase();
        
        db = null;
        mongoClient.close();
        mongoClient = new MongoClient( server , port );
        db = mongoClient.getDB( "microblog" );        
        return db;
    }
    
    @Test
    public void testAddUser() throws Exception {
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
    }
    
    @Test
    public void testAddFollowing() throws Exception {
        DaoMongoDB dao = new DaoMongoDB();
        JSONObject usuario = new JSONObject();
        boolean resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        JSONObject usuarioJson = new JSONObject(
                "{"
                + "username : "     +   "usuario2" + "," 
                + "password : "     +   "teste2"   + ","
                + "foto : "         +   "arquivo.png"
                + "}"
                );
        usuario = this.createUser(dao, usuarioJson);
        resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        resposta = dao.follow(usuario, "firstuser");
        assertTrue(resposta);
        List<JSONObject> messages = dao.getMessages(usuario);
        assertTrue(messages.size() == 1);
        assertTrue(messages.get(0).get("texto").equals("mensagem de teste do firstuser"));
    }
    
    @Test
    public void testPostMessage() throws Exception {
        DaoMongoDB dao = new DaoMongoDB();
        JSONObject usuario = new JSONObject();
        boolean resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        JSONObject usuarioJson = new JSONObject(
                "{"
                + "username : "     +   "usuario3" + "," 
                + "password : "     +   "teste3"   + ","
                + "foto : "         +   "arquivo.png"
                + "}"
                );
        usuario = this.createUser(dao, usuarioJson);
        resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        resposta = dao.follow(usuario, "firstuser");
        assertTrue(resposta);
        
        dao.postMessage(usuario, "Mensagem1");
        dao.postMessage(usuario, "Mensagem2");
        
        List<JSONObject> messages = dao.getMessages(usuario);
        assertTrue(messages.size() == 3);
        System.out.println("Mensagem 0: " + messages.get(0).get("texto"));
        assertTrue(messages.get(0).get("texto").equals("Mensagem2"));
        assertTrue(messages.get(1).get("texto").equals("Mensagem1"));
        assertTrue(messages.get(2).get("texto").equals("mensagem de teste do firstuser"));
        
    }
    
    @Test
    public void testAbandon() throws Exception {
        DaoMongoDB dao = new DaoMongoDB();
        JSONObject usuario = new JSONObject();
        boolean resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        JSONObject usuarioJson = new JSONObject(
                "{"
                + "username : "     +   "usuario4" + "," 
                + "password : "     +   "teste4"   + ","
                + "foto : "         +   "arquivo.png"
                + "}"
                );
        usuario = this.createUser(dao, usuarioJson);
        resposta = dao.follow(usuario, "teste");
        assertFalse(resposta);
        resposta = dao.follow(usuario, "firstuser");
        assertTrue(resposta);
        List<JSONObject> messages = dao.getMessages(usuario);
        assertTrue(messages.size() == 1);
        assertTrue(messages.get(0).get("texto").equals("mensagem de teste do firstuser"));
        dao.postMessage(usuario, "Mensagem1");
        messages = dao.getMessages(usuario);
        assertTrue(messages.size() == 2);
        resposta = dao.abandon(usuario, "firstuser");
        assertTrue(resposta);
        messages = dao.getMessages(usuario);
        assertTrue(messages.size() == 1);
        assertTrue(messages.get(0).get("texto").equals("Mensagem1"));
        
    }

    @Test
    public void testNewSession() throws Exception {
        DaoMongoDB dao = new DaoMongoDB();
        boolean resposta = dao.validateSession(newSessionJson);
        assertTrue(resposta);
        resposta = dao.validateSession(oldSessionJson);
        assertFalse(resposta);
        JSONObject usuario = dao.findUser("seconduser", "senha2");
        assertTrue(usuario != null);
        JSONObject session = dao.getSession(usuario);
        assertTrue(session != null);
        resposta = dao.validateSession(session);
        assertTrue(resposta);
        assertTrue(session.get("usuario_id").toString().equals(usuario.get("_id").toString()));
        BasicDBObject varObject = new BasicDBObject();
        varObject.put("variavel", "valor");
        List<BasicDBObject> listaVariaveis = new ArrayList<BasicDBObject>();
        listaVariaveis.add(varObject);
        session.put("variaveis", listaVariaveis);
        resposta = dao.updateSession(session);
        assertTrue(resposta);
        JSONObject databaseSession = dao.pullSession(session);
        assertTrue(databaseSession.get("_id").toString().equals(session.get("_id").toString()));
        assertTrue(databaseSession.get("usuario_id").toString().equals(usuario.get("_id").toString()));
        JSONArray lista =  (JSONArray) databaseSession.get("variaveis");
        String valor = (String) lista.getJSONObject(0).get("variavel");
        assertTrue(valor.equals("valor"));
        JSONObject usuarioLogado = dao.getUserFromSession(databaseSession);
        assertTrue(usuarioLogado.get("username").equals(usuario.get("username")));
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
