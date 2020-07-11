package scrumtool.api.resource;

import scrumtool.auth.CustomAuth;
import scrumtool.conf.Configuration;
import scrumtool.data.DataAccess;
import scrumtool.data.entities.SprintDB;
import scrumtool.data.entities.TeamDB;
import scrumtool.model.PBI;
import scrumtool.model.Sprint;
import scrumtool.model.Team;
import scrumtool.api.representation.JsonMapRepresentation;

import org.restlet.representation.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StoriesResource extends ServerResource {

    private final DataAccess dataAccess = Configuration.getInstance().getDataAccess();

    protected Representation get() throws ResourceException {
        Map<String, Object> map = new HashMap<>();
        Map<String, String> mapError = new HashMap<>();

        // Get UserId
        String userIdStr = getRequestAttributes().get("userId").toString();
        if (userIdStr.equals("null")) {
            mapError.put("error", "Unauthorized projects");
            return new JsonMapRepresentation(mapError);
        }
        int userId = Integer.parseInt(userIdStr);

        // Get the projectId
        String projectIdStr = getRequestAttributes().get("projectId").toString();
        if (projectIdStr.equals("null")) {
            mapError.put("error", "Unauthorized projects");
            return new JsonMapRepresentation(mapError);
        }
        int projectId = Integer.parseInt(projectIdStr);

        // Access the headers of the request!
        Series requestHeaders = (Series)getRequest().getAttributes().get("org.restlet.http.headers");
        String token = requestHeaders.getFirstValue("auth");

        if (token == null) {
            mapError.put("error", "null");
            return new JsonMapRepresentation(mapError);
        }
        CustomAuth customAuth = new CustomAuth();

        // Check if the token is ok
        if (customAuth.checkUserAuthToken(token, userIdStr)) {
            // Insert the pbi given (either epic or story)
            if (dataAccess.userMemberOfProject(userId, projectId)) {
                // Find the team members
                boolean flag = false;
                TeamDB teamDB = new TeamDB();
                List<Team> teamList = teamDB.getTeamMembers(projectId);
                for(Team member:teamList) {
                    if(member.getIdUser() == userId) {
                        flag = true;
                    }
                }

                if (flag) {
                    // If id is in the team members' list
                    // Return sprints data

                    // Check sprint id
                    String sprintIdStr = getQuery().getValues("sprintId");
                    if (sprintIdStr == null) {
                        mapError.put("error", "Missing sprintId Query Parameter");
                        return new JsonMapRepresentation(mapError);
                    }
                    else {
                        int sprintId = Integer.parseInt(sprintIdStr);
                        // Find sprint's pbis
                        List<PBI> stories = dataAccess.getOnlySprintStories(sprintId);
                        map.put("stories", stories);
                        return new JsonMapRepresentation(map);
                    }
                } else {
                    mapError.put("error", "Unauthorized sprint access");
                    return new JsonMapRepresentation(mapError);
                }
            } else {
                mapError.put("error", "Unauthorized projects");
                return new JsonMapRepresentation(mapError);
            }
        } else {
            mapError.put("error", "Unauthorized user");
            return new JsonMapRepresentation(mapError);
        }
    }
}