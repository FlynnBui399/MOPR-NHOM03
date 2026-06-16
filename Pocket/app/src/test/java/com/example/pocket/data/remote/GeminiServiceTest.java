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
    public void parseCaptions_appliesGenericCaptionFilter_keepsUpToTwo() {
        // Here we pass 3 generic captions and 2 non-generic captions:
        // Generic: "một chút hôm nay", "vừa chụp nè", "hôm nay thật vui"
        // Non-generic: "Món này ngon ghê", "Ăn thôi cả nhà"
        // The filter should keep at most 2 generic captions, so it will keep the first two,
        // and keep both non-generic ones, yielding 4 captions in total.
        List<String> parsed = GeminiService.parseCaptions(
                "[\"một chút hôm nay\",\"Món này ngon ghê\",\"vừa chụp nè\",\"hôm nay thật vui\",\"Ăn thôi cả nhà\"]");
        
        // Assert size is 4
        assertEquals(4, parsed.size());
        
        // Verify it kept the first two generic ones and the non-generic ones
        assertTrue(parsed.contains("Món này ngon ghê"));
        assertTrue(parsed.contains("Ăn thôi cả nhà"));
        assertTrue(parsed.contains("một chút hôm nay"));
        assertTrue(parsed.contains("vừa chụp nè"));
    }
}
