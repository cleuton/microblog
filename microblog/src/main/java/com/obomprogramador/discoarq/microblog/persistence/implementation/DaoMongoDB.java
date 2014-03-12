package com.obomprogramador.discoarq.microblog.persistence.implementation;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.json.JSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;
import com.obomprogramador.discoarq.microblog.persistence.JsonDao;
import com.mongodb.MongoException.*;

public class DaoMongoDB implements JsonDao {
    
    private DB db;
    private MongoClient mongoClient;

    @Override
    public boolean addUser(JSONObject user) throws Exception {
        boolean returnCode = false;
        connectToDatabase();
        DBObject dbUser = this.convert(user);
        DBCollection collection = db.getCollection("usuarios");
        try {
            collection.ensureIndex(new BasicDBObject("username", 1), "nomeIdx", true);
            collection.insert(dbUser);
            returnCode = true;
        }
        catch (DuplicateKey dupl) {
            returnCode = false;
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }
        return returnCode;
    }
    
    @Override
    public JSONObject findUser(String username, String password) throws Exception {
        JSONObject user = null; 
        try {
            DBObject userDb = findUserInternal(username);
            if (userDb != null) {
                if(userDb.get("password").equals(password)) {
                    user = new JSONObject(userDb.toString());
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }
        
        return user;
    }
    
    private DBObject findUserInternal(String username) throws Exception {
        DBObject user = null;
        try {
            connectToDatabase();
            DBCollection collection = db.getCollection("usuarios");
            BasicDBObject query = new BasicDBObject("username", username);
            DBCursor cursor = collection.find(query);
            if (cursor.hasNext()) {
                cursor.next();
                user = cursor.curr();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }

        return user;
    }
    
    private DBObject convert(JSONObject object) {
        Object obj = JSON.parse(object.toString());
        DBObject dbUser = (DBObject) obj;
        return dbUser;
    }
    
    @Override
    public List<JSONObject> getMessages(JSONObject user) throws Exception {
        List<JSONObject> messageList = new ArrayList<JSONObject>();
        try {
            connectToDatabase();
            DBCollection mensagensObject = db.getCollection("mensagem");
            DBCollection seguindoObject = db.getCollection("seguindo");
            DBObject userDb = this.convert(user);
            BasicDBObject queryOwnMessages = new BasicDBObject("usuario_id", userDb.get("_id"));
            BasicDBObject messageOrder = new BasicDBObject("data", -1);
            DBCursor cursorOwnMsg = mensagensObject.find(queryOwnMessages).sort(messageOrder);
            while (cursorOwnMsg.hasNext()) {
                cursorOwnMsg.next();
                JSONObject messageObj = new JSONObject(cursorOwnMsg.curr().toString());
                messageList.add(messageObj);
            }
            BasicDBObject queryFollowings = new BasicDBObject("usuario_id", userDb.get("_id"));
            DBCursor listaSeguindo = seguindoObject.find(queryFollowings);
            while (listaSeguindo.hasNext()) {
                listaSeguindo.next();
                BasicDBObject queryOthersMessages = new BasicDBObject("usuario_id", listaSeguindo.curr().get("seguindo_id"));
                DBCursor mensagensOthers = mensagensObject.find(queryOthersMessages).sort(messageOrder);
                while (mensagensOthers.hasNext()) {
                    mensagensOthers.next();
                    JSONObject messageObj = new JSONObject(mensagensOthers.curr().toString());
                    messageList.add(messageObj);
                }
            }

        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }
        
        return messageList;
    }

    @Override
    public boolean follow(JSONObject user, String followUserName) throws Exception {
        boolean returnCode = false;
        try {
            connectToDatabase();
            DBObject followedUser = this.findUserInternal(followUserName);
            if (followedUser != null) {
                DBCollection seguindo = db.getCollection("seguindo");
                DBObject seguindoObject = new BasicDBObject();
                
                Object obj = JSON.parse(user.toString());
                DBObject dbUser = (DBObject) obj;
                
                seguindoObject.put("usuario_id", dbUser.get("_id"));
                seguindoObject.put("seguindo_id", followedUser.get("_id"));
                try {
                        BasicDBObject objIndex = new BasicDBObject();
                        objIndex.put("usuario_id", 1);
                        objIndex.put("seguindo_id", 1);
                        seguindo.ensureIndex(objIndex, "seguindoIdx", true);
                        seguindo.insert(seguindoObject);
                        returnCode = true;
                }
                catch (DuplicateKey dupl) {
                    returnCode = false;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }
        return returnCode;
    }
    
    @Override
    public boolean postMessage(JSONObject user, String message) throws Exception {
        boolean returnCode = false;
        try {
            connectToDatabase();
            DBObject userDb = this.convert(user);
            DBCollection mensagensObject = db.getCollection("mensagem");
            DBObject mensagem = new BasicDBObject();
            DBObject msgIndex = new BasicDBObject();
            msgIndex.put("usuario_id", 1);
            msgIndex.put("data", -1);
            mensagensObject.ensureIndex(msgIndex);
            mensagem.put("usuario_id", userDb.get("_id"));
            mensagem.put("data", new Date());
            mensagem.put("texto", message);
            mensagensObject.insert(mensagem);
            returnCode = true;
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }      
        
        return returnCode;
    }
    
    @Override
    public boolean abandon(JSONObject user, String followUserName) throws Exception {
        boolean returnCode = false;
        try {
            connectToDatabase();
            DBObject userDb = this.convert(user);
            DBObject followedUser = this.findUserInternal(followUserName);
            if (followedUser != null) {
                DBCollection seguindoObject = db.getCollection("seguindo");
                BasicDBObject queryFollowings = new BasicDBObject("usuario_id", userDb.get("_id"));
                queryFollowings.put("usuario_id", userDb.get("_id"));
                queryFollowings.put("seguindo_id", followedUser.get("_id"));
                WriteResult result = seguindoObject.remove(queryFollowings);
                if (result.getN() > 0) {
                    returnCode = true;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }          
        return returnCode;
    }
    

    
    @Override
    public boolean validateSession(JSONObject session) throws Exception {
        boolean returnCode = false;
        try {
            connectToDatabase();
            returnCode = this.isSessionValid(session);
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }         
        return returnCode;
    }
    
    private boolean isSessionValid(JSONObject session) throws Exception {
        boolean returnCode = false;
        try {
            DBObject sessionDb = this.convert(session);
            DBObject currentSession = this.getSessionFromDatabase(session);
            if (currentSession != null) {
                if (currentSession.get("_id").equals(sessionDb.get("_id"))) {
                    Date dataUltimoUpdate = (Date) currentSession.get("lastUpdate");
                    DateTime ultimoUpdate = new DateTime(dataUltimoUpdate);
                    DateTime agora = new DateTime(new Date());
                    Minutes minutos = Minutes.minutesBetween(ultimoUpdate, agora);
                    if (minutos.getMinutes() >= 15) {
                        DBCollection sessions = db.getCollection("session");
                        sessions.remove(currentSession);
                    }
                    else {
                        returnCode = true;
                    }
                }
                else {
                    returnCode = false;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        return returnCode;
    }
    
    private DBObject getSessionFromDatabase(JSONObject session) throws Exception {
        DBObject databaseSession = null;
        try {
            DBObject sessionDb = this.convert(session);
            BasicDBObject querySession = new BasicDBObject("_id", sessionDb.get("_id"));
            DBCollection sessions = db.getCollection("session");
            DBCursor curSession = sessions.find(querySession);
            if (curSession.count() > 0) {
                curSession.next();
                databaseSession = curSession.curr();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        return databaseSession;
    }

    @Override
    public JSONObject getSession(JSONObject user) throws Exception {
        JSONObject session = null;
        try {
            connectToDatabase();
            BasicDBObject dbUser = (BasicDBObject) this.convert(user);
            dbUser = (BasicDBObject) this.findUserInternal(dbUser.getString("username"));
            if (dbUser != null) {
                BasicDBObject newSession = new BasicDBObject();
                newSession.put("usuario_id", dbUser.get("_id"));
                Date agora = new Date();
                DBCollection sessions = db.getCollection("session");
                newSession.put("lastUpdate",agora);
                sessions.insert(newSession);
                session = new JSONObject(newSession.toString());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }          
        return session;
    }

    @Override
    public boolean updateSession(JSONObject session) throws Exception {
        boolean returnCode = false;
        try {
            connectToDatabase();
            if (this.isSessionValid(session)) {
                DBObject dbSession = this.convert(session);
                dbSession.put("lastupdate", new Date());
                BasicDBObject searchQuery = new BasicDBObject().append("_id", dbSession.get("_id"));
                DBCollection sessions = db.getCollection("session");
                sessions.update(searchQuery, dbSession);
                returnCode = true;    
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }          
        return returnCode;
    }
    
    @Override
    public JSONObject pullSession(JSONObject session) throws Exception {
        JSONObject masterSession = null;
        try {
            connectToDatabase();
            if (this.isSessionValid(session)) {
                BasicDBObject dbSession = (BasicDBObject) this.convert(session);
                if (dbSession != null) {
                    DBObject actualSession = this.getSessionFromDatabase(session);
                    if (actualSession != null) {
                        masterSession = new JSONObject(actualSession.toString());
                    }
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }          
        return masterSession;
    }
    
    @Override
    public JSONObject getUserFromSession(JSONObject session) throws Exception {
        JSONObject user = null;
        try {
            connectToDatabase();
            if (this.isSessionValid(session)) {
                BasicDBObject dbSession = (BasicDBObject) this.convert(session);
                if (dbSession != null) {
                    DBCollection usuarios = db.getCollection("usuarios");
                    BasicDBObject query = new BasicDBObject("_id", dbSession.get("usuario_id"));
                    DBCursor cursor = usuarios.find(query);
                    if (cursor.hasNext()) {
                        cursor.next();
                        user = new JSONObject(cursor.curr().toString());
                    }                    
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(); throw new Exception(ex);
        }
        finally {
            this.closeDb();
        }          
        return user;
    }
    
    private void connectToDatabase() {
        if (this.db == null) {
            try {
                String server = "localhost";
                int port = 27017;
                if (System.getProperty("dbServer") != null) {
                    server = System.getProperty("dbServer");    
                }
                if (System.getProperty("dbServerPort") != null) {
                    port = Integer.parseInt(System.getProperty("dbServerPort"));
                }
                
                mongoClient = new MongoClient( server , port );
                this.db = mongoClient.getDB( "microblog" );
            }
            catch (UnknownHostException e) {
                e.printStackTrace();
            }

        }
    }
    
    private void closeDb() {
        db = null;
        mongoClient.close();
    }
    
    public DaoMongoDB() {
        super();
    }





   










}
