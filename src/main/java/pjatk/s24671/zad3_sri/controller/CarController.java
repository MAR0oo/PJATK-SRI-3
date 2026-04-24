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
import pjatk.s24671.zad3_sri.model.Car;
import pjatk.s24671.zad3_sri.repo.CarRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    private final CarRepository carRepository;
    private final ModelMapper modelMapper;

    public CarController(CarRepository carRepository, ModelMapper modelMapper) {
        this.carRepository = carRepository;
        this.modelMapper = modelMapper;
    }

    private CarDto convertToDto(Car c) {
        CarDto dto = modelMapper.map(c, CarDto.class);
        dto.add(linkTo(methodOn(CarController.class).getCarById(c.getId())).withSelfRel());
        if (c.getPerson() != null) {
            dto.add(linkTo(methodOn(PersonController.class).getPersonById(c.getPerson().getId())).withRel("person"));
        }
        return dto;
    }

    private Car convertToEntity(CarDto dto) {
        return modelMapper.map(dto, Car.class);
    }

    @GetMapping
    public ResponseEntity<CollectionModel<CarDto>> getCars() {
        List<Car> allCars = carRepository.findAll();
        List<CarDto> result = allCars.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        Link selfLink = linkTo(methodOn(CarController.class).getCars()).withSelfRel();
        return new ResponseEntity<>(CollectionModel.of(result, selfLink), HttpStatus.OK);
    }

    @GetMapping("/{carId}")
    public ResponseEntity<CarDto> getCarById(@PathVariable Long carId) {
        Optional<Car> car = carRepository.findById(carId);
        if(car.isPresent()) {
            CarDto carDto = convertToDto(car.get());
            return new ResponseEntity<>(carDto, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping()
    public ResponseEntity createCar(@Valid @RequestBody CarDto carDto) {
        Car newCar = convertToEntity(carDto);
        carRepository.save(newCar);

        HttpHeaders headers = new HttpHeaders();
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{carId}")
                .buildAndExpand(newCar.getId())
                .toUri();
        headers.add("Location", location.toString());
        return new ResponseEntity<>(headers, HttpStatus.CREATED);
    }

    @PutMapping("/{carId}")
    public ResponseEntity updateCar(@PathVariable Long carId, @Valid @RequestBody CarDto carDto) {
        Optional<Car> car = carRepository.findById(carId);

        if(car.isPresent()){
            carDto.setId(carId);
            Car entity = convertToEntity(carDto);
            
            // Zachowajmy powiązanie z osobą przy aktualizacji, aby go nie stracić
            entity.setPerson(car.get().getPerson());
            
            carRepository.save(entity);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{carId}")
    public ResponseEntity deleteCar(@PathVariable Long carId) {
        boolean isPresent = carRepository.existsById(carId);

        if(isPresent){
            carRepository.deleteById(carId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}