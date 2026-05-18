package sh4dow18.miteve_api.repositories

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import sh4dow18.miteve_api.entities.User
import java.util.Optional

@Repository
interface UserRepository: JpaRepository<User, Long> {
    fun findByEmail(@Param("email") email: String): Optional<User>
}