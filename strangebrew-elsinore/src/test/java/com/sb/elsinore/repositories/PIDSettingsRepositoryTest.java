package com.sb.elsinore.repositories;

import com.sb.elsinore.configuration.JpaDataConfiguration;
import com.sb.elsinore.configuration.TestJpaConfiguration;
import com.sb.elsinore.configuration.TestRestConfiguration;
import com.sb.elsinore.models.PIDSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SpringBootTest
@ContextConfiguration(classes = {
        JpaDataConfiguration.class,
        TestJpaConfiguration.class,
        TestRestConfiguration.class})
@AutoConfigureTestDatabase
@RunWith(SpringJUnit4ClassRunner.class)
public class PIDSettingsRepositoryTest extends AbstractJUnit4SpringContextTests {
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private PIDSettingsRepository pidSettingsRepository;

    @Before
    public void setup() {
        initMocks(this);
        this.pidSettingsRepository.deleteAll();
    }

    @Test
    public void canCreatePIDSettingsAndGetById() {
        // given
        PIDSettings pidSettings = new PIDSettings();
        pidSettings.setGPIO("GPIO_1");

        Long pidSettingsId = this.pidSettingsRepository.saveAndFlush(pidSettings).getId();

        // when
        Optional<PIDSettings> found = this.pidSettingsRepository.findById(pidSettingsId);

        //then
        assertTrue(found.isPresent());
        assertEquals(pidSettings.getGPIO(), found.get().getGPIO());
    }


    @Test
    public void pidSettingsMustBeUniqueByGPIO() {
        // given
        PIDSettings pidSettings = new PIDSettings();
        pidSettings.setGPIO("GPIO_1");

        this.pidSettingsRepository.saveAndFlush(pidSettings);

        // when
        pidSettings = new PIDSettings();
        pidSettings.setGPIO("GPIO_1");


        //then
        this.thrown.expect(DataIntegrityViolationException.class);
        this.pidSettingsRepository.saveAndFlush(pidSettings);
    }

    @Test
    public void canDeletePIDSettings() {
        // given
        PIDSettings pidSettings = new PIDSettings();
        pidSettings.setGPIO("GPIO_1");

        Long pidSettingsId = this.pidSettingsRepository.saveAndFlush(pidSettings).getId();

        // when
        this.pidSettingsRepository.deleteById(pidSettingsId);

        //then
        Optional<PIDSettings> loaded = this.pidSettingsRepository.findById(pidSettingsId);

        assertFalse(loaded.isPresent());
    }
}
