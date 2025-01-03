package misc.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import lombok.Data;

import java.time.Duration;

@Data
public class Week {
    @JsonFormat(shape = Shape.STRING)
    private Duration monday;
    @JsonFormat(shape = Shape.STRING)
    private Duration tuesday;
    @JsonFormat(shape = Shape.STRING)
    private Duration wednesday;
    @JsonFormat(shape = Shape.STRING)
    private Duration thursday;
    @JsonFormat(shape = Shape.STRING)
    private Duration friday;
    @JsonFormat(shape = Shape.STRING)
    private Duration saturday;
    @JsonFormat(shape = Shape.STRING)
    private Duration sunday;
}
