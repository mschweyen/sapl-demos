package io.sapl.demo.axon.iface.rest;

import java.util.List;

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway;
import org.axonframework.extensions.reactor.queryhandling.gateway.ReactorQueryGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.sapl.demo.axon.command.PatientCommandAPI.DischargePatient;
import io.sapl.demo.axon.command.PatientCommandAPI.HospitalisePatient;
import io.sapl.demo.axon.query.patients.api.PatientDocument;
import io.sapl.demo.axon.query.patients.api.PatientQueryAPI.FetchAllPatients;
import io.sapl.demo.axon.query.patients.api.PatientQueryAPI.FetchPatient;
import io.sapl.demo.axon.query.patients.api.PatientQueryAPI.MonitorPatient;
import io.sapl.demo.axon.command.Ward;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class PatientsController {

	private final ReactorQueryGateway   queryGateway;
	private final ReactorCommandGateway commandGateway;

	@GetMapping("/api/patients")
	Mono<List<PatientDocument>> fetchAllPatients() {
		return queryGateway.query(new FetchAllPatients(), ResponseTypes.multipleInstancesOf(PatientDocument.class));
	}

	@GetMapping("/api/patients/{id}")
	public Mono<ResponseEntity<PatientDocument>> fetchPatient(@PathVariable String id) {
		return queryGateway.query(new FetchPatient(id), ResponseTypes.instanceOf(PatientDocument.class))
				.map(ResponseEntity::ok).defaultIfEmpty(ResponseEntity.notFound().build());
	}

	@GetMapping(value = "/api/patients/{id}/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
	Flux<ServerSentEvent<PatientDocument>> streamPatient(@PathVariable String id) {
		return queryGateway
				.subscriptionQuery(new MonitorPatient(id), ResponseTypes.instanceOf(PatientDocument.class),
						ResponseTypes.instanceOf(PatientDocument.class))
				.flatMapMany(result -> Flux.concat(result.initialResult(), result.updates()))
				.map(view -> ServerSentEvent.<PatientDocument>builder().data(view).build());
	}

	@CrossOrigin
	@PostMapping("/api/patients/{id}/hospitalise/{ward}")
	Mono<Object> hospitalizePatient(@PathVariable String id, @PathVariable Ward ward) {
		return commandGateway.send(new HospitalisePatient(id, ward));
	}

	@CrossOrigin
	@PostMapping("/api/patients/{id}/discharge")
	Mono<Object> hospitalizePatient(@PathVariable String id) {
		return commandGateway.send(new DischargePatient(id));
	}

}
