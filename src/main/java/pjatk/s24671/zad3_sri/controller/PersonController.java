package pjatk.s24671.zad3_sri.controller;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pjatk.s24671.zad3_sri.dto.CarDto;
import pjatk.s24671.zad3_sri.dto.PersonDto;
import pjatk.s24671.zad3_sri.model.Car;
import pjatk.s24671.zad3_sri.model.Person;
import pjatk.s24671.zad3_sri.repo.CarRepository;
import pjatk.s24671.zad3_sri.repo.PersonRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/persons")
public class PersonController {

    private final PersonRepository personRepository;
    private final CarRepository carRepository;
    private final ModelMapper modelMapper;

    public PersonController(PersonRepository personRepository, CarRepository carRepository, ModelMapper modelMapper) {
        this.personRepository = personRepository;
        this.carRepository = carRepository;
        this.modelMapper = modelMapper;
    }

    private PersonDto convertToDto(Person p) {
        PersonDto dto = modelMapper.map(p, PersonDto.class);
        dto.add(linkTo(methodOn(PersonController.class).getPersonById(p.getId())).withSelfRel());
        dto.add(linkTo(methodOn(PersonController.class).getPersonCars(p.getId())).withRel("cars"));
        return dto;
    }

    private CarDto convertCarToDto(Car c) {
        CarDto dto = modelMapper.map(c, CarDto.class);
        dto.add(linkTo(methodOn(CarController.class).getCarById(c.getId())).withSelfRel());
        if (c.getPerson() != null) {
            dto.add(linkTo(methodOn(PersonController.class).getPersonById(c.getPerson().getId())).withRel("person"));
        }
        return dto;
    }

    private Person convertToEntity(PersonDto dto) {
        return modelMapper.map(dto, Person.class);
    }

    @GetMapping
    public ResponseEntity<CollectionModel<PersonDto>> getPersons() {
        List<Person> allPersons = personRepository.findAll();
        List<PersonDto> result = allPersons.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        Link selfLink = linkTo(methodOn(PersonController.class).getPersons()).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(result, selfLink), HttpStatus.OK);
    }

    @GetMapping("/{personId}")
    public ResponseEntity<PersonDto> getPersonById(@PathVariable Long personId) {
        Optional<Person> person = personRepository.findById(personId);
        if (person.isPresent()) {
            return new ResponseEntity<>(convertToDto(person.get()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity createPerson(@Valid @RequestBody PersonDto personDto) {
        Person newPerson = convertToEntity(personDto);
        personRepository.save(newPerson);

        HttpHeaders headers = new HttpHeaders();
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{personId}")
                .buildAndExpand(newPerson.getId())
                .toUri();
        headers.add("Location", location.toString());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{personId}")
    public ResponseEntity updatePerson(@PathVariable Long personId, @Valid @RequestBody PersonDto personDto) {
        Optional<Person> person = personRepository.findById(personId);
        if (person.isPresent()) {
            personDto.setId(personId);
            Person entity = convertToEntity(personDto);
            personRepository.save(entity);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{personId}")
    public ResponseEntity deletePerson(@PathVariable Long personId) {
        if (personRepository.existsById(personId)) {
            personRepository.deleteById(personId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @GetMapping("/{personId}/details")
    public ResponseEntity<?> getPersonDetails(@PathVariable Long personId) {
        Optional<Person> personOpt = personRepository.findById(personId);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            PersonDto personDto = convertToDto(person);
            List<CarDto> cars = person.getCars().stream().map(this::convertCarToDto).collect(Collectors.toList());
            
            Map<String, Object> result = new HashMap<>();
            result.put("person", personDto);
            result.put("cars", cars);
            return ResponseEntity.ok(result);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/{personId}/cars")
    public ResponseEntity<CollectionModel<CarDto>> getPersonCars(@PathVariable Long personId) {
        Optional<Person> personOpt = personRepository.findById(personId);
        if (personOpt.isPresent()) {
            List<CarDto> cars = personOpt.get().getCars().stream()
                    .map(this::convertCarToDto)
                    .collect(Collectors.toList());
            Link selfLink = linkTo(methodOn(PersonController.class).getPersonCars(personId)).withSelfRel();
            return new ResponseEntity<>(CollectionModel.of(cars, selfLink), HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping("/{personId}/cars/{carId}")
    public ResponseEntity<?> addCarToPerson(@PathVariable Long personId, @PathVariable Long carId) {
        Optional<Person> personOpt = personRepository.findById(personId);
        Optional<Car> carOpt = carRepository.findById(carId);

        if (personOpt.isPresent() && carOpt.isPresent()) {
            Person person = personOpt.get();
            Car car = carOpt.get();
            
            car.setPerson(person);
            carRepository.save(car);
            
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{personId}/cars/{carId}")
    public ResponseEntity<?> removeCarFromPerson(@PathVariable Long personId, @PathVariable Long carId) {
        Optional<Person> personOpt = personRepository.findById(personId);
        Optional<Car> carOpt = carRepository.findById(carId);

        if (personOpt.isPresent() && carOpt.isPresent()) {
            Person person = personOpt.get();
            Car car = carOpt.get();
            
            if (car.getPerson() != null && car.getPerson().getId().equals(person.getId())) {
                car.setPerson(null);
                carRepository.save(car);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
