package misc.data;

import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;

@Data
public class Elapsed {
    private LocalDate date;
    private Duration duration;
}
