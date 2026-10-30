package fu.de190366.reservation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients // scan @FeignClient interfaces (OpenFeign)
public class DE190366ReservationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DE190366ReservationServiceApplication.class, args);
    }
}
