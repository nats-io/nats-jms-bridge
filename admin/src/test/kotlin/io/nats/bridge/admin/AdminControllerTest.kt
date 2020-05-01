package io.nats.bridge.admin

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class AdminControllerTest(@Autowired wac: WebApplicationContext
//        @Autowired val reportRepository: UserHealthReportRepository,
) {

    private val mapper = jacksonObjectMapper()
    private val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(wac).build()

    @BeforeEach
    fun clearData() {

    }


    @Test
    fun ping() {
        //val valuesJson = AdminController::class.java.getResource("/LookupValues.json").readText()

        val result = mockMvc.perform(
                MockMvcRequestBuilders.get("/api/v1/util/ping")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.parseMediaType("application/json;charset=UTF-8")))
        result.andExpect(status().isOk)
                .andExpect(content().contentType("application/json;charset=UTF-8"))


    }

//
//    @Test
//    fun getLookupValues() {
//        //val valuesJson = AdminController::class.java.getResource("/LookupValues.json").readText()
//
//        val result = mockMvc.perform(
//                MockMvcRequestBuilders.get("/api/v1/lookupvalues")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.parseMediaType("application/json;charset=UTF-8")))
//        result.andExpect(status().isOk)
//                .andExpect(content().contentType("application/json;charset=UTF-8"))
//                .andExpect(jsonPath("$.lookupValues.components[0]").value("Air Force"))
//
//    }


}

