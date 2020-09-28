package com.pb.bigdata.sample;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.mapinfo.geocode.GeocodeAddress;
import com.mapinfo.geocode.GeocodingException;
import com.mapinfo.geocode.api.Address;
import com.mapinfo.geocode.api.Candidate;
import com.mapinfo.geocode.api.InteractiveGeocodingAPI;
import com.mapinfo.geocode.api.Response;
import com.pb.geocoding.config.ConfigCountry;
import com.pb.geocoding.config.ConfigurationBuilder;
import com.pb.geocoding.config.api.GeocodingConfiguration;
import com.pb.geocoding.interactive.InteractiveBuilder;
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

@RestController
@RequestMapping("/geocoding")
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

	private InteractiveGeocodingAPI m_interactiveGeocoder;

	@RequestMapping(value = "/suggest/{country}", method = RequestMethod.GET, produces = {"application/json"})
	public List<Candidate> suggest(@PathVariable String country, @RequestParam String input) {
		Address address = new GeocodeAddress();
		address.setMainAddressLine(input);
		address.setCountry(country);

		try {
			Response response = m_interactiveGeocoder.suggest(address, null);
			return response.getCandidates();
		} catch (GeocodingException e) {
			throw new RuntimeException(e);
		}
	}

	@PostConstruct
	private void initializeInteractiveGeocoder() throws Exception {
		GeocodingConfiguration geocodingConfiguration = new ConfigurationBuilder()
				.withResourcesPath(Paths.get(m_geocodingResourcesDir))
				.withDataPath(Paths.get(m_geocodingDataPath))
				.build();

		ConfigCountry usaConfig = geocodingConfiguration.getConfigByCountry("USA");
		if(usaConfig != null){
			Map<String, String> usaSettings =  usaConfig.getProperties();
			usaSettings.put("REMOTE", m_ggsRemote);
			if(m_usaPoolMaxActive == 0){
				m_usaPoolMaxActive = (int) Math.ceil(Runtime.getRuntime().availableProcessors() * 1.5);
			}
			usaSettings.put("POOL_MAX_ACTIVE", String.valueOf(m_usaPoolMaxActive));
		}

		m_interactiveGeocoder = new InteractiveBuilder()
		.withConfiguration(geocodingConfiguration)
		.build();

		LOG.info(String.format("Interactive geocoder initialized with settings [REMOTE: %s, POOL_MAX_ACTIVE: %s]",
				m_ggsRemote, m_usaPoolMaxActive));
	}

	@JsonComponent
	private static class CandidateJsonSerializer extends JsonSerializer<Candidate> {

		@Override
		public void serialize(Candidate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			//output a limited set of data from the candidate
			gen.writeStartObject();
			gen.writeStringField("formattedStreetAddress", value.getFormattedStreetAddress());
			gen.writeStringField("formattedLocationAddress", value.getFormattedLocationAddress());
			gen.writeEndObject();
		}
	}
}
