package sh4dow18.miteve_api.config

// Security Configuration Dependencies
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
@Suppress("unused")
// Tag that establishes that this configuration would be only for "init" spring profile
@Profile("init")
// Tag that establishes that this would be a Spring Configuration
@Configuration
// Tag that establishes that this would activate the Spring Web Security and this one
// personalize the original spring security configuration
@EnableWebSecurity
// Close Security Configuration class definition
class CloseSecurityConfiguration{
    // Tag that establishes that this is a Spring Bean
    @Bean
    // Function that creates and configure the Spring Security Filter Chain
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // It disables the protection for Cross-Site Request Forgery (CSRF) Attacks
            .csrf { csrf ->
                csrf.disable()
            }
            // It establishes that any request would be authenticated to access to the information
            .authorizeHttpRequests {
                it
                    .anyRequest().authenticated()
            }
        // Create the Spring Security Filter Chain
        return http.build()
    }

}
@Suppress("unused")
// Tag that establishes that this configuration would be only for "dev" spring profile
@Profile("!init")
// Tag that establishes that this would be a Spring Configuration
@Configuration
// Tag that establishes that this would activate the Spring Web Security and this one
// personalize the original spring security configuration
@EnableWebSecurity
// Tag that establishes that the security level would be in method level
@EnableMethodSecurity
// Jwt Security Configuration class definition
class JwtSecurityConfiguration(private val authenticationConfiguration: AuthenticationConfiguration) {
    @Value("\${ip.domain}")
    lateinit var ipDomain: String
    @Value("\${server.port}")
    lateinit var port: String
    // Tag that establishes that this is a Spring Bean
    @Bean
    // Creates and configure an Authentication Manager for Spring Security
    fun authenticationManager(): AuthenticationManager {
        return authenticationConfiguration.authenticationManager
    }
    // Tag that establishes that this is a Spring Bean
    @Bean
    // Creates and configure an BCrypt Password Encoder for Spring Security
    fun passwordEncoder(): BCryptPasswordEncoder? {
        return BCryptPasswordEncoder()
    }
    // Tag that establishes that this is a Spring Bean
    @Bean
    // Function that creates and configure the Spring Security Filter Chain
    fun filterChain(http: HttpSecurity, authenticationManager: AuthenticationManager): SecurityFilterChain {
        http
            // It disables the protection for Cross-Site Request Forgery (CSRF) Attacks
            .csrf { it.disable() }
            // It establishes the CORS Configuration Source
            .cors { it.configurationSource(corsConfigurationSource()) }
            // It establishes the request that are allowed and how these are allowed
            .authorizeHttpRequests {
                // Permit all the requests to check in each endpoint
                it.anyRequest().permitAll()
            }
            // It establishes the Session Management Policy as Stateless, that means that would not
            // have sessions in the server, this is because the application would use JWT
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            // Applies an Additional Custom Security Configurations
            .with(AppCustomDsl(authenticationManager)) {}
        // Create the Spring Security Filter Chain
        return http.build()
    }
    // Tag that establishes that this is a Spring Bean
    @Bean
    // Creates and Configure a CORS Configuration Source that defines the origins, headers
    // and methods that are allowed in the requests
    fun corsConfigurationSource(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:3000", "http://localhost:3001", "http://localhost:${port}", ipDomain)
        config.allowedHeaders = listOf("*")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.exposedHeaders = listOf("Authorization")
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
// App Custom DSL Class Definition that extends of "AbstractHttpConfigurer" that is used to
// custom the Spring security configuration
class AppCustomDsl(private val authenticationManager: AuthenticationManager) : AbstractHttpConfigurer<AppCustomDsl, HttpSecurity>() {
    // Function that is used to configure specific aspects in the security
    override fun init(http: HttpSecurity) {
        // Add the Additional Security Filters created in Security File
        http.addFilterBefore(JwtAuthorizationFilter(authenticationManager), BasicAuthenticationFilter::class.java)
        http.addFilter(JwtAuthenticationFilter(authenticationManager))
    }
}