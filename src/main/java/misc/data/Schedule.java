package misc.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

@Data
public class Schedule {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Duration everyDay;
    private Week week;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Map<LocalDate, Duration> calendar;
}
