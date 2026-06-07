package sh4dow18.miteve_api.services.history

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.dtos.history.HistoryResponse
import sh4dow18.miteve_api.errors.NoExists
import sh4dow18.miteve_api.mappers.HistoryMapper
import sh4dow18.miteve_api.repositories.HistoryRepository
import sh4dow18.miteve_api.repositories.ProfileRepository

// Spring Abstract History Service
@Service
class AbstractHistoryService(
    @Autowired val historyRepository: HistoryRepository,
    @Autowired val profileRepository: ProfileRepository,
    @Autowired val historyMapper: HistoryMapper
) : HistoryService {

    @Transactional(readOnly = true)
    override fun findAllByProfileId(profileId: Long): List<HistoryResponse> {
        if (!profileRepository.existsById(profileId)) {
            throw NoExists("$profileId", "Profile")
        }
        return historyMapper.historyListToHistoryResponseList(
            historyRepository.findAllByProfileId(profileId)
        )
    }
}
