package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.ContinueWatching
import java.util.Optional

@Repository
interface ContinueWatchingRepository : JpaRepository<ContinueWatching, Long> {
    fun findByProfileIdAndContentId(profileId: Long, contentId: String): Optional<ContinueWatching>
}

