package biocode.fims.service;

import biocode.fims.entities.User;
import biocode.fims.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Service class for handling {@link User} persistence
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void create(User user) {
        userRepository.save(user);

    }

    public void update(User user) {
        userRepository.save(user);
    }

    public User getUser(int userId) {
        return userRepository.findByUserId(userId);
    }
}
