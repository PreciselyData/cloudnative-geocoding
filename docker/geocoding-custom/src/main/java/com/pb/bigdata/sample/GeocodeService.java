package com.pb.bigdata.sample;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import com.precisely.addressing.v1.Addressing;
import com.precisely.addressing.v1.AddressingException;
import com.precisely.addressing.v1.model.RequestAddress;
import com.precisely.addressing.v1.model.PredictionResult;
import com.precisely.addressing.v1.model.PredictionResponse;
import com.precisely.addressing.v1.model.PreferencesBuilder;
import com.precisely.addressing.AddressingBuilder;
import com.precisely.addressing.configuration.AddressingConfiguration;
import com.precisely.addressing.configuration.AddressingConfigurationBuilder;
import com.precisely.addressing.configuration.datasources.SpdDataSourceBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

@RestController
@RequestMapping("/addressing")
public final class GeocodeService {
	private static final Logger LOG = LoggerFactory.getLogger(GeocodeService.class);

	@Value("${ggs.resources.location:/var/lib/ggs/resources}")
	private String m_geocodingResourcesDir;

	@Value("${ggs.data.location:/mnt/ggs_data}")
	private String m_geocodingDataPath;

	@Value("${ggs.remote:false}")
	private String m_ggsRemote;

	@Value("${ggs.usa.pool.max.active:0}")
	private int m_usaPoolMaxActive;

	private Addressing m_addressing;

	@RequestMapping(value = "/predict/{country}", method = RequestMethod.GET, produces = {"application/json"})
	public List<PredictionResult> suggest(@PathVariable String country, @RequestParam String input) {
		RequestAddress address = new RequestAddress();
		address.setAddressLines(Arrays.asList(input));
		address.setCountry(country);

		try {
			PredictionResponse response = m_addressing.predict(address, new PreferencesBuilder().withMaxResults(5).build());
			return response.getPredictions();
		} catch (AddressingException e) {
			throw new RuntimeException(e);
		}
	}

	@PostConstruct
	private void initializeInteractiveGeocoder() throws Exception {
		AddressingConfiguration addressingConfiguration = new AddressingConfigurationBuilder()
				.withResources(Paths.get(m_geocodingResourcesDir))
				.withData(new SpdDataSourceBuilder().withSpdPaths(Paths.get(m_geocodingDataPath)).build())
				.build();

		Map<String, String> usaConfig = addressingConfiguration.getConfiguration("USA");
		if(usaConfig != null){
			Map<String, String> usaSettings =  usaConfig;
			usaSettings.put("REMOTE", m_ggsRemote);
			if(m_usaPoolMaxActive == 0){
				m_usaPoolMaxActive = (int) Math.ceil(Runtime.getRuntime().availableProcessors() * 1.5);
			}
			usaSettings.put("POOL_MAX_ACTIVE", String.valueOf(m_usaPoolMaxActive));
		}

		m_addressing = new AddressingBuilder()
				.withConfiguration(addressingConfiguration)
				.build();

		LOG.info(String.format("Interactive geocoder initialized with settings [REMOTE: %s, POOL_MAX_ACTIVE: %s]",
				m_ggsRemote, m_usaPoolMaxActive));
	}

	@JsonComponent
	private static class PredictionResultJsonSerializer extends JsonSerializer<PredictionResult> {

		@Override
		public void serialize(PredictionResult value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			//output a limited set of data from the PredictionResult
			gen.writeStartObject();
			gen.writeStringField("prediction", value.getPrediction());
			gen.writeStringField("formattedStreetAddress", value.getAddress().getFormattedStreetAddress());
			gen.writeStringField("formattedLocationAddress", value.getAddress().getFormattedLocationAddress());
			gen.writeEndObject();
		}
	}
}
