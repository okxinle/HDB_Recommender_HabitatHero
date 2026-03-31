package habitathero.control;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import habitathero.entity.HDBBlock;
import habitathero.entity.UserSavedResults;
import habitathero.repository.UserSavedResultsRepository;

@Service
public class UserProfileDbManager {

    private final UserSavedResultsRepository userSavedResultsRepository;
    private final ObjectMapper objectMapper;

    public UserProfileDbManager(UserSavedResultsRepository userSavedResultsRepository,
                                ObjectMapper objectMapper) {
        this.userSavedResultsRepository = userSavedResultsRepository;
        this.objectMapper = objectMapper;
    }

    public void saveLatestResults(int userId, List<HDBBlock> rankedBlocks) {
        try {
            String json = objectMapper.writeValueAsString(rankedBlocks == null ? List.of() : rankedBlocks);
            UserSavedResults savedResults = new UserSavedResults(userId, json);
            userSavedResultsRepository.save(savedResults);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to persist member results.", e);
        }
    }

    public List<HDBBlock> getLatestResults(int userId) {
        try {
            return userSavedResultsRepository.findById(userId)
                .map(UserSavedResults::getResultsJson)
                .map(json -> {
                    try {
                        return objectMapper.readValue(json, new TypeReference<List<HDBBlock>>() {});
                    } catch (Exception ex) {
                        return Collections.<HDBBlock>emptyList();
                    }
                })
                .orElse(Collections.emptyList());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve member results.", e);
        }
    }
}
