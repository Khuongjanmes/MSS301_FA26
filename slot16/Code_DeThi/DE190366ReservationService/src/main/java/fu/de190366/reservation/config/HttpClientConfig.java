package fu.de190366.reservation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign variant: RoomClient duoc OpenFeign tu tao (xem @EnableFeignClients),
 * nen KHONG tao proxy RestClient o day nua.
 * Chi giu lai ObjectMapper de service dung convertValue(data -> RoomDTO).
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
