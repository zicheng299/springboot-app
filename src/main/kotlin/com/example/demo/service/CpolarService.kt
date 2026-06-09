package com.example.demo.service

import com.example.demo.domain.LoginResponse
import com.example.demo.domain.Tunnels
import com.example.demo.domain.TunnelsResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class CpolarService {

    private val restTemplate = RestTemplate()

    private var token: String? = null

    private val loginUrl = "http://127.0.0.1:9200/api/v1/user/login"
    private val tunnelsUrl = "http://127.0.0.1:9200/api/v1/tunnels"

    @Synchronized
    private fun getToken(): String {
        token?.let { return it }

        val email = "zicheng299@gmail.com"
        val password = "670664ll"

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }
        val body = mapOf("email" to email, "password" to password)
        val request = HttpEntity(body, headers)

        val response = restTemplate.postForObject(loginUrl, request, LoginResponse::class.java)
        token = response?.data?.token ?: throw IllegalStateException("登录失败，未获取到 token")
        return token!!
    }

    private fun getTunnels(): List<Tunnels> {
        val authToken = "Bearer ${getToken()}"
        val headers = HttpHeaders().apply {
            set("Authorization", authToken)
        }
        val request = HttpEntity<Any>(headers)

        val response = restTemplate.exchange(
            tunnelsUrl,
            HttpMethod.GET,
            request,
            TunnelsResponse::class.java
        )
        return response.body?.data?.items ?: emptyList()
    }

    fun getUrls(): Map<String, String> {
        val tunnels: List<Tunnels> = try {
            getTunnels()
        } catch (e: Exception) {
            token = null
            println(e.message)
            emptyList()
        }

        return tunnels.flatMap(Tunnels::publish_tunnels)
            .associate { "${it.name}-${it.proto}" to it.public_url }

    }

    fun getCpolarHost(): String {
        val tunnels = getTunnels()
        val first = tunnels.flatMap(Tunnels::publish_tunnels).first {
            val condition1 = it.name == "website"
            val condition2 = it.proto == "http"
            condition1 && condition2
        }
        return first.public_url
    }

    fun getRemoteDesktopHost(): String {
        val tunnels = getTunnels()
        val first = tunnels.flatMap(Tunnels::publish_tunnels).first {
            val condition1 = it.name == "remoteDesktop"
            val condition2 = it.proto == "tcp"
            condition1 && condition2
        }
        return first.public_url.replace("tcp://", "")
    }
}
