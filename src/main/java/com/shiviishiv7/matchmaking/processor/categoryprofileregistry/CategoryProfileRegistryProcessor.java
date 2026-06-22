package com.shiviishiv7.matchmaking.processor.categoryprofileregistry;

import com.shiviishiv7.matchmaking.common.enums.MatchCategory;
import com.shiviishiv7.matchmaking.common.exception.MatchmakingException;
import com.shiviishiv7.matchmaking.provider.implementation.CategoryProfileRegistryRepository;
import com.shiviishiv7.matchmaking.provider.model.CategoryProfileRegistry;
import com.shiviishiv7.matchmaking.provider.vo.BaseVO;
import com.shiviishiv7.matchmaking.provider.vo.CategoryProfileRegistryVO;
import com.shiviishiv7.matchmaking.provider.vo.MatchFilterVO;
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
    public BaseVO add(MatchFilterVO vo) throws MatchmakingException {
        try {
            log.info("Validating inputs for registry entry creation. {}", vo.toString());
            vo.validate();

            log.trace("Saving registry entry for cognitoSub: {}", vo.getCognitoSub());
            CategoryProfileRegistry registry = new CategoryProfileRegistry();
            registry.setCognitoSub(vo.getCognitoSub());
            registry.setMatchCategory(MatchCategory.valueOf(vo.getChildCategory().trim().toUpperCase()));
            registry.setPreferredGender(vo.getPreferredGender());
            registry.setPreferredCity(vo.getPreferredCity());
            registry.setPreferredState(vo.getPreferredState());
            registry.setPreferredCountry(vo.getPreferredCountry());
            registry.setMaxTimezoneOffsetHours(vo.getMaxTimezoneOffsetHours());
            registry.setSameCompanyAllowed(vo.getSameCompanyAllowed());
            registry.setCreatedAt(LocalDateTime.now());
            registry = registryRepository.save(registry);
            log.info("Registry entry saved for cognitoSub: {} category: {}", registry.getCognitoSub(), registry.getMatchCategory());

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

            Optional<CategoryProfileRegistry> fromDB = registryRepository.findById(vo.getId());
            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }

            CategoryProfileRegistry registry = fromDB.get();
            registry.setPreferredGender(vo.getPreferredGender());
            registry.setPreferredCity(vo.getPreferredCity());
            registry.setPreferredState(vo.getPreferredState());
            registry.setPreferredCountry(vo.getPreferredCountry());
            registry.setMaxTimezoneOffsetHours(vo.getMaxTimezoneOffsetHours());
            if (vo.getSameCompanyAllowed() != null) registry.setSameCompanyAllowed(vo.getSameCompanyAllowed());
            if (vo.getCompletionPct() != null) registry.setCompletionPct(vo.getCompletionPct());
            if (vo.getIsActive() != null) registry.setIsActive(vo.getIsActive());

            registry = registryRepository.save(registry);
            log.info("Registry entry updated for ID: {}", registry.getId());

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
            Optional<CategoryProfileRegistry> fromDB = registryRepository.findById(Integer.valueOf(id));
            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            return new BaseVO(SUCCESS, "Registry entry fetched", "Registry entry fetched", fromDB.get().toVO());
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getAllByUserId(String cognitoSub) throws MatchmakingException {
        try {
            List<CategoryProfileRegistryVO> voList = registryRepository.findByCognitoSub(cognitoSub)
                    .stream().map(CategoryProfileRegistry::toVO).collect(Collectors.toList());
            return new BaseVO(SUCCESS, "Registry entries fetched", "Registry entries fetched", voList);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching registry entries. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching registry entries: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO getActiveByUserId(String cognitoSub) throws MatchmakingException {
        try {
            List<CategoryProfileRegistryVO> voList = registryRepository
                    .findByCognitoSubAndIsActive(cognitoSub, true)
                    .stream().map(CategoryProfileRegistry::toVO).collect(Collectors.toList());
            return new BaseVO(SUCCESS, "Active registry entries fetched", "Active registry entries fetched", voList);
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error fetching active registry entries. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error fetching active registry entries: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO deactivate(String cognitoSub, String matchCategory) throws MatchmakingException {
        try {
            MatchCategory category = MatchCategory.valueOf(matchCategory.toUpperCase());
            Optional<CategoryProfileRegistry> fromDB = registryRepository
                    .findByCognitoSubAndMatchCategory(cognitoSub, category);
            if (fromDB.isEmpty()) {
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            fromDB.get().setIsActive(false);
            registryRepository.save(fromDB.get());
            return new BaseVO(SUCCESS, "Registry entry deactivated", "Registry entry deactivated");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error deactivating registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error deactivating registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }

    @Override
    public BaseVO delete(String id) throws MatchmakingException {
        try {
            if (!registryRepository.existsById(Integer.valueOf(id))) {
                throw new MatchmakingException("Registry entry does not exist", DATA_NOT_FOUND);
            }
            registryRepository.deleteById(Integer.valueOf(id));
            return new BaseVO(SUCCESS, "Registry entry deleted", "Registry entry deleted");
        } catch (MatchmakingException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("ALERT_FOR_ERROR: Error deleting registry entry. Error: {}", ex.getMessage(), ex);
            throw new MatchmakingException("Error deleting registry entry: " + ex.getMessage(), UNKNOWN_EXCEPTION);
        }
    }
}
