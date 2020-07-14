package devils.scrumtool.repositories;

import devils.scrumtool.entities.User_has_Project;
import devils.scrumtool.entities.User_has_Project.UserHasProjectId;
// Java libraries
import java.util.Optional;
// Spring libraries
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface User_has_ProjectRepository
        extends CrudRepository<User_has_Project, UserHasProjectId> {
    // Query Methods
    Optional<User_has_Project> findByUserIdAndProjectId(Integer userId, Integer projectId);

    @Transactional
    void deleteByProjectId(Integer projectId);
}
