package misc.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChcResult {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private LocalDateTime date;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration elapsed;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration limit;
}
