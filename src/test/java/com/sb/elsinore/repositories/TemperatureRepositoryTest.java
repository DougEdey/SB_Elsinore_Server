package com.sb.elsinore.repositories;

import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.Temperature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@DataJpaTest
public class TemperatureRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TemperatureRepository temperatureRepository;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void canCreateTemperature() {
        // given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        this.entityManager.persist(temperature);
        this.entityManager.flush();

        // when
        Temperature found = this.temperatureRepository.findByName("Test");

        //then
        assertEquals(temperature.getName(), found.getName());
    }

    @Test
    public void canDeleteTemperature() {
        // Given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        this.entityManager.persist(temperature);
        this.entityManager.flush();

        // when
        this.temperatureRepository.delete(temperature);

        // then
        assertNull(this.temperatureRepository.findByName(temperature.getName()));
    }

    @Test
    public void canSaveTemperatureViaRepository() {
        // Given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        // when
        this.temperatureRepository.save(temperature);

        // then
        assertEquals(temperature, this.temperatureRepository.findByName(temperature.getName()));
    }

    @Test
    public void canUpdateTemperatureName() {
        // Given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        // when
        this.temperatureRepository.save(temperature);

        // then
        assertNotNull(this.temperatureRepository.findByName("Test"));
        assertNull(this.temperatureRepository.findByName("Test Updated"));

        // given
        temperature.setName("Test Updated");

        // when
        this.temperatureRepository.save(temperature);

        // then
        assertNull(this.temperatureRepository.findByName("Test"));
        assertNotNull(this.temperatureRepository.findByName("Test Updated"));
    }

    @Test
    public void temperatureNameMustBeUnique() {
        // Given
        Temperature temperature = new Temperature();
        temperature.setName("Test");
        temperature.setDevice("Device");

        Temperature copyTemperature = new Temperature(temperature);

        // when
        this.temperatureRepository.saveAndFlush(temperature);

        // then
        this.thrown.expect(DataIntegrityViolationException.class);
        this.temperatureRepository.saveAndFlush(copyTemperature);


    }

}
