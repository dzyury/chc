package misc.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ChcResult {
    private LocalDate date;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration elapsed;
    private String logs;
}
