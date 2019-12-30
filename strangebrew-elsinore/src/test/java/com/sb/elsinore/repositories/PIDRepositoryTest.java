package com.sb.elsinore.repositories;

import com.sb.elsinore.LaunchControl;
import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.PIDModel;
import com.sb.elsinore.models.TemperatureModel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@EnableConfigurationProperties
@AutoConfigureTestDatabase
@RunWith(SpringJUnit4ClassRunner.class)
public class PIDRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private PIDRepository pidRepository;
    @MockBean
    private LaunchControl launchControl;
    
    private TemperatureModel temperature;

    @Before
    public void setup() {
        initMocks(this);
        this.temperature = new TemperatureModel();
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
        PIDModel found = this.pidRepository.findByTemperatureNameIgnoreCase("Test");

        //then
        assertEquals(this.temperature.getName(), found.getTemperature().getName());
    }

    @Test
    public void canDeletePID() {
        // given
        PIDModel pidModel = new PIDModel();
        pidModel.setTemperature(this.temperature);

        this.entityManager.persistAndFlush(pidModel);
        Long id = pidModel.getId();

        // when
        this.pidRepository.delete(pidModel);

        //then
        this.thrown.expect(JpaObjectRetrievalFailureException.class);
        this.pidRepository.getOne(id);
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
        PIDModel found = this.pidRepository.findByTemperatureNameIgnoreCase("Test");

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
