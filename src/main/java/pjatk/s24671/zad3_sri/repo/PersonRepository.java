package pjatk.s24671.zad3_sri.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pjatk.s24671.zad3_sri.model.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {
}
