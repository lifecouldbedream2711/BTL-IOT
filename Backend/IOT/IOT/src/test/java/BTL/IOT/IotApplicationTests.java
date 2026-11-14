package BTL.IOT;

import BTL.IOT.Dto.response.PageResult;
import BTL.IOT.Dto.response.SensorDataRecordDto;
import BTL.IOT.Service.DataSensorService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IotApplicationTests {

    @Autowired
    private DataSensorService dataSensorService;

    @Test
    void contextLoads() {
    }

    @Test
    void testExactSecondSearch() {
        // Query exact second "2026-11-04 15:18:15"
        PageResult<SensorDataRecordDto> result = dataSensorService.searchDataSensors("All", null, "2026-11-04 15:18:15", 1, 10);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getItems().size() > 0, "Should find records for 2026-11-04 15:18:15");
        for (SensorDataRecordDto item : result.getItems()) {
            Assertions.assertTrue(item.getTime().startsWith("2026-11-04 15:18:15"), "Formatted time should match second: " + item.getTime());
        }

        // Query with ISO format "2026-11-04T15:18:15"
        PageResult<SensorDataRecordDto> resultIso = dataSensorService.searchDataSensors("All", null, "2026-11-04T15:18:15", 1, 10);
        Assertions.assertEquals(result.getTotal(), resultIso.getTotal(), "Search with ISO string should yield the exact same results");

        // Query with sensorType filter
        PageResult<SensorDataRecordDto> resultTemp = dataSensorService.searchDataSensors("Temperature", null, "2026-11-04 15:18:15", 1, 10);
        Assertions.assertTrue(resultTemp.getTotal() >= 1, "Should find Temperature record");
        Assertions.assertEquals("Temperature", resultTemp.getItems().get(0).getType());

        // Query default pagination (all records)
        PageResult<SensorDataRecordDto> all = dataSensorService.searchDataSensors("All", null, 1, 10);
        Assertions.assertTrue(all.getTotal() > 0, "Total count should be greater than 0");
    }
}
