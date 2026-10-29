package fu.de190366.postservice.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Jackson parses/formats dd/MM/yyyy in UTC by default, while Hibernate reads DATE
 * columns at JVM-local midnight -> dates read back from the DB were shown one day
 * earlier. Use the JVM time zone for Jackson so both sides agree.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer defaultTimeZoneCustomizer() {
        return builder -> builder.defaultTimeZone(TimeZone.getDefault());
    }
}
