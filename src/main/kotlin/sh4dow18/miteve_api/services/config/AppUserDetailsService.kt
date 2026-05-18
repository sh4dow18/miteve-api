package sh4dow18.miteve_api.services.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import sh4dow18.miteve_api.entities.Role
import sh4dow18.miteve_api.entities.User
import sh4dow18.miteve_api.repositories.UserRepository

@Suppress("unused")
// AppUserDetailsService Service Class
// Spring Abstract App User Details Service
@Service
// Tag that establishes that is a Transactional Service. This one makes a transaction when
// this service is in operation.
@Transactional
class AppUserDetailsService(
    // App User Detail sService Props
    @Autowired
    val userRepository: UserRepository,
) : UserDetailsService {
    // Tag that allows to throw a Username Not Found Exception
    @Throws(UsernameNotFoundException::class)
    // Function that is used to the user details during the authentication
    override fun loadUserByUsername(email: String): UserDetails {
        val user: User = userRepository.findByEmail(email).orElse(null)
            ?: return org.springframework.security.core.userdetails.User(
                "Error", "Error", false, false, false,
                false, emptyList()
            )
        // Returns a Spring Security "User" with the "User" information found
        return org.springframework.security.core.userdetails.User(
            user.id.toString(), user.password, true, true, true,
            true, getAuthorities(user.role)
        )
    }
    // Function that get the privileges of the user as authorities
    private fun getAuthorities(role: Role): Collection<GrantedAuthority> {
        return role.privilegesList.map { privilege -> SimpleGrantedAuthority(privilege.slug) }
    }
}