package com.obomprogramador.discoarq.microblog.main;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.obomprogramador.discoarq.microblog.persistence.JsonDao;
import com.obomprogramador.discoarq.microblog.persistence.implementation.DaoMongoDB;

/**
 * RESTful Web service que recebe parâmetros JSON e se comunica com o 
 * DAO MongoDB.
 * Roda com um container Jetty embutido.
 * 
 * @author Cleuton Sampaio
 *
 */
@Path("/server")
public class MicroBlog {
    
    private JsonDao dao = new DaoMongoDB();
    private static Server server;
    
    /**
     * Obtem uma nova sessão (Logon).
     * @param userName String
     * @param password String
     * @return JSONObject sessão.
     */
    @GET
    @Path("session/{username}/{password}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSession(@PathParam("username") String userName, 
                             @PathParam("password") String password) {
        String resposta = null;
        try {
            JSONObject dbUsuario = dao.findUser(userName, password);
            if (dbUsuario != null) {
                JSONObject sessao = dao.getSession(dbUsuario);
                resposta = sessao.toString();
            }
            else {
                resposta = this.formatError("Invalid user: " + userName).toString();
            }
        }
        catch (Exception ex) {
            resposta = this.formatError("Exception ao obter sessao: " + ex.toString()).toString(); 
        }
        return resposta;
    }
    
    /**
     * Retorna o JSONObject do usuário que está logado na sessão.
     * @param session
     * @return
     */
    @POST
    @Path("userdata")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getUserFromSession(String session) { 
        String resposta = null;
        try {
            JSONObject usuario = dao.getUserFromSession(new JSONObject(session));
            if (usuario != null) {
                resposta = usuario.toString();
            }
        }
        catch (Exception ex) {
            resposta = this.formatError("Exception ao obter usuario: " + ex.toString()).toString();
        }
        return resposta;
    }
    
    /**
     * Retorna a lista de mensagens.
     * @param session
     * @return
     */
    @POST
    @Path("messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getMessages(String session) { 
        String resposta = null;
        JSONObject dbSession = new JSONObject(session);
        try {
            if (dao.validateSession(dbSession)) {
                JSONObject usuario = dao.getUserFromSession(new JSONObject(session));
                if (usuario != null) {
                    List<JSONObject> mensagens = dao.getMessages(usuario);
                    if (mensagens != null) {
                        Gson gson = new Gson();
                        resposta = gson.toJson(mensagens);                        
                    }
                }
            }
            
        }
        catch (Exception ex) {
            resposta = this.formatError("Exception ao obter usuario: " + ex.toString()).toString();
        }
        return resposta;
    }
    
    /**
     * Adiciona um novo usuário ao microblog.
     * @param usuario JSON String
     * @return JSONObject usuário.
     */
    @POST
    @Path("user")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getSession(String usuario) {
        String resposta = null;
        try {
            JSONObject dbUsuario = new JSONObject(usuario);
            if (dao.addUser(dbUsuario)) {
                dbUsuario = dao.findUser(dbUsuario.getString("username"), 
                        dbUsuario.getString("password"));
                resposta = dbUsuario.toString();
            }
            else {
                resposta = this.formatError("Error adding user").toString();
            }
        }
        catch (Exception ex) {
            resposta = this.formatError("Exception ao inserir usuario: " + ex.toString()).toString(); 
        }
        return resposta;
    }
    
    private JSONObject formatError(String message) {
        JSONObject errorMsg = new JSONObject();
        errorMsg.put("status", "*** ERRO ***");
        errorMsg.put("mensagem", message);
        return errorMsg;
    }
    
    public static void main(String[] args) throws Exception {
        server = new Server(8080);
        WebAppContext context = new WebAppContext();
        context.setDescriptor("src/main/webapp/WEB-INF/web.xml");
        context.setResourceBase("src/main/webapp");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        server.start();
        server.join();
            
    }
    
}
