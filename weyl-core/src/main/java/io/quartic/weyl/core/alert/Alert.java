package io.quartic.weyl.core.alert;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.quartic.common.SweetStyle;
import org.immutables.value.Value;

import java.util.Optional;

import static io.quartic.weyl.core.alert.Alert.Level.INFO;

@SweetStyle
@Value.Immutable
@JsonSerialize(as = AlertImpl.class)
@JsonDeserialize(as = AlertImpl.class)
public interface Alert {
    enum Level {
        INFO,
        WARNING,
        SEVERE
    }

    String title();
    Optional<String> body();
    @Value.Default
    default Level level() {
        return INFO;
    }
}
