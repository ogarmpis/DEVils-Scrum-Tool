package ys09.data;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
//import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ys09.model.*;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.jdbc.core.PreparedStatementSetter;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;



public class DataAccess {

    private static final int MAX_TOTAL_CONNECTIONS = 16;
    private static final int MAX_IDLE_CONNECTIONS = 8;

    private DataSource dataSource;
    private static JdbcTemplate jdbcTemplate;


    public void setup(String driverClass, String url, String user, String pass) throws SQLException {

        //initialize the data source
        BasicDataSource bds = new BasicDataSource();
        bds.setDriverClassName(driverClass);
        bds.setUrl(url);
        bds.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        bds.setMaxIdle(MAX_IDLE_CONNECTIONS);
        bds.setUsername(user);
        bds.setPassword(pass);
        bds.setValidationQuery("SELECT 1");
        bds.setTestOnBorrow(true);
        bds.setDefaultAutoCommit(true);

        //check that everything works OK
        bds.getConnection().close();

        //initialize the jdbc template utilitiy
        jdbcTemplate = new JdbcTemplate(bds);

        //keep the dataSource for the low-level manual example to function (not actually required)
        dataSource = bds;
    }

    // Singleton !

    public static JdbcTemplate getInstance(){
        return jdbcTemplate;
    }

    //@Transactional
    public Project insertProject(Project project, int idUser, String role) throws SQLException {
        // Insert an object to the projects array
        /*String query1 = "insert into Project (title, isDone, deadlineDate) values (?, ?, ?)";
        String query2 = "insert into Project_has_User (Project_id, User_id, role) values (?, ?, ?)";
        // Id is generated by database (Auto Increment)
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            //jdbcTemplate.update(query1, new Object[]{project.getTitle(), project.getIsDone(), project.getDeadlineDate()});
            KeyHolder keyHolder = new GeneratedKeyHolder();
            java.sql.Date sqlDate = new java.sql.Date(project.getDeadlineDate().getTime());

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query1, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, project.getTitle());
                    statement.setBoolean(2, project.getIsDone());
                    statement.setDate(3, sqlDate);
                    return statement;
                }
            }, keyHolder);
            // Return the new generated id for user
            int idProject = keyHolder.getKey().intValue();
            //System.out.println(idProject);
            project.setId(idProject);
            project.setDeadlineDate(sqlDate);

            jdbcTemplate.update(query2, new Object[]{idProject, idUser, role});
            return project;
          // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }*/

        String query1 = "insert into Project (title, isDone, deadlineDate) values (?, ?, ?)";
        String query2 = "insert into Project_has_User (Project_id, User_id, role) values (?, ?, ?)";

        PreparedStatement statement1 = null;
        PreparedStatement statement2 = null;
        Connection dbConnection = null;
        // Id is generated by database (Auto Increment)
        try {
            java.sql.Date sqlDate = new java.sql.Date(project.getDeadlineDate().getTime());

            dbConnection = dataSource.getConnection();
            dbConnection.setAutoCommit(false);

            statement1 = dbConnection.prepareStatement(query1, Statement.RETURN_GENERATED_KEYS);
            statement1.setString(1, project.getTitle());
            statement1.setBoolean(2, project.getIsDone());
            statement1.setDate(3, sqlDate);
            statement1.executeUpdate();

            ResultSet rs = statement1.getGeneratedKeys();
            long key = -1L;
            if (rs.next()) {
                key = rs.getLong(1);
            }
            // Return the new generated id for user
            int idProject = (int)key;
            System.out.println(idProject);
            project.setId(idProject);
            project.setDeadlineDate(sqlDate);

            // Update Project_has_User table
            statement2 = dbConnection.prepareStatement(query2);
            statement2.setInt(1, idProject);
            statement2.setInt(2, idUser);
            statement2.setString(3, role);
            statement2.executeUpdate();

            dbConnection.commit();      // Commit manually for single transaction
            return project;
        // Error in one of the insert statements
        } catch (SQLException e) {
            e.printStackTrace();
            dbConnection.rollback();
            return null;
        // Finally close statements and connection
        } finally {
            if (statement1 != null) {
                statement1.close();
            }
            if (statement2 != null) {
                statement2.close();
            }
            if (dbConnection != null) {
                dbConnection.close();
            }
        }
    }



