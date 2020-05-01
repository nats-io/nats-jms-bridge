package io.nats.bridge.admin.security

import io.nats.bridge.admin.models.logins.LoginToken
import io.nats.bridge.admin.util.JwtUtils
import io.nats.bridge.admin.util.LoginUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@Configuration
@EnableWebSecurity
class WebSecurityConfig(@Value("\${security.secretKey}") val adminSecretKey: String) : WebSecurityConfigurerAdapter() {


    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterBefore(JWTAuthorizationFilter(adminSecretKey), AnonymousAuthenticationFilter::class.java)
                .cors().disable()
                .csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
    }
}

class JwtAuthentication(private val loginToken: LoginToken) : Authentication {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        val roles: Array<String> = loginToken.roles.map { it.name }.toTypedArray()
        return AuthorityUtils.createAuthorityList(*roles)
    }

    override fun setAuthenticated(isAuthenticated: Boolean) {}
    override fun getName(): String {
        return loginToken.subject
    }

    override fun getCredentials(): Any {
        return loginToken.subject
    }

    override fun getPrincipal(): Any {
        return loginToken.subject
    }

    override fun isAuthenticated(): Boolean {
        return true
    }

    override fun getDetails(): Any {
        return loginToken.roles
    }
}

class JWTAuthorizationFilter(private val secret: String) : Filter {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val protectedUrlsMatcher = OrRequestMatcher(
            AntPathRequestMatcher("/api/v1/auth/**"),
            AntPathRequestMatcher("/api/v1/logins/**"),
            AntPathRequestMatcher("/api/v1/bridges/**")
    )
    private val adminUsersMatcher = OrRequestMatcher(
            AntPathRequestMatcher("/api/v1/logins/admin/**"),
            AntPathRequestMatcher("/api/v1/bridges/admin/**"),
            AntPathRequestMatcher("/api/v1/control/bridges/admin/**")
    )

    override fun doFilter(request: ServletRequest?, response: ServletResponse?, chain: FilterChain?) {
        val httpServletRequest = request as HttpServletRequest
        if (adminUsersMatcher.matches(httpServletRequest)) {
            logger.info("Admin Role Protected")
            val header: String? = httpServletRequest.getHeader("Authorization")
            val hasToken = hasJwtToken(header, response)
            val loginToken: LoginToken? = readLoginTokenFromJwtToken(hasToken, header)
            if (loginToken != null) {
                if (loginToken.roles.find { it.name == "Admin" } == null) {
                    (response as HttpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED,
                            "${loginToken.subject} is not authorized")

                    return
                }
            }
            if (hasToken) {
                placeSecurityContext(loginToken, response)
            }
        } else if (protectedUrlsMatcher.matches(httpServletRequest)) {
            logger.info("User Role Protected")
            val header = httpServletRequest.getHeader("Authorization")
            val hasToken = hasJwtToken(header, response)

            val loginToken: LoginToken? = readLoginTokenFromJwtToken(hasToken, header)

            if (hasToken) {
                placeSecurityContext(loginToken, response)
            }

        }

        chain!!.doFilter(request, response)
    }

    private fun placeSecurityContext(loginToken: LoginToken?, response: ServletResponse?) {
        if (loginToken != null) {
            SecurityContextHolder.getContext().authentication = JwtAuthentication(loginToken)
        } else {
            (response as HttpServletResponse).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid Token")
        }
    }

    private fun readLoginTokenFromJwtToken(hasToken: Boolean, header: String?): LoginToken? {
        return if (hasToken) {
            val token = header?.substring(7)
            val claims = JwtUtils.readClaims(token, secret + secret)

            if (claims != null) {
                if (claims["subject"] != null) {
                    LoginUtils.createLoginFromMap(claims)
                } else null
            } else null
        } else null
    }

    private fun hasJwtToken(header: String?, response: ServletResponse?): Boolean {
        return if (header == null || !header.startsWith("Bearer ")) {
            (response as HttpServletResponse).sendError(HttpServletResponse.SC_FORBIDDEN, "No JWT token found in request headers")
            logger.info("Token not found in header")
            false
        } else {
            true
        }
    }
}
