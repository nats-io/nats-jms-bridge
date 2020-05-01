package io.nats.bridge.admin.util


import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.security.Key
import javax.crypto.spec.SecretKeySpec

object JwtUtils {

    private val algorithmMap = SignatureAlgorithm.values().toList().map { it.name to it }.toMap()
    private val algorithmJCAMap = SignatureAlgorithm.values().toList().map { it.jcaName to it }.toMap()


    fun generateToken(subject: String,
                      claims: Map<String, String>,
                      secret: String, jwtAlgorithm: String = "HmacSHA512"): String {


        val algorithm = algorithmMap.getOrDefault(jwtAlgorithm, algorithmJCAMap[jwtAlgorithm]!!)

        val keySpec = SecretKeySpec(secret.toByteArray(), jwtAlgorithm)
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .signWith(keySpec, algorithm)
                .compact()
    }

    fun readClaims(token: String?, secret: String): Map<String, String>? {
        val key: Key = Keys.hmacShaKeyFor(secret.toByteArray())
        return if (token != null) {
            try {

                val jwtReader = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()

                val claims = jwtReader.parseClaimsJws(token).body

                val map = mutableMapOf<String, String>()
                claims.forEach {
                    map[it.key] = it.value.toString()
                }
                map
            } catch (t: Throwable) {
                null
            }
        } else null
    }
}
