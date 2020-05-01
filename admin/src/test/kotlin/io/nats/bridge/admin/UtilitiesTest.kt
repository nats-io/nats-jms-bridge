package io.nats.bridge.admin

//import org.junit.jupiter.api.Assertions.assertEquals
//import org.mockito.Mockito
//import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


@SpringBootTest
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
class UtilitiesTest {
//    @Test
//    fun putRecord() {
//        val mockedRecordResult = PutRecordsResult()
//        mockedRecordResult.failedRecordCount = 0
//        Mockito.`when`(kinesisService.putRecord(MockitoHelper.anyObject(), MockitoHelper.anyObject())).thenReturn(mockedRecordResult)
//        val result = kinesisService.putRecord(checkIn1(), "1")
//        assertEquals(0, result?.failedRecordCount)
//        //assertNotNull(result?.records)
//    }

}