    public List<Project> getProjects() {
        //TODO: Support limits SOS
        // Creates the id
        return jdbcTemplate.query("select * from Project", new ProjectRowMapper());
    }


    public int getUserProjectsNumber(int idUser, Boolean isDone) {
        //TODO: Support limits SOS
        // Creates the id
        String query = "select count(*) from Project where idProject in (select Project_id from Project_has_User where User_id = ?) and isDone = ?;";
        return jdbcTemplate.queryForObject(query, new Object[]{idUser, isDone}, Integer.class);
    }


    public List<Project> getUserProjects(int idUser, Limits limit, Boolean isDone){
        // Return all the projects belong to user with the above id
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = ?) and isDone = ? order by deadlineDate limit ?, ?;";
        return jdbcTemplate.query(query, new Object[]{idUser, isDone, limit.getStart(), limit.getCount()}, new ProjectRowMapper());
    }



    public Project getCurrentProject(int projectId){
        // Get current project Information
        String query = "select * from Project where idProject = ?;";
        Project projectItem = jdbcTemplate.queryForObject(query, new Object[]{projectId}, new ProjectRowMapper());
        return projectItem;
    }



    public Project updateCurrentProject(Project projectItem) {
        // Update an existing PBI
        String query = "update Project set title = ?, deadlineDate = ? where idProject = ?;";
        jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    java.sql.Date sqlDate = new java.sql.Date(projectItem.getDeadlineDate().getTime());
                    statement.setString(1, projectItem.getTitle());
                    statement.setDate(2, sqlDate);
                    statement.setInt(3, projectItem.getId());
                    return statement;
                }
            });
            // PBI's id is already in pbi class (as returned from frontend)
            return projectItem;
        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    // Find the PBIs (Epics or Stories)
    public List<PBI> getProjectPBIs(int idProject, Boolean isEpic, int epicId) {
        // Return the pbis asked for the current project
        //System.out.println(isEpic);
        List<PBI> pbis = new ArrayList<>();
        if (isEpic == true) {
            // Returns Epics
            String query = "select * from PBI where Project_id = :idProject and isEpic = :isEpic";
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("idProject", idProject);
            params.addValue("isEpic", isEpic);
            try {
                return namedJdbcTemplate.query(query, params, new PBIRowMapper());
            } catch(EmptyResultDataAccessException e) {
                return pbis;
            }
        }         // Returns User Stories on else
        else {
            String query = "select * from PBI where Project_id = :idProject and Epic_id = :epicId";
            NamedParameterJdbcTemplate namedJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("idProject", idProject);
            params.addValue("epicId", epicId);
            try {
                return namedJdbcTemplate.query(query, params, new PBIRowMapper());
            } catch(EmptyResultDataAccessException e) {
                return pbis;
            }
        }

    }


    public PBI insertNewPBI(PBI pbi) {
        // Insert a pbi into PBI table
        String query = "insert into PBI (title, description, priority, isEpic, Project_id, Epic_id, Sprint_id) values (?, ?, ?, ?, ?, ?, ?);";
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Id is generated by database (Auto Increment)
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, pbi.getTitle());
                    statement.setString(2, pbi.getDescription());
                    statement.setInt(3, pbi.getPriority());
                    statement.setBoolean(4, pbi.getIsEpic());
                    statement.setInt(5, pbi.getProject_id());
                    if (pbi.getEpic_id() != null)
                        statement.setInt(6, pbi.getEpic_id());
                    else statement.setNull(6, java.sql.Types.INTEGER);
                    if (pbi.getSprint_id() != null)
                        statement.setInt(7, pbi.getSprint_id());
                    else statement.setNull(7, java.sql.Types.INTEGER);
                    return statement;
                }
            }, keyHolder);

            // Return the new generated id for pbi
            int idPBI = keyHolder.getKey().intValue();
            System.out.println(idPBI);
            pbi.setIdPBI(idPBI);
            return pbi;

        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    public PBI updatePBI(PBI pbi) {
        // Update an existing PBI
        String query = "update PBI set title=?, description=?, priority=?, isEpic=?, Project_id=?, Epic_id=?, Sprint_id=? where idPBI=?;";
        jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, pbi.getTitle());
                    statement.setString(2, pbi.getDescription());
                    statement.setInt(3, pbi.getPriority());
                    statement.setBoolean(4, pbi.getIsEpic());
                    statement.setInt(5, pbi.getProject_id());
                    if (pbi.getEpic_id() != null)
                        statement.setInt(6, pbi.getEpic_id());
                    else statement.setNull(6, java.sql.Types.INTEGER);
                    if (pbi.getSprint_id() != null)
                        statement.setInt(7, pbi.getSprint_id());
                    else statement.setNull(7, java.sql.Types.INTEGER);
                    statement.setInt(8, pbi.getIdPBI());
                    return statement;
                }
            });
            // PBI's id is already in pbi class (as returned from frontend)
            return pbi;
        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Check if User exists into the database (either checking email or username)
    public boolean userExists(String mail) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);

        String query = "";
        if (mail.indexOf('@') < 0){
            query = "SELECT * FROM User WHERE username = ?";
        }
        else query = "SELECT * FROM User WHERE mail = ?";

        try {
            User exist = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());   // Exists
            return true;
        } catch (EmptyResultDataAccessException e) {
            return false;
        }        // Does not exists
    }


    // Insert User
    public User insertUser(User user) {
        // Generate Random Salt and Bcrypt
        String pw_hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(pw_hash);
        user.setIsAdmin(0);
        user.setNumOfProjects(0);
        // Insert into table with jdbc template
        // Avoid SQL injections
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String query = "INSERT INTO User(mail, username, firstname, lastname, password, isAdmin, numProjects) VALUES (?, ?, ?, ?, ?, ?, ?);";
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update(new PreparedStatementCreator() {
            @Override
            public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, user.getEmail());
                statement.setString(2, user.getUsername());
                statement.setString(3, user.getFirstName());
                statement.setString(4, user.getLastName());
                statement.setString(5, user.getPassword());
                statement.setInt(6, user.getIsAdmin());
                statement.setInt(7, user.getNumOfProjects());
                return statement;
            }
        }, keyHolder);
        // Return ther user with the new generated id
        user.setIdUser(keyHolder.getKey().intValue());
        return user;
    }


    public User updateUser(User profile, int userId) {
        // Update user profile
        String query = "update User set firstname=?, lastname=?, mail=?, job=?, company=?, country=?, Description=? where idUser=?;";
        jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, profile.getFirstName());
                    statement.setString(2, profile.getLastName());
                    statement.setString(3, profile.getEmail());
                    if (profile.getJob() != null)
                        statement.setString(4, profile.getJob());
                    else statement.setNull(4, java.sql.Types.VARCHAR);
                    if (profile.getCompany() != null)
                        statement.setString(5, profile.getCompany());
                    else statement.setNull(5, java.sql.Types.VARCHAR);
                    if (profile.getCountry() != null)
                        statement.setString(6, profile.getCountry());
                    else statement.setNull(6, java.sql.Types.VARCHAR);
                    if (profile.getDescription() != null)
                        statement.setString(7, profile.getDescription());
                    else statement.setNull(7, java.sql.Types.VARCHAR);
                    statement.setInt(8, userId);
                    return statement;
                }
            });
            // PBI's id is already in pbi class (as returned from frontend)
            return profile;
        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return null;
        }
    }



    public List<Notification> getUserNotifications(int userId){
        // Get current project Information
        String query = "select * from Notification where ToUserEmail in (select mail from User where idUser = ?) and type = 'Accept/Decline';";
        return jdbcTemplate.query(query, new Object[]{userId}, new NotificationRowMapper());
    }


    public Boolean insertNewNotification(Notification invitation, int userId) {
        // Insert a notification (the invitation) in notification table
        String query = "insert into Notification (Project_id, projectTitle, role, FromUsername, ToUserEmail, type, text) values (?, ?, ?, ?, ?, ?, ?);";
        jdbcTemplate = new JdbcTemplate(dataSource);

        // Id is generated by database (Auto Increment)
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    PreparedStatement statement = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                    if (invitation.getProject_id() > 0)
                        statement.setInt(1, invitation.getProject_id());
                    else statement.setNull(1, java.sql.Types.INTEGER);
                    if (invitation.getProjectTitle() != null)
                        statement.setString(2, invitation.getProjectTitle());
                    else statement.setNull(2, java.sql.Types.VARCHAR);
                    if (invitation.getRole() != null)
                        statement.setString(3, invitation.getRole());
                    else statement.setNull(3, java.sql.Types.VARCHAR);
                    if (invitation.getFromUsername() != null)
                        statement.setString(4, invitation.getFromUsername());
                    else statement.setNull(4, java.sql.Types.VARCHAR);
                    statement.setString(5, invitation.getToUserEmail());
                    statement.setString(6, invitation.getType());
                    if (invitation.getText() != null)
                        statement.setString(7, invitation.getText());
                    else statement.setNull(7, java.sql.Types.VARCHAR);
                    return statement;
                }
            }, keyHolder);

            return true;

        // Error in update of jdbcTemplate
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return false;
        }
    }


    public int deleteUserNotification(int idNotification) {
        // Insert a notification (the invitation) in notification table
        String query = "delete from Notification where idNotification = ?;";
        return jdbcTemplate.update(query, new Object[]{idNotification});

    }



    public List<PBI> getSprintStories(int sprintId) {
        // Find the Stories that belong to a specific Sprint
        String query = "select * from PBI where Sprint_id = ?";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new PBIRowMapper());
    }

    public List<PBI> getOnlySprintStories(int sprintId) {
        // Find the Stories that belong to a specific Sprint
        String query = "select * from PBI where Sprint_id = ? and isEpic = false";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new PBIRowMapper());
    }


    public List<Task> getSprintTasks(int sprintId) {
        // Find the Tasks belong to a specific Sprint
        String query = "select * from Task where PBI_id in (select idPBI from PBI where Sprint_id = ?);";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new TaskRowMapper());
    }


    public List<Issue> getSprintIssues(int sprintId) {
        // Find the Issues of current sprint's tasks
        String query = "select * from Issue where Task_id in (select idTask from Task where PBI_id in (select idPBI from PBI where Sprint_id = ?));";
        return jdbcTemplate.query(query, new Object[]{sprintId}, new IssueRowMapper());
    }

    public String getMemberRole(int userId, int projectId) {
        // Find the role of a specific member in project
        String query = "select role from Project_has_User where User_id = ? and Project_id = ?;";
        return jdbcTemplate.queryForObject(query, new Object[]{userId, projectId}, String.class);
    }


    public User checkSignIn(SignIn signin) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);

        String query = "SELECT * FROM User WHERE mail = ?";
        String mail = signin.getEmail();

        try {
            User user = jdbcTemplate.queryForObject(query, new Object[]{mail}, new UserRowMapper());

            if (BCrypt.checkpw(signin.getPassword(), user.getPassword())) {
                System.out.println("It matches");
                // If it matches return JWT token !
                // Save the token to a dictionary (user,token)
                return user;
            } else {
                System.out.println("It does not match");
                return null;
            }
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // Check User Email and Password

    public List<Project> getUserProjectsRole (int idUser, String role) {
        // Return all the projects belong to user with the above id

        List<Project> projects = new ArrayList<>();
        String query = "select * from Project where idProject in (select Project_id from Project_has_User where User_id = ? and Project_has_User.role=?)";
        jdbcTemplate = new JdbcTemplate(dataSource);
        try {
            return jdbcTemplate.query(query, new Object[]{idUser, role}, new ProjectRowMapper());
        } catch (EmptyResultDataAccessException e) {
            return projects;
        }
    }


    public List<Integer> createAuthProjectList (int id, String role)
    {
        List<Project> projectsByRole = getUserProjectsRole(id,role);
        List<Integer> projectsByRoleID = new ArrayList<Integer>();
        for (Project project: projectsByRole)
        {
            projectsByRoleID.add(project.getId());   //make list with the ids' of projects in which
            //the current user is owner
        }
        return projectsByRoleID;
    }


    public User getUserProfile(String username) {
        String query = "select * from User where username = ?;";
        User profile = jdbcTemplate.queryForObject(query,new Object[]{username},new UserRowMapper());
        return profile;
    }


    public boolean passwordMatches(int id,String password) {
        // Query to find if user exists
        jdbcTemplate = new JdbcTemplate(dataSource);

        String query = "SELECT * FROM User WHERE idUser = ?";

        try {
            User user = jdbcTemplate.queryForObject(query, new Object[]{id}, new UserRowMapper());

            if (BCrypt.checkpw(password, user.getPassword())) {
                System.out.println("It matches");
                // If it matches return JWT token !
                // Save the token to a dictionary (user,token)
                return true;
            } else {
                System.out.println("It does not match");
                return false;
            }
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }
}
