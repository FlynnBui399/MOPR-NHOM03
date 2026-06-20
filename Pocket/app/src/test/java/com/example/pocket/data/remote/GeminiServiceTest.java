package com.example.pocket.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class GeminiServiceTest {
    @Test
    public void parseCaptions_readsJsonObject() {
        String json = "{\n" +
                "  \"visualDetails\": [\"mèo\", \"phòng khách\"],\n" +
                "  \"captions\": [\n" +
                "    \"Em mèo nằm ngủ ngoan\",\n" +
                "    \"Góc phòng khách chill ghê\",\n" +
                "    \"Ăn no rồi ngủ thôi\",\n" +
                "    \"Thời tiết hôm nay đẹp cực\"\n" +
                "  ]\n" +
                "}";
        assertEquals(Arrays.asList(
                "Em mèo nằm ngủ ngoan",
                "Góc phòng khách chill ghê",
                "Ăn no rồi ngủ thôi",
                "Thời tiết hôm nay đẹp cực"
        ), GeminiService.parseCaptions(json));
    }

    @Test
    public void parseCaptions_readsJsonArray() {
        assertEquals(Arrays.asList("Bữa nay chill ghê", "Nắng đẹp quá nè", "Đi trốn một hôm", "Thêm câu nữa nè"),
                GeminiService.parseCaptions(
                        "[\"Bữa nay chill ghê\",\"Nắng đẹp quá nè\",\"Đi trốn một hôm\",\"Thêm câu nữa nè\"]"));
    }

    @Test
    public void parseCaptions_removesMarkdownFence() {
        assertEquals(Arrays.asList("Một chiều thật xanh", "Gió kể chuyện hôm nay", "Bình yên ở đây", "Thêm câu nữa nha"),
                GeminiService.parseCaptions("```json\n"
                        + "[\"Một chiều thật xanh\", \"Gió kể chuyện hôm nay\", "
                        + "\"Bình yên ở đây\", \"Thêm câu nữa nha\"]\n```"));
    }

    @Test
    public void parseCaptions_acceptsNumberedLinesAndLimitsToFour() {
        assertEquals(Arrays.asList("Món này cuốn nha", "Ăn trước tính sau", "No bụng vui lòng", "Tuyệt cú mèo luôn"),
                GeminiService.parseCaptions("1. Món này cuốn nha\n"
                        + "2) Ăn trước tính sau\n"
                        + "3 - No bụng vui lòng\n"
                        + "4. Tuyệt cú mèo luôn\n"
                        + "5. Dòng này dư rồi"));
    }

    @Test
    public void parseCaptions_preservesThreeSpecificAndOneGeneric() {
        List<String> parsed = GeminiService.parseCaptions(
                "{\"imageSpecific\":[\"G\",\"Chó 3\",\"Mèo ngồi ngắm\","
                        + "\"ly cà phê trên bàn\",\"góc phòng đầy nắng\","
                        + "\"chiếc ghế trông thật êm\"],"
                        + "\"generic\":\"một chút hôm nay\"}");

        assertEquals(4, parsed.size());
        assertEquals("Ly cà phê trên bàn", parsed.get(0));
        assertEquals("Góc phòng đầy nắng", parsed.get(1));
        assertEquals("Chiếc ghế trông thật êm", parsed.get(2));
        assertEquals("Một chút hôm nay", parsed.get(3));
    }
}
