package misc.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class ChcResult {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration elapsed;
    private String logs;
}
