package io.sapl.demo.axon.query.vitals.api;

import lombok.experimental.UtilityClass;

@UtilityClass
public class VitalSignsQueryAPI {

	// @formatter:off
	public record FetchVitalSignsOfPatient 		(String patientId) {};
	public record MonitorAllVitalSignsOfPatient (String patientId) {};
	public record FetchVitalSignOfPatient 	    (String patientId, String type) {};
	public record MonitorVitalSignOfPatient 	(String patientId, String type) {};
	// @formatter:on

}
