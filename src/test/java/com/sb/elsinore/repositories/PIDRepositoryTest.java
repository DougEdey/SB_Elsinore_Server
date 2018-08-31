package com.sb.elsinore.repositories;

import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.PIDModel;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@DataJpaTest
public class PIDRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TemperatureRepository temperatureRepository;
    @Autowired
    private PIDRepository pidRepository;

    private Temperature temperature;

    @Before
    public void setup() {
        initMocks(this);
        this.temperature = new Temperature();
        this.temperature.setName("Test");
        this.temperature.setDevice("Device");

        this.entityManager.persistAndFlush(this.temperature);
    }

    @Test
    public void canCreatePIDAndGetByTemperatureName() {
        // given
        PIDModel pidModel = new PIDModel();
        pidModel.setTemperature(this.temperature);

        this.entityManager.persistAndFlush(pidModel);

        // when
        PIDModel found = this.pidRepository.findByTemperatureName("Test");

        //then
        assertEquals(this.temperature.getName(), found.getTemperature().getName());
    }

    @Test
    public void canCreatePIDAndFindById() {
        // given
        PIDModel pidModel = new PIDModel();
        pidModel.setTemperature(this.temperature);

        Long pidId = this.entityManager.persistAndFlush(pidModel).getId();

        // when
        PIDModel found = this.pidRepository.getOne(pidId);

        //then
        assertEquals(pidModel, found);
    }

    @Test
    public void canCreatePIDViaRepositoryAndGetByTemperatureName() {
        // given
        PIDModel pidModel = new PIDModel();
        pidModel.setTemperature(this.temperature);

        this.pidRepository.saveAndFlush(pidModel);

        // when
        PIDModel found = this.pidRepository.findByTemperatureName("Test");

        //then
        assertEquals(this.temperature.getName(), found.getTemperature().getName());
    }

    @Test
    public void canOnlyCreateOnePIDPerTemperatureProbe() {
        // given
        PIDModel pidModel = new PIDModel();
        pidModel.setTemperature(this.temperature);

        this.pidRepository.saveAndFlush(pidModel);

        pidModel = new PIDModel(this.temperature);

        this.thrown.expect(DataIntegrityViolationException.class);

        this.pidRepository.saveAndFlush(pidModel);
    }
}
