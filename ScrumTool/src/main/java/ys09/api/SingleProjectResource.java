package ys09.api;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import ys09.auth.CustomAuth;
import ys09.conf.Configuration;
import ys09.data.DataAccess;
import ys09.data.Limits;
import ys09.model.Project;
import java.io.Reader;
import org.restlet.resource.Post;
import org.restlet.ext.json.JsonRepresentation;
import org.json.JSONObject;
import com.google.gson.Gson;
import org.restlet.representation.BufferingRepresentation;
import org.restlet.data.ClientInfo;
import org.restlet.engine.header.Header;
import org.restlet.util.Series;
import java.io.IOException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.restlet.Message;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.util.Series;

import static org.restlet.data.Status.CLIENT_ERROR_UNAUTHORIZED;


public class SingleProjectResource extends ServerResource {

    private final DataAccess dataAccess = Configuration.getInstance().getDataAccess();

    @Override
    protected Representation get() throws ResourceException {

        // New map string (which is the json name) and objects
        Map<String, Object> map = new HashMap<>();
        Map<String, String> mapError = new HashMap<>();

        //get param from request
        String userId = getRequestAttributes().get("userId").toString();
        String projectId = getRequestAttributes().get("projectId").toString();
        int user = Integer.parseInt(userId);

        // Get project of the requested user

        //call DAO with parameter

        /*
        // Access the headers of the request !
        Series requestHeaders = (Series)getRequest().getAttributes().get("org.restlet.http.headers");
        String token = requestHeaders.getFirstValue("auth");

        if (token == null) {
            mapError.put("error", "Client Error Unauthorized");
            return new JsonMapRepresentation(mapError);
        }

        CustomAuth customAuth = new CustomAuth();

        if(customAuth.checkAuthToken(token)) {
            // Get Projects only for the current user
            // Show them in the index page
            List<Project> projects = dataAccess.getProjects();
            map.put("results", projects);
            // Set the response headers
            return new JsonMapRepresentation(map);
        }
        else {
            mapError.put("error", "Client Error Unauthorized");
            return new JsonMapRepresentation(mapError);
        }
        */
        //map.put("start", xxx);
        //map.put("count", xxx);
        //map.put("total", xxx);
        // all the projects with a string

        // For expirimentation
        //List<Project> projects = dataAccess.getUserProjects(user);
        //map.put("results", projects);
        // Set the response headers
        return new JsonMapRepresentation(map);

    }




    // Post Representation
    @Override
    protected Representation post(Representation entity) throws ResourceException {

        //Foo targetObject = new Gson().fromJson(entity, Project.class);
        System.out.println(entity);
        try {
            // Get the json representation from the frontend
            String str = entity.getText();
            //System.out.println(str);
            Map<String, Object> map = new HashMap<>();
            map.put("results", entity);
            // Now Create from String the JAVA object
            Gson gson = new Gson();
            Project project = gson.fromJson(str, Project.class);

            // Insert the Project to the database
            dataAccess.insertProject(project, 1, "Developer");

            Map<String, Object> mapError = new HashMap<>();
            mapError.put("results", project);
            return new JsonMapRepresentation(mapError);
        }

        catch(IOException e) {
            Map<String, String> mapError = new HashMap<>();
            mapError.put("result", "System Exception");
            return new JsonMapRepresentation(mapError);
        }
    }
}
