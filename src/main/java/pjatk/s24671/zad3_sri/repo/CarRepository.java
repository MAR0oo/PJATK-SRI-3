package pjatk.s24671.zad3_sri.repo;

import org.springframework.data.repository.CrudRepository;
import pjatk.s24671.zad3_sri.model.Car;

import java.util.List;

public interface CarRepository extends CrudRepository<Car, Long> {
    List<Car> findAll();
}