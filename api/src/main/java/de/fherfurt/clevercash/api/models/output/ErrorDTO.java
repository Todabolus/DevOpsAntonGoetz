package de.fherfurt.clevercash.api.models.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDTO {
    private String reason;
    private LocalDateTime timeStamp;

    public ErrorDTO(String reason) {
        this.reason = reason;
        this.timeStamp = LocalDateTime.now();
    }
}


