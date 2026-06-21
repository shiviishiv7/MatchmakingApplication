package com.shiviishiv7.matchmaking.processor.userprofile;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;

import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.shiviishiv7.matchmaking.common.constants.MatchmakingHttpStatus.*;

@Component
@Transactional
@Slf4j
public class CategoryProfileRegistryProcessor implements ICategoryProfileRegistryProcessor {

    @Autowired
    private CategoryProfileRegistryRepository registryRepository;

    @Override
    public BaseVO add(CategoryProfileRegistryVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for registry entry creation. {}", vo.toString());
            vo.validate();
            log.info("CategoryProfileRegistryVO validation completed successfully.");

            log.trace("Checking for duplicate registry entry for userId: {} and category: {}", vo.getUserId(), vo.getMatchCategory());
            if (registryRepository.existsByUserIdAndMatchCategory(vo.getUserId(), vo.getMatchCategory())) {
                log.error("ALERT_FOR_ERROR: Registry entry already exists for userId: {} and category: {}", vo.getUserId(), vo.getMatchCategory());
                throw new MatchmakingException("Registry entry already exists for this user and category", DUPLICATE_RECORD);
            }

            log.trace("Saving registry entry for userId: {}", vo.getUserId());
            CategoryProfileRegistry registry = new CategoryProfileRegistry();
            registry.fromVO(vo);
            registry.setCreatedAt(LocalDateTime.now());
            registry = registryRepository.save(registry);
            log.info("Registry entry saved successfully for userId: {} category: {}", registry.getUserId(), registry.getMatchCategory());

            return new BaseVO(SUCCESS, "Registry entry created", "Registry entry created", registry.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while adding registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while adding registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO update(CategoryProfileRegistryVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for registry entry update.");
            if (vo.getId() == null) {
                throw new MatchmakingException("Registry ID cannot be null for update", VALIDATION_ERROR);
            }
            vo.validate();

            log.trace("Fetching existing registry entry for ID: {}", vo.getId());
            Optional<CategoryProfileRegistry> fromDB = registryRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Registry entry not found for ID: {}", vo.getId());
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }

            CategoryProfileRegistry registry = fromDB.get();
            log.trace("Registry entry found for ID: {}. Applying updates.", vo.getId());

            registry.setExtensionProfileId(vo.getExtensionProfileId());
            registry.setCompletionPct(vo.getCompletionPct());
            if (vo.getIsActive() != null) {
                registry.setIsActive(vo.getIsActive());
            }

            registry = registryRepository.save(registry);
            log.info("Registry entry updated successfully for ID: {}", registry.getId());

            return new BaseVO(SUCCESS, "Registry entry updated", "Registry entry updated", registry.toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while updating registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while updating registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getById(String id) throws MatchmakingException {
        try {
            log.info("Fetching registry entry for ID: {}", id);
            Optional<CategoryProfileRegistry> fromDB = registryRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Registry entry not found for ID: {}", id);
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            log.info("Registry entry found for ID: {}", id);
            return new BaseVO(SUCCESS, "Registry entry fetched", "Registry entry fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAllByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching all registry entries for userId: {}", userId);
            List<CategoryProfileRegistry> list = registryRepository.findByUserId(Integer.valueOf(userId));
            List<CategoryProfileRegistryVO> voList = list.stream()
                    .map(CategoryProfileRegistry::toVO)
                    .collect(Collectors.toList());
            log.info("Found {} registry entries for userId: {}", voList.size(), userId);
            return new BaseVO(SUCCESS, "Registry entries fetched", "Registry entries fetched", voList);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching registry entries by userId. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching registry entries: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getActiveByUserId(String userId) throws MatchmakingException {
        try {
            log.info("Fetching active registry entries for userId: {}", userId);
            List<CategoryProfileRegistry> list = registryRepository.findByUserIdAndIsActive(Integer.valueOf(userId), true);
            List<CategoryProfileRegistryVO> voList = list.stream()
                    .map(CategoryProfileRegistry::toVO)
                    .collect(Collectors.toList());
            log.info("Found {} active registry entries for userId: {}", voList.size(), userId);
            return new BaseVO(SUCCESS, "Active registry entries fetched", "Active registry entries fetched", voList);
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while fetching active registry entries. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while fetching active registry entries: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO deactivate(String userId, String matchCategory) throws MatchmakingException {
        try {
            log.info("Deactivating registry entry for userId: {} category: {}", userId, matchCategory);
            MatchCategory category = MatchCategory.valueOf(matchCategory.toUpperCase());
            Optional<CategoryProfileRegistry> fromDB = registryRepository.findByUserIdAndMatchCategory(
                    Integer.valueOf(userId), category);
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Registry entry not found for userId: {} category: {}", userId, matchCategory);
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            CategoryProfileRegistry registry = fromDB.get();
            registry.setIsActive(false);
            registryRepository.save(registry);
            log.info("Registry entry deactivated for userId: {} category: {}", userId, matchCategory);
            return new BaseVO(SUCCESS, "Registry entry deactivated", "Registry entry deactivated");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deactivating registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deactivating registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            log.info("Deleting registry entry for ID: {}", id);
            Optional<CategoryProfileRegistry> fromDB = registryRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                log.error("ALERT_FOR_ERROR: Registry entry not found for ID: {}", id);
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            registryRepository.deleteById(Integer.valueOf(id));
            log.info("Registry entry deleted for ID: {}", id);
            return new BaseVO(SUCCESS, "Registry entry deleted", "Registry entry deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error occurred while deleting registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error occurred while deleting registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
