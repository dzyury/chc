package misc.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class Main {
    public static void main(String[] args) throws JsonProcessingException {
        String json = """
        {
          "everyDay": "pt60m",
          "week": {
            "sunday": "pt100m"
          },
          "calendar": {
            "2024-01-07": "pt80m"
          }
        }
        """;
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Schedule schedule = mapper.readValue(json, Schedule.class);
        Logger.log(schedule);
        Logger.log(mapper.writeValueAsString(schedule));
    }
}
