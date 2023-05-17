package com.example.service;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
public class ServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServiceApplication.class, args);
	}

	@Bean
	ApplicationListener<ApplicationReadyEvent> readyEventApplicationListener(CustomerRepository customerRepository) {
		return event -> customerRepository.findAll().forEach(System.out::println);
	}
}

@RestController
class CustomerController {

	private final CustomerRepository customerRepository;
	private final ObservationRegistry observationRegistry;

	CustomerController(CustomerRepository customerRepository, ObservationRegistry observationRegistry) {
		this.customerRepository = customerRepository;
		this.observationRegistry = observationRegistry;
	}


	@GetMapping("/customers")
	Iterable<Customer> all() {
		return this.customerRepository.findAll();
	}
	
	@GetMapping("/customers/{name}")
	Iterable<Customer> byName(@PathVariable String name) {
		Assert.state( Character.isUpperCase(name.charAt(0)), "must be a uppercase.");

		return Observation.createNotStarted("by-name", this.observationRegistry)
				.observe( () ->
				this.customerRepository.findByName(name)
				);
	}

	@PostMapping("/customer")
	Customer save(@RequestBody Customer customer) {
		return customerRepository.save(customer);
	}
}
interface CustomerRepository extends CrudRepository<Customer, Integer> {

	Iterable<Customer> findByName(String name);
}

record Customer (
		@Id Integer id,
		String name) {
}

@ControllerAdvice
class ErrorHandlerControllerAdvice {

	@ExceptionHandler
	public ProblemDetail handle(IllegalStateException illegalStateException, HttpServletRequest httpServletRequest) {
		httpServletRequest.getHeaderNames().asIterator().forEachRemaining(System.out::println);

		var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST.value());
		pd.setDetail(illegalStateException.getMessage());
		return pd;
	}
}